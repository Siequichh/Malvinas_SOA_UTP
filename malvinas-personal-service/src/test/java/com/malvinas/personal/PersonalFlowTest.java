package com.malvinas.personal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
                .header("X-Employee-Id", "1")
                .header("X-Employee-Role", "ADM"))
           .andExpect(status().isOk());
    }
}
