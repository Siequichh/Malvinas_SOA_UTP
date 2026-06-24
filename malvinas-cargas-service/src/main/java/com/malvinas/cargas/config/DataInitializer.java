package com.malvinas.cargas.config;

import com.malvinas.cargas.domain.entity.Load;
import com.malvinas.cargas.domain.entity.LoadDetail;
import com.malvinas.cargas.domain.repository.LoadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
public class DataInitializer {

    // ponytail: mobilizerId 6-10 = MOV employees seeded in personal-service (clean DB, sequential IDs)
    private static final long[] MOV_IDS = {6L, 7L, 8L, 9L, 10L};

    // Plates that will be LOADING status in vehiculos seed (one per type)
    private static final String[] LOADING_PLATES = {"ABC-110", "DEF-110", "GHI-110", "JKL-110", "MNO-110"};

    // Plates that are AVAILABLE (historical loads completed)
    private static final String[] HIST_PLATES = {
        "ABC-100","ABC-101","ABC-102","DEF-100","DEF-101",
        "DEF-102","GHI-100","GHI-101","GHI-102","JKL-100",
        "JKL-101","JKL-102","MNO-100","MNO-101","MNO-102"
    };

    // Plates for PENDING loads (still AVAILABLE)
    private static final String[] PENDING_PLATES = {
        "ABC-103","ABC-104","DEF-103","DEF-104","GHI-103",
        "JKL-103","JKL-104","MNO-103","MNO-104","MNO-105"
    };

    private static final String[][] PRODUCTS = {
        {"Cajas Cerveza Cristal 330ml",    "50",  "750.00"},
        {"Cajas Gaseosa Inca Kola 500ml",  "80",  "480.00"},
        {"Bidones Agua San Luis 20L",       "30",  "600.00"},
        {"Cajas Cerveza Pilsen 620ml",      "60",  "900.00"},
        {"Jugos Gloria 1L Surtido",         "120", "360.00"},
        {"Agua Cielo 2.5L Paquete x6",     "90",  "540.00"},
        {"Chips Piqueo Surtido",            "200", "300.00"},
        {"Agua San Mateo 500ml x24",        "100", "480.00"},
        {"Cajas Leche Gloria 1L",           "70",  "630.00"},
        {"Bebidas Rehidratantes Powerade",  "150", "600.00"}
    };

    @Bean
    public CommandLineRunner seedLoads(LoadRepository repo) {
        return args -> {
            try {
            if (repo.count() > 0) return;

            LocalDateTime now = LocalDateTime.now();

            // 15 COMPLETED loads — historial de los últimos 2 meses
            for (int i = 0; i < 15; i++) {
                LocalDateTime start = now.minusDays(60 - (i * 2L)).withHour(6).withMinute(0);
                LocalDateTime end   = start.plusHours(4);
                Load load = Load.builder()
                    .vehiclePlate(HIST_PLATES[i])
                    .mobilizerId(MOV_IDS[i % 5])
                    .status("03")
                    .loadingPlant("Babel - Huachipa")
                    .loadingStartTime(start)
                    .loadingEndTime(end)
                    .remarks("Carga completada sin incidencias")
                    .build();
                addDetail(load, PRODUCTS[i % 10], 1);
                if (i % 3 == 0) addDetail(load, PRODUCTS[(i + 1) % 10], 2);
                repo.save(load);
            }

            // 5 IN_PROGRESS loads — usando vehículos en estado LOADING
            for (int i = 0; i < 5; i++) {
                LocalDateTime start = now.minusHours(2 + i);
                Load load = Load.builder()
                    .vehiclePlate(LOADING_PLATES[i])
                    .mobilizerId(MOV_IDS[i])
                    .status("02")
                    .loadingPlant("Babel - Huachipa")
                    .loadingStartTime(start)
                    .remarks("Carga en proceso")
                    .build();
                addDetail(load, PRODUCTS[(i + 5) % 10], 1);
                repo.save(load);
            }

            // 10 PENDING loads — en espera de inicio
            for (int i = 0; i < 10; i++) {
                Load load = Load.builder()
                    .vehiclePlate(PENDING_PLATES[i])
                    .mobilizerId(MOV_IDS[i % 5])
                    .status("01")
                    .loadingPlant("Babel - Huachipa")
                    .remarks("Pendiente de asignación de turno")
                    .build();
                addDetail(load, PRODUCTS[i % 10], 1);
                repo.save(load);
            }
            } catch (Exception e) {
                log.warn("[seed] cargas skipped: {}", e.getMessage());
            }
        };
    }

    private void addDetail(Load load, String[] product, int seq) {
        LoadDetail detail = LoadDetail.builder()
            .load(load)
            .description(product[0])
            .quantity(Integer.parseInt(product[1]))
            .weightKg(new BigDecimal(product[2]))
            .remark("Bulto " + seq)
            .build();
        load.getDetails().add(detail);
    }
}
