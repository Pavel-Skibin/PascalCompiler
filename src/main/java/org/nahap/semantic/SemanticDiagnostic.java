package org.nahap.semantic;

import java.util.Objects;

public final class SemanticDiagnostic {
    private final String message;

    public SemanticDiagnostic(String message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
