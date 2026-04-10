package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.stmt.AssignmentStatement;
import org.nahap.ast.stmt.CompoundStatement;
import org.nahap.ast.expr.CastExpression;
import org.nahap.support.CompilerTestUtils;

class SemanticAnalysisTest {
    @Test
    void invalidSemanticProgramReportsDiagnostics() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
        assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("Unknown variable: y")));
        assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("break used outside of loop")));
    }

    @Test
    void semanticPassInjectsCastForAssignmentToDouble() {
        String code = """
                program CastDemo;
                var
                  a: integer;
                  b: double;
                begin
                  a := 10;
                  b := a;
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        ProgramNode transformed = semantic.getProgram();
        CompoundStatement body = transformed.getBlock().getBody();
        AssignmentStatement assignmentToDouble = (AssignmentStatement) body.getStatements().get(1);
        assertTrue(assignmentToDouble.getValue() instanceof CastExpression);
    }

    @Test
    void validProgramPassesSemanticAnalysis() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_loops_io.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());
    }
}
