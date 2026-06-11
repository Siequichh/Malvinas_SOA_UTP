package com.malvinas.vehiculos.infrastructure.controller;

import com.malvinas.vehiculos.application.dto.*;
import com.malvinas.vehiculos.domain.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.VEHICLES)
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Fleet vehicle management")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "List all active vehicles")
    public ResponseEntity<List<VehicleResponse>> findAll() {
        return ResponseEntity.ok(vehicleService.findAll());
    }

    @GetMapping("/{plate}")
    @Operation(summary = "Get vehicle by license plate")
    public ResponseEntity<VehicleResponse> findByPlate(@PathVariable String plate) {
        return ResponseEntity.ok(vehicleService.findByPlate(plate));
    }

    @GetMapping("/status/{statusCode}")
    @Operation(summary = "Filter vehicles by status code")
    public ResponseEntity<List<VehicleResponse>> findByStatus(@PathVariable String statusCode) {
        return ResponseEntity.ok(vehicleService.findByStatus(statusCode));
    }

    @GetMapping("/{plate}/history")
    @Operation(summary = "Get status change history for a vehicle")
    public ResponseEntity<List<StatusHistoryResponse>> findHistory(@PathVariable String plate) {
        return ResponseEntity.ok(vehicleService.findHistory(plate));
    }

    @PostMapping
    @Operation(summary = "Register a new vehicle")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
    }

    @PutMapping("/{plate}")
    @Operation(summary = "Update vehicle data")
    public ResponseEntity<VehicleResponse> update(@PathVariable String plate,
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(plate, request));
    }

    @PutMapping("/{plate}/status")
    @Operation(summary = "Change vehicle operational status")
    public ResponseEntity<VehicleResponse> changeStatus(@PathVariable String plate,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(vehicleService.changeStatus(plate, request));
    }

    @DeleteMapping("/{plate}")
    @Operation(summary = "Deactivate vehicle")
    public ResponseEntity<Void> deactivate(@PathVariable String plate) {
        vehicleService.deactivate(plate);
        return ResponseEntity.noContent().build();
    }
}
