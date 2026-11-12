package com.myapp.aqdeeha.modules.terms;

import com.myapp.aqdeeha.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermsFlowTest extends IntegrationTestBase {

    private static final String PHONE = "+201001112222";
    private static final String PASSWORD = "Passw0rd!";
    private static final String UA = "EqdehaApp/1.0 (Android 15)";

    private ResultActions sendOtp(String body) throws Exception {
        return mvc.perform(post("/api/v1/auth/customer/signup/send-otp").header("User-Agent", UA)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** Full customer signup accepting terms version 1; returns the access token. */
    private String signUpCustomer() throws Exception {
        sendOtp("{\"phone\":\"" + PHONE + "\",\"acceptTerms\":true,\"termsVersion\":1}").andExpect(status().isOk());
        JsonNode verified = body(mvc.perform(post("/api/v1/auth/customer/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"code\":\"" + whatsApp.lastCode(PHONE) + "\"}"))
                .andExpect(status().isOk()));
        JsonNode auth = body(mvc.perform(multipart("/api/v1/auth/customer/signup/complete")
                        .param("verificationToken", verified.get("verificationToken").asString())
                        .param("fullName", "Ahmed Ali")
                        .param("password", PASSWORD)
                        .param("birthDate", "1995-05-20")
                        .param("gender", "male"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.termsUpToDate").value(true)));
        return auth.get("accessToken").asString();
    }

    @Test
    void currentTerms_arePublic_perAudience() throws Exception {
        mvc.perform(get("/api/v1/terms").param("audience", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audience").value("CUSTOMER"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.content").value(startsWith("## ")));
        mvc.perform(get("/api/v1/terms").param("audience", "courier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audience").value("COURIER"));
        mvc.perform(get("/api/v1/terms").param("audience", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TERMS_AUDIENCE"));
    }

    @Test
    void signupStep1_requiresAcceptTermsAndTheCurrentVersion() throws Exception {
        for (String bad : new String[]{
                "{\"phone\":\"" + PHONE + "\"}",
                "{\"phone\":\"" + PHONE + "\",\"acceptTerms\":true}",
                "{\"phone\":\"" + PHONE + "\",\"acceptTerms\":false,\"termsVersion\":1}",
                "{\"phone\":\"" + PHONE + "\",\"acceptTerms\":true,\"termsVersion\":2}"}) {
            sendOtp(bad).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TERMS_NOT_ACCEPTED"));
        }
        assertThat(whatsApp.messages(PHONE)).isEmpty();
    }

    @Test
    void accountCreation_storesAcceptanceWithIpAndUserAgent_loginAndMeAreUpToDate() throws Exception {
        String token = signUpCustomer();

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT a.ip_address, a.user_agent, v.version, v.audience FROM terms_acceptances a
                JOIN terms_versions v ON v.id = a.terms_version_id JOIN users u ON u.id = a.user_id
                WHERE u.phone = ?""", PHONE);
        assertThat(row.get("version")).isEqualTo(1);
        assertThat(row.get("audience")).isEqualTo("CUSTOMER");
        assertThat(row.get("user_agent")).isEqualTo(UA);
        assertThat(row.get("ip_address")).isNotNull();

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.termsUpToDate").value(true));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.termsUpToDate").value(true));
    }

    @Test
    void newVersion_bySuperAdminOnly_audited_usersMustAcceptAgain() throws Exception {
        String customer = signUpCustomer();
        String admin = adminToken("admin@test.com");
        String superAdmin = superAdminToken();
        String newTerms = "{\"audience\":\"CUSTOMER\",\"content\":\"## ١. جديد\\nنص الشروط الجديدة للعملاء.\"}";

        mvc.perform(post("/api/admin/terms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(newTerms))
                .andExpect(status().isForbidden());

        JsonNode v2 = body(mvc.perform(post("/api/admin/terms").header("Authorization", bearer(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(newTerms))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.current").value(true)));
        long v2Id = v2.get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'terms.published'",
                Integer.class)).isEqualTo(1);

        mvc.perform(get("/api/v1/terms").param("audience", "CUSTOMER"))
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/api/v1/terms").param("audience", "COURIER"))      // other audience untouched
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.termsUpToDate").value(false));

        long v1Id = jdbc.queryForObject(
                "SELECT id FROM terms_versions WHERE audience = 'CUSTOMER' AND version = 1", Long.class);
        mvc.perform(post("/api/v1/terms/accept").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"termsVersionId\":" + v1Id + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TERMS_VERSION_INVALID"));

        for (int i = 0; i < 2; i++) { // idempotent
            mvc.perform(post("/api/v1/terms/accept").header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"termsVersionId\":" + v2Id + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.termsUpToDate").value(true))
                    .andExpect(jsonPath("$.currentVersion").value(2));
        }
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.termsUpToDate").value(true));

        mvc.perform(get("/api/admin/terms").param("audience", "CUSTOMER").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].acceptanceCount").value(1))
                .andExpect(jsonPath("$[1].current").value(false));

        // old terms can't be used for a new signup any more
        sendOtp("{\"phone\":\"+201001113333\",\"acceptTerms\":true,\"termsVersion\":1}")
                .andExpect(jsonPath("$.code").value("TERMS_NOT_ACCEPTED"));
    }

    @Test
    void acceptEndpoint_needsALoggedInMobileUser() throws Exception {
        mvc.perform(post("/api/v1/terms/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"termsVersionId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/terms/accept").header("Authorization", bearer(adminToken("a@test.com")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"termsVersionId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptances_areAppendOnly_inTheDatabase() throws Exception {
        signUpCustomer();
        assertThatThrownBy(() -> jdbc.update("UPDATE terms_acceptances SET user_agent = 'x'"))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM terms_acceptances"))
                .hasMessageContaining("append-only");
    }
}
