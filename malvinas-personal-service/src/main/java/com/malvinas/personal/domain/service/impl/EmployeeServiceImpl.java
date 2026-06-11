package com.malvinas.personal.domain.service.impl;

import com.malvinas.personal.application.dto.employee.EmployeeAuthResponse;
import com.malvinas.personal.application.dto.employee.EmployeeRequest;
import com.malvinas.personal.application.dto.employee.EmployeeResponse;
import com.malvinas.personal.domain.entity.Employee;
import com.malvinas.personal.domain.entity.Role;
import com.malvinas.personal.domain.repository.EmployeeRepository;
import com.malvinas.personal.domain.repository.RoleRepository;
import com.malvinas.personal.domain.service.EmployeeService;
import com.malvinas.personal.infrastructure.exception.BusinessException;
import com.malvinas.personal.infrastructure.exception.ResourceNotFoundException;
import com.malvinas.personal.infrastructure.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final RoleRepository roleRepo;
    private final EmployeeMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<EmployeeResponse> findAll() {
        return employeeRepo.findByIsActiveTrue().stream().map(mapper::toDto).toList();
    }

    @Override
    public EmployeeResponse findById(Integer id) {
        return mapper.toDto(findOrThrow(id));
    }

    @Override
    public EmployeeResponse findByDni(String dni) {
        return employeeRepo.findByDni(dni)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado con DNI: " + dni));
    }

    @Override
    public EmployeeAuthResponse findAuthInfoByDni(String dni) {
        Employee emp = employeeRepo.findByDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado con DNI: " + dni));
        return new EmployeeAuthResponse(
                emp.getId(),
                emp.getDni(),
                emp.getFirstName(),
                emp.getLastName(),
                emp.getPasswordHash(),
                emp.getRole() != null ? emp.getRole().getCode() : null,
                emp.getIsActive()
        );
    }

    @Override
    public List<EmployeeResponse> findByRole(String roleCode) {
        return employeeRepo.findByRoleCode(roleCode).stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepo.existsByDni(request.dni())) {
            throw new BusinessException("Ya existe un empleado con DNI: " + request.dni());
        }
        Role role = roleRepo.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", request.roleId()));

        Employee emp = Employee.builder()
                .dni(request.dni())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .email(request.email())
                .passwordHash(request.password() != null ? passwordEncoder.encode(request.password()) : null)
                .role(role)
                .hireDate(request.hireDate() != null ? request.hireDate() : LocalDate.now())
                .licenseNumber(request.licenseNumber())
                .licenseCategory(request.licenseCategory())
                .isActive(true)
                .build();

        return mapper.toDto(employeeRepo.save(emp));
    }

    @Override
    @Transactional
    public EmployeeResponse update(Integer id, EmployeeRequest request) {
        Employee emp = findOrThrow(id);
        Role role = roleRepo.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", request.roleId()));

        emp.setFirstName(request.firstName());
        emp.setLastName(request.lastName());
        emp.setPhone(request.phone());
        emp.setEmail(request.email());
        emp.setRole(role);
        emp.setLicenseNumber(request.licenseNumber());
        emp.setLicenseCategory(request.licenseCategory());
        if (request.password() != null && !request.password().isBlank()) {
            emp.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return mapper.toDto(employeeRepo.save(emp));
    }

    @Override
    @Transactional
    public void deactivate(Integer id) {
        Employee emp = findOrThrow(id);
        emp.setIsActive(false);
        employeeRepo.save(emp);
    }

    @Override
    public boolean isActive(Integer id) {
        return employeeRepo.findById(id).map(Employee::getIsActive).orElse(false);
    }

    private Employee findOrThrow(Integer id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", id));
    }
}
