package com.myapp.aqdeeha.common.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Helpers for paging/search request parameters. */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    /** 0-based page, size clamped to 1..100. Sorting is decided by each query, never by the client. */
    public static Pageable of(Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s);
    }

    /** Trimmed search text, or null when blank. */
    public static String search(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    /** {@code %text%} lower-cased for a LIKE on a lower()'d column, with LIKE wildcards escaped. */
    public static String containsPattern(String q) {
        if (q == null) {
            return null;
        }
        String escaped = q.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
