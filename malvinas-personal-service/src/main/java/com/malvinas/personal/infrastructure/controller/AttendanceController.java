package com.malvinas.personal.infrastructure.controller;

import com.malvinas.personal.application.dto.attendance.AttendanceRequest;
import com.malvinas.personal.application.dto.attendance.AttendanceResponse;
import com.malvinas.personal.domain.service.AttendanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.ATTENDANCES)
@RequiredArgsConstructor
@Tag(name = "Asistencias", description = "Control de asistencia diaria")
public class AttendanceController {

    private final AttendanceService service;

    @PostMapping
    public ResponseEntity<AttendanceResponse> register(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceResponse>> findToday() {
        return ResponseEntity.ok(service.findByDate(LocalDate.now()));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AttendanceResponse>> findByDate(@PathVariable LocalDate date) {
        return ResponseEntity.ok(service.findByDate(date));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceResponse>> findByEmployee(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(service.findByEmployee(employeeId));
    }
}
