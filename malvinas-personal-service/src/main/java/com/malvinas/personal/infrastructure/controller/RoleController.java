package com.malvinas.personal.infrastructure.controller;

import com.malvinas.personal.application.dto.role.RoleResponse;
import com.malvinas.personal.domain.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.ROLES)
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Consulta de roles del sistema")
public class RoleController {

    private final RoleService service;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
