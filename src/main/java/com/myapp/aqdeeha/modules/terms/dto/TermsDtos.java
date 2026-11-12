package com.myapp.aqdeeha.modules.terms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TermsDtos {

    private TermsDtos() {
    }

    /**
     * Current terms for the apps. {@code content}: Arabic text; a line starting with {@code "## "} is a
     * section title, the following lines are its body. Send {@code version} back at signup step 1.
     */
    public record TermsResponse(Long id, String audience, int version, String content, Instant publishedAt) {
    }

    /** POST /api/v1/terms/accept — the id from {@code GET /api/v1/terms}. */
    public record AcceptTermsRequest(@NotNull(message = "validation.required") Long termsVersionId) {
    }

    public record TermsStatusResponse(boolean termsUpToDate, Long currentTermsVersionId, int currentVersion) {
    }

    /** POST /api/admin/terms — publishes a new version and makes it current. */
    public record PublishTermsRequest(
            @NotBlank(message = "validation.required")
            @Pattern(regexp = "(?i)^(customer|courier)$", message = "validation.terms_audience")
            String audience,

            @NotBlank(message = "validation.required")
            @Size(min = 20, max = 20000, message = "validation.terms_content_size")
            String content) {
    }

    public record AdminTermsResponse(Long id, String audience, int version, String content, Instant publishedAt,
                                     boolean current, Long createdBy, long acceptanceCount) {
    }
}
