package com.myapp.aqdeeha.config;

import com.myapp.aqdeeha.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Browser preflight: the local profile allows the Expo web dev server (http://localhost:8081). */
class CorsIntegrationTest extends IntegrationTestBase {

    @Test
    void preflightFromAllowedOrigin_isAccepted() throws Exception {
        mvc.perform(options("/api/v1/auth/customer/signup/send-otp")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void preflightFromUnknownOrigin_isRejected() throws Exception {
        mvc.perform(options("/api/v1/auth/customer/signup/send-otp")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
