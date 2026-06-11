package com.malvinas.vehiculos.config;

import com.malvinas.vehiculos.domain.entity.VehicleType;
import com.malvinas.vehiculos.domain.repository.VehicleTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner seedData(VehicleTypeRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            List.of(
                new String[]{"Van", "3500.00", "Light delivery van"},
                new String[]{"Camion Mediano", "8000.00", "Medium truck"},
                new String[]{"Camion Grande", "15000.00", "Large truck"},
                new String[]{"Furgon", "5000.00", "Cargo van"},
                new String[]{"Minivan", "2000.00", "Small cargo van"}
            ).forEach(t -> {
                VehicleType vt = new VehicleType();
                vt.setName(t[0]);
                vt.setCapacityKg(new BigDecimal(t[1]));
                vt.setDescription(t[2]);
                repo.save(vt);
            });
        };
    }
}
