package com.myapp.aqdeeha.modules.audit.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Collects {@code field -> {from, to}} for an audit "updated" entry, ignoring fields that did not change. */
public final class AuditChanges {

    private final Map<String, Object> changes = new LinkedHashMap<>();

    /** Records the change if the values differ; returns {@code true} when it did. */
    public boolean put(String field, Object from, Object to) {
        if (Objects.equals(from == null ? null : from.toString(), to == null ? null : to.toString())) {
            return false;
        }
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("from", from == null ? null : from.toString());
        diff.put("to", to == null ? null : to.toString());
        changes.put(field, diff);
        return true;
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public Map<String, Object> asMap() {
        return changes;
    }
}
