package com.malvinas.rutas.infrastructure.controller;

import com.malvinas.rutas.application.dto.DispatchRequest;
import com.malvinas.rutas.application.dto.DispatchResponse;
import com.malvinas.rutas.domain.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.DISPATCHES)
@RequiredArgsConstructor
@Tag(name = "Dispatches", description = "Vehicle dispatch and route management")
public class DispatchController {

    private final DispatchService dispatchService;

    @GetMapping
    @Operation(summary = "List all dispatches")
    public ResponseEntity<List<DispatchResponse>> findAll() {
        return ResponseEntity.ok(dispatchService.findAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active dispatches (on route)")
    public ResponseEntity<List<DispatchResponse>> findActive() {
        return ResponseEntity.ok(dispatchService.findActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dispatch by ID")
    public ResponseEntity<DispatchResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(dispatchService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new dispatch with assigned driver and delivery points")
    public ResponseEntity<DispatchResponse> create(@Valid @RequestBody DispatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dispatchService.create(request));
    }

    @PostMapping("/{id}/departure")
    @Operation(summary = "Register vehicle departure — generates unique Loading Order Code")
    public ResponseEntity<DispatchResponse> registerDeparture(@PathVariable Long id) {
        return ResponseEntity.ok(dispatchService.registerDeparture(id));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete dispatch — vehicle has returned to base")
    public ResponseEntity<DispatchResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(dispatchService.complete(id));
    }
}
