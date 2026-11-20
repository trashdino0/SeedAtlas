package dev.seedatlas.core.validation;

import java.util.List;

public record ValidationResult(List<String> errors, List<String> warnings) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public static ValidationResult ok() {
        return new ValidationResult(List.of(), List.of());
    }
}
