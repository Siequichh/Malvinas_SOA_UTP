package com.malvinas.cargas.infrastructure.controller;

import com.malvinas.cargas.application.dto.LoadRequest;
import com.malvinas.cargas.application.dto.LoadResponse;
import com.malvinas.cargas.domain.service.LoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.LOADS)
@RequiredArgsConstructor
@Tag(name = "Loads", description = "Vehicle loading control")
public class LoadController {

    private final LoadService loadService;

    @GetMapping
    @Operation(summary = "List all loads")
    public ResponseEntity<List<LoadResponse>> findAll() {
        return ResponseEntity.ok(loadService.findAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active loads (in progress)")
    public ResponseEntity<List<LoadResponse>> findActive() {
        return ResponseEntity.ok(loadService.findActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get load by ID")
    public ResponseEntity<LoadResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(loadService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Start loading a vehicle")
    public ResponseEntity<LoadResponse> create(@Valid @RequestBody LoadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loadService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update loading details (remarks, loading plant)")
    public ResponseEntity<LoadResponse> update(@PathVariable Long id, @Valid @RequestBody LoadRequest request) {
        return ResponseEntity.ok(loadService.update(id, request));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete loading — vehicle is ready for dispatch")
    public ResponseEntity<LoadResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(loadService.complete(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel loading — vehicle returns to AVAILABLE")
    public ResponseEntity<LoadResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(loadService.cancel(id));
    }
}
