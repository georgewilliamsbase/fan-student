package com.example.student.controller;

import com.example.student.configuration.SecurityConfiguration;
import com.example.student.dto.*;
import com.example.student.security.JwtService;
import com.example.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@Import(SecurityConfiguration.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;
    // нужны для JwtFilter, который подключается в SecurityConfiguration
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String STUDENT_JSON = """
            {"name": "Ivan", "surname": "Petrov", "phone_number": "111", "email": "ivan@mail.ru"}
            """;

    @Test
    @WithMockUser(roles = "TEACHER")
    void findAll_shouldReturnList() throws Exception {
        when(studentService.findAll()).thenReturn(new StudentResponseList(
                List.of(new StudentResponseForList(1L, "Ivan", "Petrov")), 1));

        mockMvc.perform(get("/api/v1/student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.response[0].name").value("Ivan"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_shouldReturnStudent() throws Exception {
        when(studentService.findById(1L))
                .thenReturn(new StudentFullResponse(1L, "Ivan", "Petrov", "111", "ivan@mail.ru"));

        mockMvc.perform(get("/api/v1/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Petrov"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStudent_asAdmin_shouldReturnCreatedStudent() throws Exception {
        when(studentService.save(any(StudentRequest.class)))
                .thenReturn(new StudentResponse(1L, "Ivan", "Petrov"));

        mockMvc.perform(post("/api/v1/student/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STUDENT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ivan"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createStudent_asTeacher_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/student/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STUDENT_JSON))
                .andExpect(status().isForbidden());

        verify(studentService, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStudent_shouldReturnUpdatedStudent() throws Exception {
        when(studentService.update(eq(1L), any(StudentUpdateRequest.class)))
                .thenReturn(new StudentResponse(1L, "Ivan", "Petrov"));

        mockMvc.perform(put("/api/v1/student/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "222", "email": "new@mail.ru"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void request_withoutAuthentication_shouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/student"))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(studentService);
    }
}
