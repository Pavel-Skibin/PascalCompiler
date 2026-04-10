package org.nahap.semantic;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.ProgramNode;

public final class SemanticAnalysisResult {
    private final ProgramNode program;
    private final List<SemanticDiagnostic> diagnostics;

    public SemanticAnalysisResult(ProgramNode program, List<SemanticDiagnostic> diagnostics) {
        this.program = Objects.requireNonNull(program, "program");
        this.diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public ProgramNode getProgram() {
        return program;
    }

    public List<SemanticDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public boolean hasErrors() {
        return !diagnostics.isEmpty();
    }
}
