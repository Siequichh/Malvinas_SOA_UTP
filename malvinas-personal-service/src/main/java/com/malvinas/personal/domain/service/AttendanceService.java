package com.malvinas.personal.domain.service;

import com.malvinas.personal.application.dto.attendance.AttendanceRequest;
import com.malvinas.personal.application.dto.attendance.AttendanceResponse;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    AttendanceResponse register(AttendanceRequest request);
    List<AttendanceResponse> findByDate(LocalDate date);
    List<AttendanceResponse> findByEmployee(Integer employeeId);
}
