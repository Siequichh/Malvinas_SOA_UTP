package com.malvinas.vehiculos.config;

import com.malvinas.vehiculos.domain.entity.Vehicle;
import com.malvinas.vehiculos.domain.entity.VehicleType;
import com.malvinas.vehiculos.domain.enumerate.VehicleStatus;
import com.malvinas.vehiculos.domain.repository.VehicleRepository;
import com.malvinas.vehiculos.domain.repository.VehicleTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(VehicleTypeRepository typeRepo, VehicleRepository vehicleRepo) {
        return args -> {
            try {
                seedTypes(typeRepo);
                seedVehicles(typeRepo, vehicleRepo);
            } catch (Exception e) {
                log.warn("[seed] vehiculos skipped: {}", e.getMessage());
            }
        };
    }

    private void seedTypes(VehicleTypeRepository repo) {
        if (repo.count() > 0) return;
        List.of(
            new String[]{"Van",            "3500.00",  "Furgoneta de reparto urbano"},
            new String[]{"Camion Mediano", "8000.00",  "Camion de carga mediana"},
            new String[]{"Camion Grande",  "15000.00", "Camion de carga pesada"},
            new String[]{"Furgon",         "5000.00",  "Furgón cerrado de distribución"},
            new String[]{"Minivan",        "2000.00",  "Minivan de carga liviana"}
        ).forEach(t -> {
            VehicleType vt = new VehicleType();
            vt.setName(t[0]);
            vt.setCapacityKg(new BigDecimal(t[1]));
            vt.setDescription(t[2]);
            vt.setActive(true); // ponytail: primitive boolean defaults false with no-arg ctor; @Builder.Default only applies via builder
            repo.save(vt);
        });
    }

    private void seedVehicles(VehicleTypeRepository typeRepo, VehicleRepository vehicleRepo) {
        if (vehicleRepo.count() > 0) return;

        List<VehicleType> types = typeRepo.findAll(Sort.by("id"));

        // ponytail: 5 types × 15 plates each = 75 vehicles
        // Status layout per type (indices 0-14):
        //   0-9  AVAILABLE  |  10 LOADING  |  11 LOADED  |  12-13 ON_ROUTE  |  14 MAINTENANCE
        // Plate scheme: {PREFIX}-{100+idx}  (e.g. ABC-100, ABC-101, ...)
        String[]   prefixes = {"ABC", "DEF", "GHI", "JKL", "MNO"};
        VehicleStatus[] statusMap = {
            VehicleStatus.AVAILABLE,  VehicleStatus.AVAILABLE,  VehicleStatus.AVAILABLE,
            VehicleStatus.AVAILABLE,  VehicleStatus.AVAILABLE,  VehicleStatus.AVAILABLE,
            VehicleStatus.AVAILABLE,  VehicleStatus.AVAILABLE,  VehicleStatus.AVAILABLE,
            VehicleStatus.AVAILABLE,  VehicleStatus.LOADING,    VehicleStatus.LOADED,
            VehicleStatus.ON_ROUTE,   VehicleStatus.ON_ROUTE,   VehicleStatus.MAINTENANCE
        };
        // brand/model cycling per type (index % 5)
        String[][][] brandModel = {
            {{"Hyundai","H350"},{"Toyota","Hiace"},{"Mitsubishi","L300"},{"Hyundai","H1"},{"Mercedes","Vito"}},
            {{"JAC","N75"},{"Foton","Aumark 838"},{"Hino","300"},{"JAC","N55"},{"Foton","Tornado"}},
            {{"Volvo","FM"},{"Mercedes","Actros"},{"Hino","700"},{"Scania","R450"},{"Volvo","FH"}},
            {{"Mercedes","Sprinter"},{"Ford","Transit"},{"JAC","X200"},{"Iveco","Daily"},{"Renault","Master"}},
            {{"Toyota","Hiace Combi"},{"Hyundai","Starex"},{"Kia","Grand Sedona"},{"Mitsubishi","Delica"},{"Toyota","Hiace GL"}}
        };
        String[] colors = {"Blanco", "Gris Plata", "Azul Marino", "Rojo", "Amarillo"};
        short[]  years  = {2018, 2019, 2020, 2021, 2022};
        LocalDate soatExpiry  = LocalDate.of(2026, 12, 31);
        LocalDate techInspect = LocalDate.of(2026, 6, 30);

        for (int t = 0; t < 5; t++) {
            VehicleType vType = types.get(t);
            for (int v = 0; v < 15; v++) {
                String plate = prefixes[t] + "-" + String.format("%03d", 100 + v);
                String[] bm  = brandModel[t][v % 5];
                vehicleRepo.save(Vehicle.builder()
                    .licensePlate(plate)
                    .vehicleType(vType)
                    .brand(bm[0])
                    .model(bm[1])
                    .year(years[v % 5])
                    .color(colors[v % 5])
                    .status(statusMap[v])
                    .mileage(10000 + v * 500 + t * 2000)
                    .soatExpiryDate(soatExpiry)
                    .technicalInspectionDate(techInspect)
                    .isActive(true)
                    .build());
            }
        }
    }
}
