package com.malvinas.rutas.domain.service.impl;

import com.malvinas.rutas.application.dto.*;
import com.malvinas.rutas.domain.entity.*;
import com.malvinas.rutas.domain.enumerate.DispatchPriority;
import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import com.malvinas.rutas.domain.enumerate.DisplayableEnum;
import com.malvinas.rutas.domain.repository.*;
import com.malvinas.rutas.domain.service.DispatchService;
import com.malvinas.rutas.infrastructure.client.VehiculosServiceClient;
import com.malvinas.rutas.infrastructure.exception.*;
import com.malvinas.rutas.infrastructure.mapper.DispatchMapper;
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
    private final DispatchMapper mapper;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public DispatchServiceImpl(DispatchRepository dispatchRepo, DeliveryPointRepository deliveryPointRepo,
            VehiculosServiceClient vehiculosClient, DispatchMapper mapper) {
        this.dispatchRepo = dispatchRepo;
        this.deliveryPointRepo = deliveryPointRepo;
        this.vehiculosClient = vehiculosClient;
        this.mapper = mapper;
    }

    @Override
    public DispatchResponse create(DispatchRequest req) {
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
        vehiculosClient.changeVehicleStatus(dispatch.getVehiclePlate(), "01", "Despacho completado");
        return mapper.toDto(dispatchRepo.save(dispatch));
    }

    private Dispatch getDispatch(Long id) {
        return dispatchRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dispatch", id));
    }
}
