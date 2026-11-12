package com.myapp.aqdeeha.modules.catalog;

import com.myapp.aqdeeha.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogFlowTest extends IntegrationTestBase {

    @Test
    void categoriesAndShops() throws Exception {
        String admin = adminToken("admin@test.com");
        String customer = customerToken("+201000000010");

        long fish = createCategory(admin, "سمك", "fish", 2, true);
        createCategory(admin, "مشويات", "grill", 1, true);
        long hidden = createCategory(admin, "قسم مخفي", "box", 0, false);

        // duplicate name, missing icon, bad color
        mvc.perform(multipart("/api/admin/categories").param("name", "سمك").param("iconKey", "x").param("color", "#000000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATEGORY_NAME_EXISTS"));
        mvc.perform(multipart("/api/admin/categories").param("name", "حلويات").param("color", "#000000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CATEGORY_ICON_REQUIRED"));
        mvc.perform(multipart("/api/admin/categories").param("name", "حلويات").param("iconKey", "cake").param("color", "red")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("color"));

        // customer sees active only, in sortOrder; admin sees all
        mvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("مشويات"))
                .andExpect(jsonPath("$[1].name").value("سمك"));
        mvc.perform(get("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.length()").value(3));

        // shops
        long shopId = createShop(admin, fish, "سمك أبو علي", "شارع البحر", "00:00", "00:00");
        createShop(admin, fish, "أسماك الصياد", "شارع النيل", "09:00", "23:00");
        createShop(admin, hidden, "محل مخفي", "شارع ١", "09:00", "23:00");

        mvc.perform(multipart("/api/admin/shops").file(new MockMultipartFile("photo", "s.png", "image/png", png()))
                        .param("name", "X").param("categoryId", String.valueOf(fish)).param("street", "st")
                        .param("lat", "31.2").param("lng", "29.9").param("opensAt", "9am").param("closesAt", "23:00")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='opensAt')].message").value("الوقت لازم يكون بالشكل ده 09:30"));

        mvc.perform(get("/api/v1/shops").param("categoryId", String.valueOf(fish)).header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].categoryName").value("سمك"));
        mvc.perform(get("/api/v1/shops").param("categoryId", String.valueOf(fish)).param("q", "النيل")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("أسماك الصياد"));

        mvc.perform(get("/api/v1/shops/{id}", shopId).header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opensAt").value("00:00"))
                .andExpect(jsonPath("$.isOpen").value(true))
                .andExpect(jsonPath("$.openNow").value(true)); // 00:00-00:00 = open 24h

        // inactive category: hidden from customers
        mvc.perform(get("/api/v1/shops").param("categoryId", String.valueOf(hidden)).header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        assertThat(jdbc.queryForObject("select count(*) from audit_log where action='shop.created'", Long.class))
                .isEqualTo(3);
    }

    @Test
    void editAndDelete_adminOnly_softDelete_audited() throws Exception {
        String admin = adminToken("admin@test.com");
        String customer = customerToken("+201000000011");

        long fish = createCategory(admin, "سمك", "fish", 1, true);
        long grill = createCategory(admin, "مشويات", "grill", 2, true);
        long shopId = createShop(admin, fish, "سمك أبو علي", "شارع البحر", "09:00", "23:00");

        // only admins can write
        mvc.perform(multipart("/api/admin/shops").file(new MockMultipartFile("photo", "s.png", "image/png", png()))
                        .param("name", "X shop").param("categoryId", String.valueOf(fish)).param("street", "st")
                        .param("lat", "31.2").param("lng", "29.9").param("opensAt", "09:00").param("closesAt", "23:00")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(HttpMethod.PATCH, "/api/admin/shops/{id}", shopId).param("name", "hack")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/shops/{id}", shopId).header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());

        // edit category (multipart PATCH): rename, recolor, deactivate; upload an icon image
        mvc.perform(multipart(HttpMethod.PATCH, "/api/admin/categories/{id}", fish)
                        .file(new MockMultipartFile("icon", "i.png", "image/png", png()))
                        .param("name", "أسماك").param("color", "#00aa00").param("isActive", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("أسماك"))
                .andExpect(jsonPath("$.color").value("#00AA00"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.iconUrl").value(org.hamcrest.Matchers.startsWith("/files/public/categories/")));
        // name clash, removing the last icon
        mvc.perform(multipart(HttpMethod.PATCH, "/api/admin/categories/{id}", fish).param("name", "مشويات")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
        mvc.perform(multipart(HttpMethod.PATCH, "/api/admin/categories/{id}", grill).param("iconKey", "")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CATEGORY_ICON_REQUIRED"));

        // inactive category: hidden for customers, still visible to admins (with its shops)
        mvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/admin/shops").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].categoryName").value("أسماك"));

        // edit shop: move to another category, change hours and switch
        mvc.perform(multipart(HttpMethod.PATCH, "/api/admin/shops/{id}", shopId)
                        .param("categoryId", String.valueOf(grill)).param("closesAt", "02:00").param("isOpen", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("مشويات"))
                .andExpect(jsonPath("$.closesAt").value("02:00"))
                .andExpect(jsonPath("$.isOpen").value(false))
                .andExpect(jsonPath("$.openNow").value(false));

        // a category with shops cannot be deleted
        mvc.perform(delete("/api/admin/categories/{id}", grill).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATEGORY_HAS_SHOPS"));

        // soft-delete the shop, then the category
        mvc.perform(delete("/api/admin/shops/{id}", shopId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/shops/{id}", shopId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/shops/{id}", shopId).header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/categories/{id}", grill).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.length()").value(1));

        // the rows are still in the DB (soft delete) and the name can be reused
        assertThat(jdbc.queryForObject("select count(*) from shops where deleted_at is not null", Long.class)).isEqualTo(1);
        createCategory(admin, "مشويات", "grill", 3, true);

        assertThat(jdbc.queryForList("select action from audit_log order by id", String.class))
                .contains("category.updated", "shop.updated", "shop.deleted", "category.deleted");
    }

    @Test
    void noLimitOnTheNumberOfCategories() throws Exception {
        String admin = adminToken("admin@test.com");
        String customer = customerToken("+201000000011");
        for (int i = 1; i <= 20; i++) {
            createCategory(admin, "قسم رقم " + i, "fish", i, true);
        }
        mvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }

    private long createCategory(String admin, String name, String iconKey, int sort, boolean active) throws Exception {
        MvcResult r = mvc.perform(multipart("/api/admin/categories")
                        .param("name", name).param("iconKey", iconKey).param("color", "#ff5722")
                        .param("sortOrder", String.valueOf(sort)).param("isActive", String.valueOf(active))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.color").value("#FF5722"))
                .andExpect(jsonPath("$.isActive").value(active))
                .andReturn();
        return json.readValue(r.getResponse().getContentAsString(), Map.class).get("id") instanceof Number n ? n.longValue() : -1;
    }

    private long createShop(String admin, long categoryId, String name, String street, String opens, String closes)
            throws Exception {
        MvcResult r = mvc.perform(multipart("/api/admin/shops")
                        .file(new MockMultipartFile("photo", "s.png", "image/png", png()))
                        .param("name", name).param("categoryId", String.valueOf(categoryId)).param("street", street)
                        .param("lat", "31.2001234").param("lng", "29.9187654")
                        .param("opensAt", opens).param("closesAt", closes)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").value(org.hamcrest.Matchers.startsWith("/files/public/shops/")))
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).path("id").asLong();
    }
}
