package dev.seedatlas.core.model;

public record ProjectInfo(String name, String notes) {
    public ProjectInfo {
        name = name == null ? "" : name;
        notes = notes == null ? "" : notes;
    }
}
