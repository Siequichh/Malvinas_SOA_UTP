package com.malvinas.personal.domain.service;

import com.malvinas.personal.application.dto.employee.EmployeeAuthResponse;
import com.malvinas.personal.application.dto.employee.EmployeeRequest;
import com.malvinas.personal.application.dto.employee.EmployeeResponse;
import java.util.List;

public interface EmployeeService {
    List<EmployeeResponse> findAll();
    EmployeeResponse findById(Integer id);
    EmployeeResponse findByDni(String dni);
    EmployeeAuthResponse findAuthInfoByDni(String dni);
    List<EmployeeResponse> findByRole(String roleCode);
    EmployeeResponse create(EmployeeRequest request);
    EmployeeResponse update(Integer id, EmployeeRequest request);
    void deactivate(Integer id);
    boolean isActive(Integer id);
}
