package com.malvinas.rutas.config;

import com.malvinas.rutas.domain.entity.DeliveryPoint;
import com.malvinas.rutas.domain.repository.DeliveryPointRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner seedData(DeliveryPointRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            List.of(
                new String[]{"Tambo - Ate", "Av. Nicolas Ayllon 3200, Ate", "Ate", "Frente al mercado"},
                new String[]{"Listo - San Isidro", "Av. Conquistadores 500, San Isidro", "San Isidro", ""},
                new String[]{"OXXO - Miraflores", "Av. Larco 345, Miraflores", "Miraflores", "Esquina con Benavides"},
                new String[]{"Tambo - La Molina", "Av. La Molina 1200, La Molina", "La Molina", ""},
                new String[]{"Listo - Surco", "Av. Primavera 800, Santiago de Surco", "Surco", ""}
            ).forEach(t -> {
                DeliveryPoint dp = new DeliveryPoint();
                dp.setName(t[0]); dp.setAddress(t[1]); dp.setDistrict(t[2]); dp.setReference(t[3]);
                repo.save(dp);
            });
        };
    }
}
