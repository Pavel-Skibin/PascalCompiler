package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.stmt.AssignmentStatement;
import org.nahap.ast.stmt.CompoundStatement;
import org.nahap.ast.expr.CastExpression;
import org.nahap.support.CompilerTestUtils;
import org.nahap.support.ConsoleTestWatcher;

@ExtendWith(ConsoleTestWatcher.class)
class SemanticAnalysisTest {
    @Test
    void invalidSemanticProgramReportsDiagnostics() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
      semantic.getDiagnostics().forEach(d -> System.out.println("[SEMANTIC][ERROR] " + d.getMessage()));
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
        System.out.println("[SEMANTIC] CastExpression inserted for integer -> double assignment");

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
        System.out.println("[SEMANTIC] valid_loops_io.pas diagnostics=" + semantic.getDiagnostics().size());
        assertFalse(semantic.hasErrors());
    }

    @Test
    void invalidFileProgramProcedureUsedAsFunctionReportsDiagnostic() throws Exception {
      var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic_proc_as_function_file.pas"));
      assertFalse(parsed.syntaxErrors().hasErrors());

      var semantic = CompilerTestUtils.semantic(parsed.program());
      semantic.getDiagnostics().forEach(d -> System.out.println("[SEMANTIC][ERROR] " + d.getMessage()));
      assertTrue(semantic.hasErrors());
      assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("Procedure used as function")));
    }

    @Test
    void invalidFileProgramWrongArgumentTypeReportsDiagnostic() throws Exception {
      var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic_wrong_argument_type_file.pas"));
      assertFalse(parsed.syntaxErrors().hasErrors());

      var semantic = CompilerTestUtils.semantic(parsed.program());
      semantic.getDiagnostics().forEach(d -> System.out.println("[SEMANTIC][ERROR] " + d.getMessage()));
      assertTrue(semantic.hasErrors());
      assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("Type mismatch in argument 1")));
    }

    @Test
    void invalidFileProgramWrongArgumentCountReportsDiagnostic() throws Exception {
      var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic_wrong_argument_count_file.pas"));
      assertFalse(parsed.syntaxErrors().hasErrors());

      var semantic = CompilerTestUtils.semantic(parsed.program());
      semantic.getDiagnostics().forEach(d -> System.out.println("[SEMANTIC][ERROR] " + d.getMessage()));
      assertTrue(semantic.hasErrors());
      assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("Wrong argument count")));
    }

    @Test
    void invalidFileProgramIfConditionNotBooleanReportsDiagnostic() throws Exception {
      var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic_if_condition_not_boolean.pas"));
      assertFalse(parsed.syntaxErrors().hasErrors());

      var semantic = CompilerTestUtils.semantic(parsed.program());
      semantic.getDiagnostics().forEach(d -> System.out.println("[SEMANTIC][ERROR] " + d.getMessage()));
      assertTrue(semantic.hasErrors());
      assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("Type mismatch in if condition")));
    }

    @Test
    void invalidFileProgramForIteratorNotIntegerReportsDiagnostic() throws Exception {
      var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic_for_iterator_not_integer.pas"));
      assertFalse(parsed.syntaxErrors().hasErrors());

      var semantic = CompilerTestUtils.semantic(parsed.program());
      semantic.getDiagnostics().forEach(d -> System.out.println("[SEMANTIC][ERROR] " + d.getMessage()));
      assertTrue(semantic.hasErrors());
      assertTrue(semantic.getDiagnostics().stream().anyMatch(d -> d.getMessage().contains("For-loop variable must be integer")));
    }

    @Test
    void validFileProgramInjectsCastForIntegerToDoubleAssignment() throws Exception {
      var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_semantic_cast_assignment_file.pas"));
      assertFalse(parsed.syntaxErrors().hasErrors());

      var semantic = CompilerTestUtils.semantic(parsed.program());
      assertFalse(semantic.hasErrors());
      System.out.println("[SEMANTIC] valid_semantic_cast_assignment_file.pas diagnostics=" + semantic.getDiagnostics().size());

      ProgramNode transformed = semantic.getProgram();
      CompoundStatement body = transformed.getBlock().getBody();
      AssignmentStatement assignmentToDouble = (AssignmentStatement) body.getStatements().get(1);
      assertTrue(assignmentToDouble.getValue() instanceof CastExpression);
    }
}
