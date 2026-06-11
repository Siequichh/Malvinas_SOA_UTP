package com.malvinas.cargas.domain.service.impl;

import com.malvinas.cargas.application.dto.LoadDetailRequest;
import com.malvinas.cargas.application.dto.LoadRequest;
import com.malvinas.cargas.application.dto.LoadResponse;
import com.malvinas.cargas.domain.entity.Load;
import com.malvinas.cargas.domain.entity.LoadDetail;
import com.malvinas.cargas.domain.enumerate.LoadStatus;
import com.malvinas.cargas.domain.repository.LoadRepository;
import com.malvinas.cargas.domain.service.LoadService;
import com.malvinas.cargas.infrastructure.client.VehiculosServiceClient;
import com.malvinas.cargas.infrastructure.exception.BusinessException;
import com.malvinas.cargas.infrastructure.exception.ResourceNotFoundException;
import com.malvinas.cargas.infrastructure.mapper.LoadMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LoadServiceImpl implements LoadService {

    private final LoadRepository loadRepo;
    private final VehiculosServiceClient vehiculosClient;
    private final LoadMapper loadMapper;

    public LoadServiceImpl(LoadRepository loadRepo, VehiculosServiceClient vehiculosClient, LoadMapper loadMapper) {
        this.loadRepo = loadRepo;
        this.vehiculosClient = vehiculosClient;
        this.loadMapper = loadMapper;
    }

    @Override
    public LoadResponse create(LoadRequest request) {
        Load load = new Load();
        load.setVehiclePlate(request.vehiclePlate());
        load.setMobilizerId(request.mobilizerId());
        load.setRemarks(request.remarks());
        if (request.loadingPlant() != null) load.setLoadingPlant(request.loadingPlant());
        load.setStatus(LoadStatus.IN_PROGRESS.getCode());
        load.setLoadingStartTime(LocalDateTime.now());

        if (request.details() != null) {
            for (LoadDetailRequest dr : request.details()) {
                LoadDetail detail = new LoadDetail();
                detail.setLoad(load);
                detail.setDescription(dr.description());
                detail.setQuantity(dr.quantity() != null ? dr.quantity() : 1);
                detail.setWeightKg(dr.weightKg());
                detail.setRemark(dr.remark());
                load.getDetails().add(detail);
            }
        }
        Load saved = loadRepo.save(load);
        vehiculosClient.changeVehicleStatus(request.vehiclePlate(), "02", "Loading started");
        return loadMapper.toResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    public LoadResponse findById(Long id) {
        return loadMapper.toResponse(getLoad(id));
    }

    @Override @Transactional(readOnly = true)
    public List<LoadResponse> findAll() {
        return loadRepo.findAll().stream().map(loadMapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<LoadResponse> findActive() {
        return loadRepo.findByStatus(LoadStatus.IN_PROGRESS.getCode()).stream()
                .map(loadMapper::toResponse).toList();
    }

    @Override
    public LoadResponse complete(Long id) {
        Load load = getLoad(id);
        validateTransition(load, LoadStatus.COMPLETED);
        load.setStatus(LoadStatus.COMPLETED.getCode());
        load.setLoadingEndTime(LocalDateTime.now());
        Load saved = loadRepo.save(load);
        vehiculosClient.changeVehicleStatus(load.getVehiclePlate(), "03", "Loading completed");
        return loadMapper.toResponse(saved);
    }

    @Override
    public LoadResponse cancel(Long id) {
        Load load = getLoad(id);
        validateTransition(load, LoadStatus.CANCELLED);
        load.setStatus(LoadStatus.CANCELLED.getCode());
        Load saved = loadRepo.save(load);
        vehiculosClient.changeVehicleStatus(load.getVehiclePlate(), "01", "Load cancelled");
        return loadMapper.toResponse(saved);
    }

    private void validateTransition(Load load, LoadStatus target) {
        LoadStatus current = LoadStatus.fromCode(load.getStatus());
        if (!current.canTransitionTo(target))
            throw new BusinessException("Cannot transition from " + current.getDisplayName() + " to " + target.getDisplayName());
    }

    private Load getLoad(Long id) {
        return loadRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Load", id));
    }
}
