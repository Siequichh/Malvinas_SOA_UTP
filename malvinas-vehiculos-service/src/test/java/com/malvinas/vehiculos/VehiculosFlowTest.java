package com.malvinas.vehiculos;

import com.malvinas.vehiculos.domain.entity.VehicleType;
import com.malvinas.vehiculos.domain.repository.VehicleTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehiculosFlowTest {

    @Autowired MockMvc mvc;
    @Autowired VehicleTypeRepository typeRepo;

    @Test
    void createVehicle_thenChangeStatus() throws Exception {
        VehicleType type = new VehicleType();
        type.setName("Camión Test");
        type.setActive(true);
        type = typeRepo.save(type);

        String plate = "TST-001";
        String createBody = """
                {"licensePlate":"%s","vehicleTypeId":%d}
                """.formatted(plate, type.getId());

        mvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
                .header("X-Employee-Id", "1")
                .header("X-Employee-Role", "ADM"))
           .andExpect(status().isCreated());

        mvc.perform(put("/api/vehicles/" + plate + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"newStatusCode":"02","reason":"test"}""")
                .header("X-Employee-Id", "1")
                .header("X-Employee-Role", "ADM"))
           .andExpect(status().isOk());
    }
}
