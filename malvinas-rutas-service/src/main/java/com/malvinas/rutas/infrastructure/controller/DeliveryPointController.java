package com.malvinas.rutas.infrastructure.controller;

import com.malvinas.rutas.application.dto.DeliveryPointRequest;
import com.malvinas.rutas.application.dto.DeliveryPointResponse;
import com.malvinas.rutas.domain.entity.DeliveryPoint;
import com.malvinas.rutas.domain.repository.DeliveryPointRepository;
import com.malvinas.rutas.infrastructure.exception.ResourceNotFoundException;
import com.malvinas.rutas.infrastructure.mapper.DeliveryPointMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.DELIVERY_POINTS)
@RequiredArgsConstructor
@Tag(name = "Delivery Points", description = "Client delivery point catalog")
public class DeliveryPointController {

    private final DeliveryPointRepository repo;
    private final DeliveryPointMapper mapper;

    @GetMapping
    @Operation(summary = "List all active delivery points")
    public ResponseEntity<List<DeliveryPointResponse>> findAll() {
        return ResponseEntity.ok(repo.findByIsActiveTrue().stream().map(mapper::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery point by ID")
    public ResponseEntity<DeliveryPointResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDto(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryPoint", id))));
    }

    @PostMapping
    @Operation(summary = "Create a new delivery point")
    public ResponseEntity<DeliveryPointResponse> create(@Valid @RequestBody DeliveryPointRequest req) {
        DeliveryPoint dp = new DeliveryPoint();
        dp.setName(req.name()); dp.setAddress(req.address()); dp.setDistrict(req.district());
        dp.setReference(req.reference()); dp.setContact(req.contact()); dp.setPhone(req.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(repo.save(dp)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery point")
    public ResponseEntity<DeliveryPointResponse> update(@PathVariable Long id,
            @Valid @RequestBody DeliveryPointRequest req) {
        DeliveryPoint dp = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryPoint", id));
        dp.setName(req.name()); dp.setAddress(req.address()); dp.setDistrict(req.district());
        dp.setReference(req.reference()); dp.setContact(req.contact()); dp.setPhone(req.phone());
        return ResponseEntity.ok(mapper.toDto(repo.save(dp)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate delivery point")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        DeliveryPoint dp = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryPoint", id));
        dp.setActive(false);
        repo.save(dp);
        return ResponseEntity.noContent().build();
    }
}
