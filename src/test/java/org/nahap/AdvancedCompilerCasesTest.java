package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.nahap.runtime.PascalInterpreter;
import org.nahap.support.CompilerTestUtils;

class AdvancedCompilerCasesTest {
    @Test
    void arraysWorkWithinBounds() {
        String code = """
                program ArrayOk;
                var
                  arr: array [1..3] of integer;
                  i: integer;
                begin
                  for i := 1 to 3 do
                    arr[i] := i * 10;
                  WriteLn(arr[1], ' ', arr[2], ' ', arr[3]);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);
        assertTrue(output.contains("10 20 30"));
    }

    @Test
    void runtimeReportsArrayOutOfBounds() {
        String code = """
                program ArrayBad;
                var
                  arr: array [1..2] of integer;
                begin
                  arr[3] := 10;
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new PascalInterpreter().execute(optimized));
        assertTrue(ex.getMessage().contains("Array index out of bounds"));
    }

    @Test
    void recursionComputesFactorial() {
        String code = """
                program RecursionDemo;
                var
                  result: integer;

                function Fact(n: integer): integer;
                begin
                  if n <= 1 then
                    Fact := 1
                  else
                    Fact := n * Fact(n - 1);
                end;

                begin
                  result := Fact(5);
                  WriteLn(result);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);
        assertTrue(output.contains("120"));
    }

    @Test
    void deeperScopesUseNearestVariable() {
        String code = """
                program DeepScopes;
                var
                  x: integer;

                procedure Outer;
                var
                  x: integer;

                  procedure Inner;
                  var
                    x: integer;
                  begin
                    x := 30;
                    WriteLn('inner=', x);
                  end;

                begin
                  x := 20;
                  Inner;
                  WriteLn('outer=', x);
                end;

                begin
                  x := 10;
                  Outer;
                  WriteLn('global=', x);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("inner=30"));
        assertTrue(output.contains("outer=20"));
        assertTrue(output.contains("global=10"));
    }

    @Test
    void semanticReportsProcedureWrongArgumentCount() {
        String code = """
                program BadProcArgs;
                procedure P(a: integer; b: integer);
                begin
                end;

                begin
                  P(1);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
        assertTrue(semantic.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("Wrong argument count for P")));
    }

    @Test
    void semanticReportsFunctionWrongArgumentCount() {
        String code = """
                program BadFuncArgs;
                var x: integer;

                function Add(a: integer; b: integer): integer;
                begin
                  Add := a + b;
                end;

                begin
                  x := Add(1);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
        assertTrue(semantic.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("Wrong argument count for Add")));
    }

    @Test
    void semanticReportsFunctionUsedAsProcedure() {
        String code = """
                program FuncAsProc;
                function Add(a: integer; b: integer): integer;
                begin
                  Add := a + b;
                end;

                begin
                  Add(1, 2);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
        assertTrue(semantic.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("Function used as procedure: Add")));
    }

    @Test
    void semanticReportsProcedureUsedAsFunction() {
        String code = """
                program ProcAsFunc;
                var
                  x: integer;

                procedure P(a: integer);
                begin
                end;

                begin
                  x := P(1);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
        assertTrue(semantic.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("Procedure used as function: P")));
    }

    @Test
    void semanticReportsWrongArgumentType() {
        String code = """
                program ArgTypeMismatch;
                var
                  x: integer;

                function NeedsBool(flag: boolean): integer;
                begin
                  if flag then
                    NeedsBool := 1
                  else
                    NeedsBool := 0;
                end;

                begin
                  x := NeedsBool(1);
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
        assertTrue(semantic.getDiagnostics().stream()
                .anyMatch(d -> d.getMessage().contains("Type mismatch in argument 1 of NeedsBool")));
    }
}
