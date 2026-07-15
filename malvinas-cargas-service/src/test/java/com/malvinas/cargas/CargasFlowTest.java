package com.malvinas.cargas;

import com.jayway.jsonpath.JsonPath;
import com.malvinas.cargas.infrastructure.client.VehiculosServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CargasFlowTest {

    @Autowired MockMvc mvc;
    @MockitoBean VehiculosServiceClient vehiculosClient;

    @Test
    void createLoad_thenComplete() throws Exception {
        doNothing().when(vehiculosClient).changeVehicleStatus(anyString(), anyString(), anyString());

        String response = mvc.perform(post("/api/loads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehiclePlate\":\"TST-001\",\"mobilizerId\":1}")
                .header("X-Employee-Id", "6")
                .header("X-Employee-Role", "MOV"))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mvc.perform(put("/api/loads/" + id + "/complete")
                .header("X-Employee-Id", "6")
                .header("X-Employee-Role", "MOV"))
           .andExpect(status().isOk());
    }
}
