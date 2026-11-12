package com.myapp.aqdeeha.support;

import com.myapp.aqdeeha.modules.admin.entity.Admin;
import com.myapp.aqdeeha.modules.admin.service.AdminService;
import com.myapp.aqdeeha.modules.auth.service.AuthTokenService;
import com.myapp.aqdeeha.modules.user.entity.User;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.Gender;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserRole;
import com.myapp.aqdeeha.modules.user.entity.UserEnums.UserStatus;
import com.myapp.aqdeeha.modules.user.service.UserService;
import com.myapp.aqdeeha.modules.setting.service.ApplicationConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Full-stack tests (HTTP → security → DB) against the separate {@code myapp_test} database.
 * Every test starts from empty tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
public abstract class IntegrationTestBase {

    private static final String[] TABLES = {
            "terms_acceptances", "notifications", "audit_log", "shops", "categories", "auth_tokens", "otp_codes", "courier_profiles", "users", "admins"};

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected JsonMapper json;
    @Autowired protected UserService userService;
    @Autowired protected AdminService adminService;
    @Autowired protected AuthTokenService tokenService;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected RecordingWhatsAppSender whatsApp;
    @Autowired protected FakeDistanceService distance;
    @Autowired protected ApplicationConfigService configService;

    /** application_config is seeded by Flyway, so it is not truncated: values a test changes are restored after it. */
    private final Map<String, String> configBefore = new HashMap<>();

    @BeforeEach
    void cleanDatabase() {
        whatsApp.clear();
        distance.reset();
        configBefore.clear();
        jdbc.query("SELECT config_key, value FROM application_config",
                rs -> { configBefore.put(rs.getString(1), rs.getString(2)); });
        configService.evict();
        jdbc.execute((ConnectionCallback<Void>) con -> {
            try (Statement st = con.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String t : TABLES) {
                    st.execute("TRUNCATE TABLE " + t);
                }
                // terms_versions is seeded by Flyway: keep version 1 per audience, drop versions tests published
                st.execute("DELETE FROM terms_versions WHERE version > 1");
                st.execute("UPDATE terms_versions SET is_current = TRUE WHERE version = 1");
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }

    @AfterEach
    void restoreConfig() {
        configBefore.forEach((k, v) -> jdbc.update(
                "UPDATE application_config SET value = ?, updated_by = NULL WHERE config_key = ?", v, k));
        configService.evict();
    }

    /** Changes an application_config value for this test only (restored afterwards). */
    protected void setConfig(String key, String value) {
        jdbc.update("UPDATE application_config SET value = ? WHERE config_key = ?", value, key);
        configService.evict();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String customerToken(String phone) {
        Instant now = Instant.now();
        User u = User.create(UserRole.CUSTOMER, phone, passwordEncoder.encode("Passw0rd!"), "Test Customer",
                LocalDate.of(1995, 1, 1), now, now, UserStatus.ACTIVE);
        u.setGender(Gender.MALE);
        return tokenService.issueForUser(userService.create(u)).value();
    }

    protected String superAdminToken() {
        Admin a = adminService.createSuperAdmin("Super Admin", "super@test.com", "Sup3rAdmin!");
        return tokenService.issueForAdmin(a).value();
    }

    protected String adminToken(String email) {
        Admin a = adminService.createAdmin("Admin", email, "Adm1nPass!", null);
        return tokenService.issueForAdmin(a).value();
    }

    /** A valid 1x1 PNG. */
    protected static byte[] png() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            chunk(out, "IHDR", ByteBuffer.allocate(13).putInt(1).putInt(1).put((byte) 8).put((byte) 2)
                    .put((byte) 0).put((byte) 0).put((byte) 0).array());
            Deflater d = new Deflater();
            d.setInput(new byte[]{0, (byte) 0xFF, 0, 0});
            d.finish();
            byte[] buf = new byte[64];
            int n = d.deflate(buf);
            byte[] idat = new byte[n];
            System.arraycopy(buf, 0, idat, 0, n);
            chunk(out, "IDAT", idat);
            chunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws Exception {
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        byte[] t = type.getBytes();
        out.write(t);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
