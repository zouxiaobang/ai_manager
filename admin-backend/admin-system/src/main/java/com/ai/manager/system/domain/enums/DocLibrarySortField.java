package com.ai.manager.system.domain.enums;

import lombok.Getter;

@Getter
public enum DocLibrarySortField {

    NAME("name", true),
    SIZE("file_size", false),
    UPDATE_TIME("update_time", false);

    private final String nameField;
    private final Boolean asc;

    DocLibrarySortField(String nameField, Boolean asc) {
        this.nameField = nameField;
        this.asc = asc;
    }

    public String nameField() {
        return nameField;
    }

    public Boolean asc() {
        return asc;
    }
}
