package com.malvinas.personal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalFlowTest {

    @Autowired MockMvc mvc;

    @Test
    void getEmployees_asAdm_returns200() throws Exception {
        mvc.perform(get("/api/employees")
                .header("X-Internal-Key", "dev-internal-key")
                .header("X-Employee-Id", "1")
                .header("X-Employee-Role", "ADM"))
           .andExpect(status().isOk());
    }

    @Test
    void directCall_withoutInternalKey_returns401_evenWithSpoofedRole() throws Exception {
        mvc.perform(get("/api/employees")
                .header("X-Employee-Id", "1")
                .header("X-Employee-Role", "ADM"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void authInfoEndpoint_withoutInternalKey_returns401() throws Exception {
        mvc.perform(get("/api/employees/auth/by-dni/00000001"))
           .andExpect(status().isUnauthorized());
    }
}
