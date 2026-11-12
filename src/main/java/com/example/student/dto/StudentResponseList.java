package com.example.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StudentResponseList {
    List<StudentResponseForList> response;
    int totalCount;
}
