package com.malvinas.vehiculos.infrastructure.controller;

import com.malvinas.vehiculos.application.dto.VehicleTypeResponse;
import com.malvinas.vehiculos.domain.repository.VehicleTypeRepository;
import com.malvinas.vehiculos.infrastructure.mapper.VehicleTypeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.VEHICLE_TYPES)
@RequiredArgsConstructor
@Tag(name = "Vehicle Types", description = "Vehicle type catalog")
public class VehicleTypeController {

    private final VehicleTypeRepository typeRepo;
    private final VehicleTypeMapper typeMapper;

    @GetMapping
    @Operation(summary = "List all active vehicle types")
    public ResponseEntity<List<VehicleTypeResponse>> findAll() {
        return ResponseEntity.ok(typeRepo.findByIsActiveTrue().stream().map(typeMapper::toResponse).toList());
    }
}
