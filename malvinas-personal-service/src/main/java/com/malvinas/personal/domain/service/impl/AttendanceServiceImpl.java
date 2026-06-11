package com.malvinas.personal.domain.service.impl;

import com.malvinas.personal.application.dto.attendance.AttendanceRequest;
import com.malvinas.personal.application.dto.attendance.AttendanceResponse;
import com.malvinas.personal.domain.entity.Attendance;
import com.malvinas.personal.domain.entity.Employee;
import com.malvinas.personal.domain.repository.AttendanceRepository;
import com.malvinas.personal.domain.repository.EmployeeRepository;
import com.malvinas.personal.domain.service.AttendanceService;
import com.malvinas.personal.domain.enumerate.AttendanceStatus;
import com.malvinas.personal.domain.enumerate.DisplayableEnum;
import com.malvinas.personal.infrastructure.exception.BusinessException;
import com.malvinas.personal.infrastructure.exception.ResourceNotFoundException;
import com.malvinas.personal.infrastructure.mapper.AttendanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final EmployeeRepository employeeRepo;
    private final AttendanceMapper mapper;

    @Override
    @Transactional
    public AttendanceResponse register(AttendanceRequest request) {
        Employee employee = employeeRepo.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", request.employeeId()));

        LocalDate date = request.date() != null ? request.date() : LocalDate.now();

        if (attendanceRepo.findByEmployeeIdAndDate(request.employeeId(), date).isPresent()) {
            throw new BusinessException("Ya existe asistencia registrada para este empleado en la fecha: " + date);
        }

        AttendanceStatus attendanceStatus = DisplayableEnum.fromCode(AttendanceStatus.class, request.status());

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(date)
                .checkInTime(request.checkInTime())
                .checkOutTime(request.checkOutTime())
                .status(attendanceStatus)
                .remark(request.remark())
                .build();

        return mapper.toDto(attendanceRepo.save(attendance));
    }

    @Override
    public List<AttendanceResponse> findByDate(LocalDate date) {
        return attendanceRepo.findByDate(date).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<AttendanceResponse> findByEmployee(Integer employeeId) {
        return attendanceRepo.findByEmployeeId(employeeId).stream().map(mapper::toDto).toList();
    }
}
