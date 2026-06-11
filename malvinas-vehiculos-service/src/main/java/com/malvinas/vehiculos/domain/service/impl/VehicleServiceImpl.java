package com.malvinas.vehiculos.domain.service.impl;

import com.malvinas.vehiculos.application.dto.*;
import com.malvinas.vehiculos.domain.entity.*;
import com.malvinas.vehiculos.domain.enumerate.DisplayableEnum;
import com.malvinas.vehiculos.domain.enumerate.VehicleStatus;
import com.malvinas.vehiculos.domain.repository.*;
import com.malvinas.vehiculos.domain.service.VehicleService;
import com.malvinas.vehiculos.infrastructure.exception.*;
import com.malvinas.vehiculos.infrastructure.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepo;
    private final VehicleTypeRepository typeRepo;
    private final StatusHistoryRepository historyRepo;
    private final VehicleMapper vehicleMapper;
    private final VehicleTypeMapper typeMapper;

    public VehicleServiceImpl(VehicleRepository vehicleRepo, VehicleTypeRepository typeRepo,
            StatusHistoryRepository historyRepo, VehicleMapper vehicleMapper, VehicleTypeMapper typeMapper) {
        this.vehicleRepo = vehicleRepo;
        this.typeRepo = typeRepo;
        this.historyRepo = historyRepo;
        this.vehicleMapper = vehicleMapper;
        this.typeMapper = typeMapper;
    }

    @Override @Transactional(readOnly = true)
    public List<VehicleResponse> findAll() {
        return vehicleRepo.findByIsActiveTrue().stream().map(vehicleMapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public VehicleResponse findByPlate(String plate) {
        return vehicleMapper.toResponse(getByPlate(plate));
    }

    @Override
    public VehicleResponse create(VehicleRequest req) {
        if (vehicleRepo.existsByLicensePlate(req.licensePlate()))
            throw new BusinessException("Vehicle " + req.licensePlate() + " already exists");
        VehicleType type = typeRepo.findById(req.vehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("VehicleType", req.vehicleTypeId()));
        Vehicle v = new Vehicle();
        v.setLicensePlate(req.licensePlate()); v.setVehicleType(type);
        v.setBrand(req.brand()); v.setModel(req.model()); v.setYear(req.year());
        v.setColor(req.color()); v.setMileage(req.mileage() != null ? req.mileage() : 0);
        v.setSoatExpiryDate(req.soatExpiryDate());
        v.setTechnicalInspectionDate(req.technicalInspectionDate());
        return vehicleMapper.toResponse(vehicleRepo.save(v));
    }

    @Override
    public VehicleResponse update(String plate, VehicleRequest req) {
        Vehicle v = getByPlate(plate);
        VehicleType type = typeRepo.findById(req.vehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("VehicleType", req.vehicleTypeId()));
        v.setVehicleType(type); v.setBrand(req.brand()); v.setModel(req.model());
        v.setYear(req.year()); v.setColor(req.color());
        if (req.mileage() != null) v.setMileage(req.mileage());
        v.setSoatExpiryDate(req.soatExpiryDate());
        v.setTechnicalInspectionDate(req.technicalInspectionDate());
        return vehicleMapper.toResponse(vehicleRepo.save(v));
    }

    @Override
    public VehicleResponse changeStatus(String plate, StatusChangeRequest req) {
        Vehicle v = getByPlate(plate);
        VehicleStatus current = v.getStatus();
        VehicleStatus next = DisplayableEnum.fromCode(VehicleStatus.class, req.newStatusCode());
        if (!current.canTransitionTo(next))
            throw new BusinessException("Cannot transition from " + current.getDisplayName() + " to " + next.getDisplayName());
        StatusHistory h = new StatusHistory();
        h.setVehicle(v); h.setPreviousStatus(current.getCode());
        h.setNewStatus(next.getCode()); h.setReason(req.reason()); h.setEmployeeId(req.employeeId());
        historyRepo.save(h);
        v.setStatus(next);
        return vehicleMapper.toResponse(vehicleRepo.save(v));
    }

    @Override @Transactional(readOnly = true)
    public List<VehicleResponse> findByStatus(String statusCode) {
        VehicleStatus status = DisplayableEnum.fromCode(VehicleStatus.class, statusCode);
        return vehicleRepo.findByStatusAndIsActiveTrue(status).stream()
                .map(vehicleMapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<StatusHistoryResponse> findHistory(String plate) {
        return historyRepo.findByVehicle_LicensePlateOrderByCreatedAtDesc(plate).stream()
                .map(h -> new StatusHistoryResponse(
                    h.getId(),
                    h.getPreviousStatus(), DisplayableEnum.fromCode(VehicleStatus.class, h.getPreviousStatus()).getDisplayName(),
                    h.getNewStatus(), DisplayableEnum.fromCode(VehicleStatus.class, h.getNewStatus()).getDisplayName(),
                    h.getReason(), h.getEmployeeId(), h.getCreatedAt()))
                .toList();
    }

    @Override
    public void deactivate(String plate) {
        Vehicle v = getByPlate(plate);
        v.setActive(false);
        vehicleRepo.save(v);
    }

    private Vehicle getByPlate(String plate) {
        return vehicleRepo.findByLicensePlateAndIsActiveTrue(plate)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", plate));
    }
}
