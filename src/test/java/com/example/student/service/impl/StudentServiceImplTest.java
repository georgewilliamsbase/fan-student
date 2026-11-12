package com.example.student.service.impl;

import com.example.student.dto.*;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.StudentMapper;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentMapper studentMapper;
    @InjectMocks
    private StudentServiceImpl studentService;

    private Student createStudent() {
        Student student = new Student();
        student.setId(1L);
        student.setName("Ivan");
        student.setSurname("Petrov");
        student.setPhoneNumber("111");
        student.setEmail("old@mail.ru");
        return student;
    }

    @Test
    void save_shouldMapSaveAndReturnResponse() {
        StudentRequest request = new StudentRequest("Ivan", "Petrov", "111", "old@mail.ru");
        Student student = createStudent();
        StudentResponse expected = new StudentResponse(1L, "Ivan", "Petrov");

        when(studentMapper.requestToStudent(request)).thenReturn(student);
        when(studentRepository.save(student)).thenReturn(student);
        when(studentMapper.studentToResponse(student)).thenReturn(expected);

        StudentResponse result = studentService.save(request);

        assertThat(result).isSameAs(expected);
        verify(studentRepository).save(student);
    }

    @Test
    void findById_whenStudentExists_shouldReturnFullResponse() {
        Student student = createStudent();
        StudentFullResponse expected = new StudentFullResponse(1L, "Ivan", "Petrov", "111", "old@mail.ru");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentMapper.studentToFullResponse(student)).thenReturn(expected);

        assertThat(studentService.findById(1L)).isSameAs(expected);
    }

    @Test
    void findById_whenStudentNotFound_shouldThrowException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> studentService.findById(99L));

        assertThat(ex.getMessage()).contains("99");
    }

    @Test
    void update_whenAllFieldsPresent_shouldUpdatePhoneAndEmail() {
        Student student = createStudent();
        StudentUpdateRequest request = new StudentUpdateRequest("222", "new@mail.ru");
        StudentResponse expected = new StudentResponse(1L, "Ivan", "Petrov");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenReturn(student);
        when(studentMapper.studentToResponse(student)).thenReturn(expected);

        StudentResponse result = studentService.update(1L, request);

        assertThat(result).isSameAs(expected);
        assertThat(student.getPhoneNumber()).isEqualTo("222");
        assertThat(student.getEmail()).isEqualTo("new@mail.ru");
    }

    @Test
    void update_whenStudentNotFound_shouldThrowException() {
        when(studentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> studentService.update(5L, new StudentUpdateRequest("1", "a@b.c")));
        verify(studentRepository, never()).save(any());
    }

}
