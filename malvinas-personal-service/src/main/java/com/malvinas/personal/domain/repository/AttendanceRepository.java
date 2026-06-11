package com.malvinas.personal.domain.repository;

import com.malvinas.personal.domain.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    Optional<Attendance> findByEmployeeIdAndDate(Integer employeeId, LocalDate date);
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeId(Integer employeeId);
}
