package com.myapp.aqdeeha.security.access;

import com.myapp.aqdeeha.support.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** @Protect / @AllowTo: allowed, forbidden, no token — on probe endpoints and on real ones. */
class AccessControlIntegrationTest extends IntegrationTestBase {

    private ResultActions call(String path, String token) throws Exception {
        var req = get(path);
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return mvc.perform(req);
    }

    private void expectTokenInvalid(ResultActions r) throws Exception {
        r.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private void expectForbidden(ResultActions r) throws Exception {
        r.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("مش مسموحلك تعمل الحركة دي"));
    }

    @Nested
    class NoToken {
        @Test
        void publicEndpointIsReachable() throws Exception {
            call("/api/test/access/public", null).andExpect(status().isOk()).andExpect(content().string("public"));
        }

        @Test
        void protectReturnsTokenInvalid() throws Exception {
            expectTokenInvalid(call("/api/test/access/protected", null));
        }

        @Test
        void allowToReturnsTokenInvalid() throws Exception {
            expectTokenInvalid(call("/api/test/access/admin", null));
        }

        @Test
        void garbageTokenReturnsTokenInvalid() throws Exception {
            expectTokenInvalid(call("/api/test/access/protected", "not.a.jwt"));
        }
    }

    @Test
    void customer_allowedOnProtectAndCustomer_forbiddenOnAdmin() throws Exception {
        String token = customerToken("+201000000001");
        call("/api/test/access/protected", token).andExpect(status().isOk()).andExpect(content().string("CUSTOMER"));
        call("/api/test/access/customer", token).andExpect(status().isOk());
        expectForbidden(call("/api/test/access/admin", token));
        expectForbidden(call("/api/test/access/super", token));
    }

    @Test
    void admin_allowedOnAdmin_forbiddenOnCustomerAndSuper() throws Exception {
        String token = adminToken("admin@test.com");
        call("/api/test/access/admin", token).andExpect(status().isOk()).andExpect(content().string("ADMIN"));
        call("/api/test/access/protected", token).andExpect(status().isOk());
        expectForbidden(call("/api/test/access/customer", token));
        expectForbidden(call("/api/test/access/super", token));
    }

    @Test
    void superAdmin_isAlwaysAllowedWhereAdminIs() throws Exception {
        String token = superAdminToken();
        call("/api/test/access/admin", token).andExpect(status().isOk()).andExpect(content().string("SUPER_ADMIN"));
        call("/api/test/access/super", token).andExpect(status().isOk());
        expectForbidden(call("/api/test/access/customer", token));
    }

    @Test
    void classLevelRule_andMethodOverride() throws Exception {
        String admin = adminToken("admin@test.com");
        expectForbidden(call("/api/test/class/inherited", admin));
        call("/api/test/class/override", admin).andExpect(status().isOk());
        call("/api/test/class/inherited", customerToken("+201000000002")).andExpect(status().isOk());
        expectTokenInvalid(call("/api/test/class/override", null));
    }

    @Test
    void realEndpoints_useTheSameRules() throws Exception {
        String customer = customerToken("+201000000003");
        String admin = adminToken("admin@test.com");

        expectTokenInvalid(call("/api/admin/couriers", null));
        expectForbidden(call("/api/admin/couriers", customer));
        call("/api/admin/couriers", admin).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));

        expectForbidden(call("/api/v1/courier/me", customer));
        call("/api/v1/categories", customer).andExpect(status().isOk());
        call("/api/v1/categories", admin).andExpect(status().isOk());
        expectTokenInvalid(call("/api/v1/categories", null));

        expectForbidden(mvc.perform(post("/api/admin/admins")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X Y\",\"email\":\"x@test.com\",\"password\":\"Adm1nPass!\"}")));
    }
}
