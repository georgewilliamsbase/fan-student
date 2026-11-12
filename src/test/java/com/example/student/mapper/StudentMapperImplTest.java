package com.example.student.mapper;

import com.example.student.dto.*;
import com.example.student.model.Student;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperImplTest {

    private final StudentMapperImpl mapper = new StudentMapperImpl();

    private Student createStudent(long id, String name, String surname) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setSurname(surname);
        student.setPhoneNumber("111");
        student.setEmail(name.toLowerCase() + "@mail.ru");
        return student;
    }

    @Test
    void requestToStudent_shouldCopyAllFields() {
        StudentRequest request = new StudentRequest("Ivan", "Petrov", "111", "ivan@mail.ru");

        Student student = mapper.requestToStudent(request);

        assertThat(student.getId()).isNull();
        assertThat(student.getName()).isEqualTo("Ivan");
        assertThat(student.getSurname()).isEqualTo("Petrov");
        assertThat(student.getPhoneNumber()).isEqualTo("111");
        assertThat(student.getEmail()).isEqualTo("ivan@mail.ru");
    }

    @Test
    void studentListToStudentResponseList_shouldMapAllAndCount() {
        List<Student> students = List.of(
                createStudent(1L, "Ivan", "Petrov"),
                createStudent(2L, "Petr", "Semenov"));

        StudentResponseList result = mapper.studentListToStudentResponseList(students);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getResponse())
                .extracting(StudentResponseForList::getName)
                .containsExactly("Ivan", "Petr");
    }
}
