package com.malvinas.rutas.domain.service.impl;

import com.malvinas.rutas.application.dto.*;
import com.malvinas.rutas.domain.entity.*;
import com.malvinas.rutas.domain.enumerate.DispatchPriority;
import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import com.malvinas.rutas.domain.enumerate.DisplayableEnum;
import com.malvinas.rutas.domain.repository.*;
import com.malvinas.rutas.domain.service.DispatchService;
import com.malvinas.rutas.infrastructure.client.PersonalServiceClient;
import com.malvinas.rutas.infrastructure.client.VehiculosServiceClient;
import com.malvinas.rutas.infrastructure.exception.*;
import com.malvinas.rutas.infrastructure.mapper.DispatchMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class DispatchServiceImpl implements DispatchService {

    private final DispatchRepository dispatchRepo;
    private final DeliveryPointRepository deliveryPointRepo;
    private final VehiculosServiceClient vehiculosClient;
    private final PersonalServiceClient personalClient;
    private final DispatchMapper mapper;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public DispatchServiceImpl(DispatchRepository dispatchRepo, DeliveryPointRepository deliveryPointRepo,
            VehiculosServiceClient vehiculosClient, PersonalServiceClient personalClient, DispatchMapper mapper) {
        this.dispatchRepo = dispatchRepo;
        this.deliveryPointRepo = deliveryPointRepo;
        this.vehiculosClient = vehiculosClient;
        this.personalClient = personalClient;
        this.mapper = mapper;
    }

    @PostConstruct
    public void initSequence() {
        try {
            dispatchRepo.findMaxLoadingOrderCode().ifPresent(code -> {
                try {
                    String[] parts = code.split("-");
                    if (parts.length == 3) sequence.set(Integer.parseInt(parts[2]) + 1);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {
            // table may not exist yet on first deploy; sequence starts at 1
        }
    }

    @Override
    public List<DispatchResponse> findByDriver(Long driverId) {
        return dispatchRepo.findByDriverId(driverId).stream().map(mapper::toDto).toList();
    }

    @Override
    public DispatchResponse accept(Long id, Long employeeId) {
        Dispatch dispatch = getDispatch(id);
        // Idempotent: if already accepted by the same driver, return as-is
        if (dispatch.getStatus() == DispatchStatus.ON_ROUTE) {
            if (!dispatch.getDriverId().equals(employeeId))
                throw new BusinessException("Solo el conductor asignado puede aceptar este despacho");
            return mapper.toDto(dispatch);
        }
        if (dispatch.getStatus() != DispatchStatus.SCHEDULED)
            throw new BusinessException("El despacho debe estar PROGRAMADO para aceptar la salida");
        if (!dispatch.getDriverId().equals(employeeId))
            throw new BusinessException("Solo el conductor asignado puede aceptar este despacho");

        String code = "OC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", sequence.getAndIncrement());
        dispatch.setLoadingOrderCode(code);
        dispatch.setActualDepartureTime(LocalDateTime.now());
        dispatch.setStatus(DispatchStatus.ON_ROUTE);
        Dispatch saved = dispatchRepo.save(dispatch);
        vehiculosClient.changeVehicleStatus(saved.getVehiclePlate(), "04", "Conductor aceptó despacho: " + code);
        return mapper.toDto(saved);
    }

    @Override
    public DispatchResponse create(DispatchRequest req) {
        if (!personalClient.isEmployeeActive(req.driverId()))
            throw new BusinessException("El conductor con ID " + req.driverId() + " no está activo");
        Dispatch dispatch = new Dispatch();
        dispatch.setVehiclePlate(req.vehiclePlate());
        dispatch.setDriverId(req.driverId());
        dispatch.setHelper1Id(req.helper1Id());
        dispatch.setHelper2Id(req.helper2Id());
        dispatch.setLoadId(req.loadId());
        dispatch.setPriority(req.priority() != null
                ? DisplayableEnum.fromCode(DispatchPriority.class, req.priority())
                : DispatchPriority.MEDIUM);
        dispatch.setScheduledDepartureTime(req.scheduledDepartureTime());
        dispatch.setRemarks(req.remarks());
        dispatch.setStatus(DispatchStatus.SCHEDULED);

        if (req.points() != null) {
            for (DispatchPointRequest pr : req.points()) {
                DeliveryPoint dp = deliveryPointRepo.findById(pr.deliveryPointId())
                        .orElseThrow(() -> new ResourceNotFoundException("DeliveryPoint", pr.deliveryPointId()));
                DispatchPoint dpt = new DispatchPoint();
                dpt.setDispatch(dispatch);
                dpt.setDeliveryPoint(dp);
                dpt.setVisitOrder(pr.visitOrder() != null ? pr.visitOrder() : (short) 1);
                dispatch.getDispatchPoints().add(dpt);
            }
        }
        return mapper.toDto(dispatchRepo.save(dispatch));
    }

    @Override @Transactional(readOnly = true)
    public DispatchResponse findById(Long id) {
        return mapper.toDto(getDispatch(id));
    }

    @Override @Transactional(readOnly = true)
    public List<DispatchResponse> findAll() {
        return dispatchRepo.findAll().stream().map(mapper::toDto).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<DispatchResponse> findActive() {
        return dispatchRepo.findByStatus(DispatchStatus.ON_ROUTE).stream().map(mapper::toDto).toList();
    }

    @Override
    public DispatchResponse registerDeparture(Long id) {
        Dispatch dispatch = getDispatch(id);
        if (dispatch.getStatus() != DispatchStatus.SCHEDULED)
            throw new BusinessException("El despacho debe estar en estado PROGRAMADO para registrar salida");

        String code = "OC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", sequence.getAndIncrement());
        dispatch.setLoadingOrderCode(code);
        dispatch.setActualDepartureTime(LocalDateTime.now());
        dispatch.setStatus(DispatchStatus.ON_ROUTE);
        vehiculosClient.changeVehicleStatus(dispatch.getVehiclePlate(), "04", "Despacho en ruta: " + code);
        return mapper.toDto(dispatchRepo.save(dispatch));
    }

    @Override
    public DispatchResponse complete(Long id) {
        Dispatch dispatch = getDispatch(id);
        if (dispatch.getStatus() != DispatchStatus.ON_ROUTE)
            throw new BusinessException("El despacho debe estar EN_RUTA para completarlo");

        dispatch.setReturnTime(LocalDateTime.now());
        dispatch.setStatus(DispatchStatus.COMPLETED);
        Dispatch saved = dispatchRepo.save(dispatch);
        vehiculosClient.changeVehicleStatusBestEffort(saved.getVehiclePlate(), "01", "Despacho completado");
        return mapper.toDto(saved);
    }

    @Override
    public DispatchResponse cancel(Long id) {
        Dispatch dispatch = getDispatch(id);
        if (dispatch.getStatus() != DispatchStatus.SCHEDULED)
            throw new BusinessException("Solo se pueden cancelar despachos en estado PROGRAMADO");
        dispatch.setStatus(DispatchStatus.CANCELLED);
        // Vehicle stays CARGADO — a new dispatch can be created for the same vehicle
        return mapper.toDto(dispatchRepo.save(dispatch));
    }

    @Override
    public DispatchResponse update(Long id, DispatchRequest req) {
        Dispatch dispatch = getDispatch(id);
        boolean isCancelled = dispatch.getStatus() == DispatchStatus.CANCELLED;
        if (dispatch.getStatus() != DispatchStatus.SCHEDULED && !isCancelled)
            throw new BusinessException("Solo se pueden editar despachos en estado PROGRAMADO o CANCELADO");
        dispatch.setVehiclePlate(req.vehiclePlate());
        dispatch.setDriverId(req.driverId());
        if (req.scheduledDepartureTime() != null) dispatch.setScheduledDepartureTime(req.scheduledDepartureTime());
        if (req.remarks() != null) dispatch.setRemarks(req.remarks());
        // Reactivate cancelled dispatch back to SCHEDULED
        if (isCancelled) dispatch.setStatus(DispatchStatus.SCHEDULED);
        return mapper.toDto(dispatchRepo.save(dispatch));
    }

    private Dispatch getDispatch(Long id) {
        return dispatchRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dispatch", id));
    }
}
