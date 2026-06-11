package com.malvinas.personal.infrastructure.controller;

import com.malvinas.personal.application.dto.employee.EmployeeAuthResponse;
import com.malvinas.personal.application.dto.employee.EmployeeRequest;
import com.malvinas.personal.application.dto.employee.EmployeeResponse;
import com.malvinas.personal.domain.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPaths.EMPLOYEES)
@RequiredArgsConstructor
@Tag(name = "Empleados", description = "Gestion de empleados")
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    @Operation(summary = "Listar todos los empleados activos")
    public ResponseEntity<List<EmployeeResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener empleado por ID")
    public ResponseEntity<EmployeeResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/active")
    @Operation(summary = "Verificar si empleado esta activo")
    public ResponseEntity<Map<String, Boolean>> isActive(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of("active", service.isActive(id)));
    }

    @GetMapping("/by-dni/{dni}")
    @Operation(summary = "Buscar empleado por DNI")
    public ResponseEntity<EmployeeResponse> findByDni(@PathVariable String dni) {
        return ResponseEntity.ok(service.findByDni(dni));
    }

    @GetMapping("/auth/by-dni/{dni}")
    @Operation(summary = "Obtener info de autenticacion por DNI (uso interno de auth-service)")
    public ResponseEntity<EmployeeAuthResponse> findAuthInfoByDni(@PathVariable String dni) {
        return ResponseEntity.ok(service.findAuthInfoByDni(dni));
    }

    @GetMapping("/role/{roleCode}")
    @Operation(summary = "Listar empleados por rol")
    public ResponseEntity<List<EmployeeResponse>> findByRole(@PathVariable String roleCode) {
        return ResponseEntity.ok(service.findByRole(roleCode));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo empleado")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar empleado")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Integer id,
                                                    @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar empleado")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
