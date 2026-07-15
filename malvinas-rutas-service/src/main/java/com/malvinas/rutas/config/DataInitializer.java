package com.malvinas.rutas.config;

import com.malvinas.rutas.domain.entity.DeliveryPoint;
import com.malvinas.rutas.domain.entity.Dispatch;
import com.malvinas.rutas.domain.entity.DispatchPoint;
import com.malvinas.rutas.domain.enumerate.DeliveryStatus;
import com.malvinas.rutas.domain.enumerate.DispatchPriority;
import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import com.malvinas.rutas.domain.repository.DeliveryPointRepository;
import com.malvinas.rutas.domain.repository.DispatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Profile("!test")
@Configuration
public class DataInitializer {

    // ponytail: driverId 11-40 = DRV employees from personal-service (clean DB, sequential IDs)
    // Plates match vehiculos seed: ABC/DEF/GHI/JKL/MNO prefix, status by index 0-14

    // LOADED vehicles (index 11 per prefix) — ready for SCHEDULED dispatch
    private static final String[] LOADED_PLATES   = {"ABC-111", "DEF-111", "GHI-111", "JKL-111", "MNO-111"};
    // ON_ROUTE vehicles (indices 12-13 per prefix)
    private static final String[] ON_ROUTE_PLATES = {
        "ABC-112","ABC-113","DEF-112","DEF-113","GHI-112",
        "GHI-113","JKL-112","JKL-113","MNO-112","MNO-113"
    };
    // AVAILABLE vehicles for historical COMPLETED dispatches
    private static final String[] HIST_PLATES = {
        "ABC-100","ABC-101","ABC-102","ABC-103","ABC-104",
        "DEF-100","DEF-101","DEF-102","DEF-103","DEF-104",
        "GHI-100","GHI-101","GHI-102","GHI-103","GHI-104",
        "JKL-100","JKL-101","JKL-102","MNO-100","MNO-101"
    };

    @Bean
    public CommandLineRunner seedData(DeliveryPointRepository dpRepo, DispatchRepository dispatchRepo) {
        return args -> {
            try {
                seedDeliveryPoints(dpRepo);
                seedDispatches(dpRepo, dispatchRepo);
            } catch (Exception e) {
                log.warn("[seed] rutas skipped: {}", e.getMessage());
            }
        };
    }

    private void seedDeliveryPoints(DeliveryPointRepository repo) {
        if (repo.count() > 0) return;
        List.of(
            new String[]{"Tambo - Ate",         "Av. Nicolas Ayllon 3200, Ate",           "Ate",        "Frente al mercado"},
            new String[]{"Listo - San Isidro",  "Av. Conquistadores 500, San Isidro",     "San Isidro", ""},
            new String[]{"OXXO - Miraflores",   "Av. Larco 345, Miraflores",              "Miraflores", "Esquina con Benavides"},
            new String[]{"Tambo - La Molina",   "Av. La Molina 1200, La Molina",          "La Molina",  ""},
            new String[]{"Listo - Surco",       "Av. Primavera 800, Santiago de Surco",   "Surco",      ""}
        ).forEach(t -> {
            DeliveryPoint dp = new DeliveryPoint();
            dp.setName(t[0]); dp.setAddress(t[1]); dp.setDistrict(t[2]); dp.setReference(t[3]);
            repo.save(dp);
        });
    }

    private void seedDispatches(DeliveryPointRepository dpRepo, DispatchRepository dispatchRepo) {
        if (dispatchRepo.count() > 0) return;

        List<DeliveryPoint> points = dpRepo.findAll();
        LocalDateTime now = LocalDateTime.now();
        DispatchPriority[] priorities = DispatchPriority.values();

        // 20 COMPLETED dispatches — historial últimos 30 días
        for (int i = 0; i < 20; i++) {
            LocalDateTime departure = now.minusDays(30 - i).withHour(6 + (i % 5)).withMinute(0);
            LocalDateTime returnTime = departure.plusHours(8 + (i % 4));
            Dispatch d = Dispatch.builder()
                .vehiclePlate(HIST_PLATES[i])
                .driverId(11L + i)
                .loadId((long)(i + 1))
                .status(DispatchStatus.COMPLETED)
                .priority(priorities[i % 3])
                .scheduledDepartureTime(LocalTime.of(6 + (i % 5), 0))
                .actualDepartureTime(departure)
                .returnTime(returnTime)
                .loadingOrderCode(formatOC(LocalDate.now().minusDays(30 - i), i + 1))
                .remarks("Despacho completado")
                .build();
            addDispatchPoint(d, points.get(i % 5), (short) 1, DeliveryStatus.DELIVERED, returnTime.minusHours(2));
            if (i % 3 == 0)
                addDispatchPoint(d, points.get((i + 1) % 5), (short) 2, DeliveryStatus.DELIVERED, returnTime.minusHours(1));
            dispatchRepo.save(d);
        }

        // 10 ON_ROUTE dispatches — salieron hoy
        for (int i = 0; i < 10; i++) {
            LocalDateTime departure = now.minusHours(2 + i);
            Dispatch d = Dispatch.builder()
                .vehiclePlate(ON_ROUTE_PLATES[i])
                .driverId(31L + i)
                .status(DispatchStatus.ON_ROUTE)
                .priority(priorities[i % 3])
                .scheduledDepartureTime(LocalTime.of(6, 0))
                .actualDepartureTime(departure)
                .loadingOrderCode(formatOC(LocalDate.now(), 100 + i))
                .remarks("En ruta")
                .build();
            addDispatchPoint(d, points.get(i % 5), (short) 1, DeliveryStatus.PENDING, null);
            dispatchRepo.save(d);
        }

        // 5 SCHEDULED dispatches — vehículos LOADED listos para salir
        for (int i = 0; i < 5; i++) {
            Dispatch d = Dispatch.builder()
                .vehiclePlate(LOADED_PLATES[i])
                .driverId(11L + i)
                .status(DispatchStatus.SCHEDULED)
                .priority(i == 0 ? DispatchPriority.HIGH : DispatchPriority.MEDIUM)
                .scheduledDepartureTime(LocalTime.of(14 + i, 0))
                .remarks("Programado para turno tarde")
                .build();
            addDispatchPoint(d, points.get(i), (short) 1, DeliveryStatus.PENDING, null);
            dispatchRepo.save(d);
        }

        // 5 CANCELLED dispatches
        for (int i = 0; i < 5; i++) {
            Dispatch d = Dispatch.builder()
                .vehiclePlate("ABC-10" + (5 + i))
                .driverId(16L + i)
                .status(DispatchStatus.CANCELLED)
                .priority(DispatchPriority.LOW)
                .scheduledDepartureTime(LocalTime.of(8, 0))
                .remarks("Cancelado por mantenimiento no programado")
                .build();
            dispatchRepo.save(d);
        }
    }

    private void addDispatchPoint(Dispatch dispatch, DeliveryPoint dp, short order,
                                  DeliveryStatus status, LocalDateTime deliveryTime) {
        DispatchPoint pt = DispatchPoint.builder()
            .dispatch(dispatch)
            .deliveryPoint(dp)
            .visitOrder(order)
            .deliveryStatus(status)
            .deliveryTime(deliveryTime)
            .build();
        dispatch.getDispatchPoints().add(pt);
    }

    private String formatOC(LocalDate date, int seq) {
        return String.format("OC-%s-%03d",
            date.toString().replace("-", ""),
            seq);
    }
}
