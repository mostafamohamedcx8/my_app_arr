package com.myapp.aqdeeha.modules.courier;

import com.myapp.aqdeeha.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pending courier can't log in → admin approves → courier logs in → /me. Plus reject/needs_fix rules. */
class CourierApprovalFlowTest extends IntegrationTestBase {

    private static final String PHONE = "01112345678";
    private static final String PHONE_E164 = "+201112345678";
    private static final String PASSWORD = "Passw0rd!";

    @Test
    void pendingCourier_cannotLogin_untilApproved() throws Exception {
        byte[] idFront = png();
        long courierId = signUpCourier(idFront);

        login().andExpectStatus(403, "COURIER_PENDING_APPROVAL");

        String admin = superAdminToken();

        // list pending couriers
        mvc.perform(get("/api/admin/couriers").param("status", "pending").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(courierId))
                .andExpect(jsonPath("$.items[0].approvalStatus").value("pending"))
                .andExpect(jsonPath("$.items[0].idCardFrontUrl").doesNotExist());

        // search by phone and by name
        mvc.perform(get("/api/admin/couriers").param("q", "0111234").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/admin/couriers").param("q", "محمود").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/admin/couriers").param("status", "approved").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/admin/couriers").param("status", "bogus").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FILTER"));

        // full details + decrypted ID image
        mvc.perform(get("/api/admin/couriers/{id}", courierId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType").value("motorcycle"))
                .andExpect(jsonPath("$.idCardFrontDownloadUrl").value("/api/admin/couriers/" + courierId + "/id-card/front"))
                .andExpect(jsonPath("$.idCardFrontUrl").value(org.hamcrest.Matchers.startsWith("/api/files/id-cards/" + courierId + "/front?")))
                .andExpect(jsonPath("$.idCardUrlsExpireAt").isNotEmpty());
        mvc.perform(get("/api/admin/couriers/{id}/id-card/front", courierId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().bytes(idFront));
        mvc.perform(get("/api/admin/couriers/{id}/id-card/front", courierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken("+201000000009"))))
                .andExpect(status().isForbidden());

        // rejecting without a note is refused
        decide(admin, courierId, Map.of("status", "rejected"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REVIEW_NOTE_REQUIRED"));

        // approve
        decide(admin, courierId, Map.of("status", "approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("approved"))
                .andExpect(jsonPath("$.accountStatus").value("active"));

        String courierToken = login().andExpectStatus(200, null).path("accessToken").asString();

        mvc.perform(get("/api/v1/courier/me").header(HttpHeaders.AUTHORIZATION, bearer(courierToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courierId))
                .andExpect(jsonPath("$.approvalStatus").value("approved"))
                .andExpect(jsonPath("$.vehicleColor").value("أسود"))
                .andExpect(jsonPath("$.idCardFrontUrl").doesNotExist())
                .andExpect(jsonPath("$.idCardBackUrl").doesNotExist());

        // audit trail
        Map<String, Object> audit = jdbc.queryForMap("select * from audit_log where target_type='courier' and target_id=?", courierId);
        assertThat(audit.get("action")).isEqualTo("courier.approval_changed");
        assertThat((String) audit.get("details")).contains("\"from\":\"pending\"").contains("\"to\":\"approved\"");
    }

    @Test
    void rejectingAnApprovedCourier_revokesTokens_andBlocksLogin() throws Exception {
        long courierId = signUpCourier(png());
        String admin = adminToken("reviewer@test.com");
        decide(admin, courierId, Map.of("status", "approved")).andExpect(status().isOk());
        String courierToken = login().andExpectStatus(200, null).path("accessToken").asString();

        decide(admin, courierId, Map.of("status", "rejected", "note", "صورة البطاقة مش واضحة"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewNote").value("صورة البطاقة مش واضحة"))
                .andExpect(jsonPath("$.accountStatus").value("pending"));

        mvc.perform(get("/api/v1/courier/me").header(HttpHeaders.AUTHORIZATION, bearer(courierToken)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        login().andExpectStatus(403, "COURIER_REJECTED");

        decide(admin, courierId, Map.of("status", "needs_fix", "note", "ارفع صورة أوضح", "fields", List.of("idCardBack")))
                .andExpect(status().isOk());
        // password login still works for needs_fix and gives the limited token
        assertThat(login().andExpectStatus(200, null).path("nextStep").asString()).isEqualTo("fix_application");

        assertThat(jdbc.queryForObject("select count(*) from audit_log where target_id=?", Long.class, courierId))
                .isEqualTo(3);
    }

    @Test
    void needsFix_phoneCodeAccess_onlyFlaggedFields_resubmit_signedLinks_approve() throws Exception {
        long courierId = signUpCourier(png());
        String admin = superAdminToken();

        // needs_fix must name valid fields
        decide(admin, courierId, Map.of("status", "needs_fix", "note", "عدل"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FIX_FIELDS_REQUIRED"));
        decide(admin, courierId, Map.of("status", "needs_fix", "note", "عدل", "fields", List.of("bogus")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FIX_FIELD"));
        decide(admin, courierId, Map.of("status", "needs_fix", "note", "صورة البطاقة من قدام مش واضحة",
                        "fields", List.of("wallet", "idCardFront")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixFields.length()").value(2));

        // WhatsApp carried nothing but the signup code: the decision is stored, not sent
        assertThat(whatsApp.messages(PHONE_E164)).hasSize(1).allMatch(m -> m.startsWith("signup:"));

        // open the application with the phone number + WhatsApp code
        mvc.perform(post("/api/v1/auth/courier/fix/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01099999999\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTHING_TO_FIX"));
        mvc.perform(post("/api/v1/auth/courier/fix/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\"}"))
                .andExpect(status().isOk());
        assertThat(whatsApp.messages(PHONE_E164).getLast()).startsWith("courier_fix:");
        MvcResult access = mvc.perform(post("/api/v1/auth/courier/fix/verify-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"code\":\"" + whatsApp.lastCode(PHONE_E164) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("fix_application"))
                .andReturn();
        String applicant = json.readTree(access.getResponse().getContentAsString()).path("accessToken").asString();

        // ONLY the flagged fields are shown
        mvc.perform(get("/api/v1/courier/application").header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewNote").value("صورة البطاقة من قدام مش واضحة"))
                .andExpect(jsonPath("$.requiredFields.length()").value(2))
                .andExpect(jsonPath("$.requiredFields[0].field").value("idCardFront"))
                .andExpect(jsonPath("$.requiredFields[0].inputType").value("image"))
                .andExpect(jsonPath("$.requiredFields[0].currentValue").doesNotExist())
                .andExpect(jsonPath("$.requiredFields[1].field").value("wallet"))
                .andExpect(jsonPath("$.requiredFields[1].formKeys[0]").value("walletType"))
                .andExpect(jsonPath("$.requiredFields[1].formKeys[1]").value("walletNumber"))
                .andExpect(jsonPath("$.requiredFields[1].currentValue.walletNumber").value("01012345678"));
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications.items[0].type").value("courier_needs_fix"))
                .andExpect(jsonPath("$.notifications.items[0].body").value(org.hamcrest.Matchers.containsString("صورة البطاقة من قدام")));
        // the limited token reaches nothing else
        mvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/courier/wallet").header(HttpHeaders.AUTHORIZATION, bearer(applicant))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"walletType\":\"vodafone_cash\",\"walletNumber\":\"01012345678\"}"))
                .andExpect(status().isForbidden());

        // every flagged field is required
        byte[] newFront = png();
        mvc.perform(multipart("/api/v1/courier/application/resubmit")
                        .file(new MockMultipartFile("idCardFront", "f2.png", "image/png", newFront))
                        .header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.containsInAnyOrder("walletType", "walletNumber")));

        // complete resubmission; a field that was not flagged (fullName) is ignored
        mvc.perform(multipart("/api/v1/courier/application/resubmit")
                        .file(new MockMultipartFile("idCardFront", "f2.png", "image/png", newFront))
                        .param("walletType", "instapay").param("walletNumber", "Mahmoud.Ali@InstaPay")
                        .param("fullName", "اسم مش مطلوب")
                        .header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("pending"))
                .andExpect(jsonPath("$.resubmissionCount").value(1));

        // session ended; waiting for review
        mvc.perform(get("/api/v1/courier/application").header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
                .andExpect(status().isUnauthorized());
        login().andExpectStatus(403, "COURIER_PENDING_APPROVAL");

        // admin sees the resubmission, history (with the flagged fields) and signed links
        mvc.perform(get("/api/admin/couriers").param("status", "pending").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.items[0].resubmissionCount").value(1))
                .andExpect(jsonPath("$.items[0].hasWallet").value(true));
        MvcResult detail = mvc.perform(get("/api/admin/couriers/{id}", courierId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("محمود علي"))
                .andExpect(jsonPath("$.walletType").value("instapay"))
                .andExpect(jsonPath("$.walletNumber").value("mahmoud.ali@instapay"))
                .andExpect(jsonPath("$.fixFields.length()").value(0))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].fields[0]").value("idCardFront"))
                .andExpect(jsonPath("$.history[0].fields[1]").value("wallet"))
                .andExpect(jsonPath("$.history[0].adminName").value("Super Admin"))
                .andReturn();
        String signedFront = json.readTree(detail.getResponse().getContentAsString()).path("idCardFrontUrl").asString();

        // signed link works WITHOUT a token (for <img src>), tampering breaks it
        mvc.perform(get(signedFront))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(newFront))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
        mvc.perform(get(signedFront.replace("/front?", "/back?")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SIGNED_URL_INVALID"));
        mvc.perform(get(signedFront.replaceAll("exp=\\d+", "exp=9999999999")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SIGNED_URL_INVALID"));
        mvc.perform(get(signedFront.replaceAll("exp=\\d+", "exp=1000")))
                .andExpect(status().isForbidden());

        // approve: wallet was given at signup, so nothing else to do
        decide(admin, courierId, Map.of("status", "approved")).andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(2));
        JsonNode approvedLogin = login().andExpectStatus(200, null);
        assertThat(approvedLogin.path("nextStep").isNull()).isTrue();
        String courier = approvedLogin.path("accessToken").asString();
        mvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, bearer(courier)))
                .andExpect(jsonPath("$.notifications.totalItems").value(2))
                .andExpect(jsonPath("$.notifications.items[0].type").value("courier_approved"));
        // still only OTPs ever went to WhatsApp
        assertThat(whatsApp.messages(PHONE_E164)).allMatch(m -> m.startsWith("signup:") || m.startsWith("courier_fix:"));

        // the courier can change the wallet later
        mvc.perform(put("/api/v1/courier/wallet").header(HttpHeaders.AUTHORIZATION, bearer(courier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"walletType\":\"vodafone_cash\",\"walletNumber\":\"12345\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_WALLET_NUMBER"));
        mvc.perform(put("/api/v1/courier/wallet").header(HttpHeaders.AUTHORIZATION, bearer(courier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"walletType\":\"vodafone_cash\",\"walletNumber\":\"+20 101 234 5678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletNumber").value("01012345678"));
    }

    @Test
    void signupRequiresWallet() throws Exception {
        mvc.perform(post("/api/v1/auth/courier/signup/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"acceptTerms\":true,\"termsVersion\":1}"))
                .andExpect(status().isOk());
        MvcResult verified = mvc.perform(post("/api/v1/auth/courier/signup/verify-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"code\":\"" + whatsApp.lastCode(PHONE_E164) + "\"}"))
                .andReturn();
        String token = json.readTree(verified.getResponse().getContentAsString()).path("verificationToken").asString();
        mvc.perform(multipart("/api/v1/auth/courier/signup/complete")
                        .file(new MockMultipartFile("photo", "p.png", "image/png", png()))
                        .file(new MockMultipartFile("idCardFront", "f.png", "image/png", png()))
                        .file(new MockMultipartFile("idCardBack", "b.png", "image/png", png()))
                        .file(new MockMultipartFile("vehiclePhoto", "v.png", "image/png", png()))
                        .param("verificationToken", token).param("fullName", "محمود علي").param("password", PASSWORD)
                        .param("birthDate", "1995-01-15").param("address", "عزبة سكينة")
                        .param("vehicleType", "motorcycle").param("vehicleColor", "أسود"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.containsInAnyOrder("walletType", "walletNumber")));
    }

    @Test
    void signupPlateNumber_lettersAndDigits_max8_spacesNotCounted() throws Exception {
        String token = verifiedSignupToken();
        mvc.perform(courierComplete(token).param("plateNumber", "سعد١٢٣٤٥٦"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plateNumber"));
        mvc.perform(courierComplete(token).param("plateNumber", "س-ع-د"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plateNumber"));

        MvcResult created = mvc.perform(courierComplete(token).param("plateNumber", " س  ع د ١٢٣٤ "))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).path("userId").asLong();
        assertThat(jdbc.queryForObject("SELECT plate_number FROM courier_profiles WHERE user_id = ?", String.class, id))
                .isEqualTo("س ع د ١٢٣٤");
    }

    // ---------------------------------------------------------------- helpers

    private String verifiedSignupToken() throws Exception {
        mvc.perform(post("/api/v1/auth/courier/signup/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"acceptTerms\":true,\"termsVersion\":1}"))
                .andExpect(status().isOk());
        MvcResult verified = mvc.perform(post("/api/v1/auth/courier/signup/verify-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"code\":\"" + whatsApp.lastCode(PHONE_E164) + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(verified.getResponse().getContentAsString()).path("verificationToken").asString();
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder courierComplete(String token)
            throws Exception {
        return multipart("/api/v1/auth/courier/signup/complete")
                .file(new MockMultipartFile("photo", "p.png", "image/png", png()))
                .file(new MockMultipartFile("idCardFront", "f.png", "image/png", png()))
                .file(new MockMultipartFile("idCardBack", "b.png", "image/png", png()))
                .file(new MockMultipartFile("vehiclePhoto", "v.png", "image/png", png()))
                .param("verificationToken", token).param("fullName", "محمود علي").param("password", PASSWORD)
                .param("birthDate", "1995-01-15").param("address", "عزبة سكينة")
                .param("vehicleType", "motorcycle").param("vehicleColor", "أسود")
                .param("walletType", "vodafone_cash").param("walletNumber", "01012345678");
    }

    private long signUpCourier(byte[] idFront) throws Exception {
        mvc.perform(post("/api/v1/auth/courier/signup/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"acceptTerms\":true,\"termsVersion\":1}"))
                .andExpect(status().isOk());
        MvcResult verified = mvc.perform(post("/api/v1/auth/courier/signup/verify-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + PHONE + "\",\"code\":\"" + whatsApp.lastCode(PHONE_E164) + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(verified.getResponse().getContentAsString()).path("verificationToken").asString();

        MvcResult created = mvc.perform(multipart("/api/v1/auth/courier/signup/complete")
                        .file(new MockMultipartFile("photo", "p.png", "image/png", png()))
                        .file(new MockMultipartFile("idCardFront", "f.png", "image/png", idFront))
                        .file(new MockMultipartFile("idCardBack", "b.png", "image/png", png()))
                        .file(new MockMultipartFile("vehiclePhoto", "v.png", "image/png", png()))
                        .param("verificationToken", token)
                        .param("fullName", "محمود علي")
                        .param("password", PASSWORD)
                        .param("birthDate", "1995-01-15")
                        .param("address", "عزبة سكينة")
                        .param("vehicleType", "motorcycle")
                        .param("vehicleColor", "أسود")
                        .param("walletType", "vodafone_cash")
                        .param("walletNumber", "01012345678"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalStatus").value("pending"))
                .andReturn();
        return json.readTree(created.getResponse().getContentAsString()).path("userId").asLong();
    }

    private LoginCall login() {
        return new LoginCall();
    }

    private class LoginCall {
        JsonNode andExpectStatus(int expected, String code) throws Exception {
            MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"" + PHONE + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andReturn();
            JsonNode body = json.readTree(r.getResponse().getContentAsString());
            assertThat(r.getResponse().getStatus()).as(body.toString()).isEqualTo(expected);
            if (code != null) {
                assertThat(body.path("code").asString()).isEqualTo(code);
            }
            return body;
        }
    }

    private org.springframework.test.web.servlet.ResultActions decide(String adminToken, long courierId,
                                                                      Map<String, ?> body) throws Exception {
        return mvc.perform(patch("/api/admin/couriers/{id}/approval", courierId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }
}
