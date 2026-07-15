package com.malvinas.rutas;

import com.jayway.jsonpath.JsonPath;
import com.malvinas.rutas.infrastructure.client.PersonalServiceClient;
import com.malvinas.rutas.infrastructure.client.VehiculosServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RutasFlowTest {

    @Autowired MockMvc mvc;
    @MockitoBean PersonalServiceClient personalClient;
    @MockitoBean VehiculosServiceClient vehiculosClient;

    private static final long DRIVER_ID = 11L;

    @Test
    void createDispatch_thenAcceptAsDriver() throws Exception {
        when(personalClient.isEmployeeActive(anyLong())).thenReturn(true);
        doNothing().when(vehiculosClient).changeVehicleStatus(anyString(), anyString(), anyString());

        String response = mvc.perform(post("/api/dispatches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehiclePlate\":\"TST-001\",\"driverId\":" + DRIVER_ID + "}")
                .header("X-Employee-Id", "3")
                .header("X-Employee-Role", "SUP"))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mvc.perform(post("/api/dispatches/" + id + "/accept")
                .header("X-Employee-Id", String.valueOf(DRIVER_ID))
                .header("X-Employee-Role", "DRV"))
           .andExpect(status().isOk());
    }
}
