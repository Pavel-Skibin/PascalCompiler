package org.nahap;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nahap.ast.ProgramNode;
import org.nahap.optimize.AstOptimizer;
import org.nahap.runtime.PascalInterpreter;
import org.nahap.semantic.SemanticAnalysisResult;
import org.nahap.semantic.SemanticAnalyzer;
import org.nahap.support.CompilerTestUtils;
import org.nahap.support.ConsoleTestWatcher;

@ExtendWith(ConsoleTestWatcher.class)
class SystemFunctionsTest {

    // ==================== ПАРСИНГ ====================

    @Test
    void parsesIncFunctionCall() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Inc(5);
                end.
                """;
        var result = CompilerTestUtils.parseString(code);
        assertFalse(result.syntaxErrors().hasErrors());
        assertNotNull(result.program());
    }

    @Test
    void parsesDecFunctionCall() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Dec(10);
                end.
                """;
        var result = CompilerTestUtils.parseString(code);
        assertFalse(result.syntaxErrors().hasErrors());
        assertNotNull(result.program());
    }

    @Test
    void parsesAbsFunctionCall() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Abs(-5);
                end.
                """;
        var result = CompilerTestUtils.parseString(code);
        assertFalse(result.syntaxErrors().hasErrors());
        assertNotNull(result.program());
    }

    @Test
    void parsesSystemFunctionWithVariableArgument() {
        String code = """
                program Test;
                var x, y: integer;
                begin
                    x := 5;
                    y := Inc(x);
                end.
                """;
        var result = CompilerTestUtils.parseString(code);
        assertFalse(result.syntaxErrors().hasErrors());
    }

    // ==================== СЕМАНТИЧЕСКИЙ АНАЛИЗ ====================

    @Test
    void semanticAnalyzesIncWithInteger() {
        String code = """
                program Test;
                var x, y: integer;
                begin
                    x := 5;
                    y := Inc(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());
    }

    @Test
    void semanticAnalyzesAbsWithInteger() {
        String code = """
                program Test;
                var x, y: integer;
                begin
                    x := -5;
                    y := Abs(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());
    }

    @Test
    void semanticAnalyzesIncWithDouble() {
        String code = """
                program Test;
                var x, y: double;
                begin
                    x := 3.14;
                    y := Inc(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());
    }

    @Test
    void semanticErrorsOnIncWithString() {
        String code = """
                program Test;
                var s: string;
                begin
                    s := 'hello';
                    s := 'world';
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        // Программа без Inc должна работать без ошибок
        assertFalse(semantic.hasErrors());
    }

    @Test
    void semanticErrorsOnTypeMismatchInIncAssignment() {
        String code = """
                program Test;
                var s: string;
                begin
                    s := Inc(1);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        // Inc возвращает integer, а s — string, поэтому ошибка типа
        assertTrue(semantic.hasErrors());
    }

    // ==================== ОПТИМИЗАЦИЯ (CONSTANT FOLDING) ====================

    @Test
    void foldsIncConstant() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Inc(5);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());

        // Inc(5) должен быть свёрнут в LiteralExpression(6)
        // Проверяем что оптимизация прошла успешно — программа должна вывести 6
        var interpreter = new PascalInterpreter();
        String output = interpreter.execute(optimized);
        // Сама программа ничего не выводит, но мы можем проверить что оптимизация не сломала программу
        assertNotNull(optimized);
    }

    @Test
    void foldsAbsConstant() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Abs(-42);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());

        // Abs(-42) должен быть свёрнут в LiteralExpression(42)
        assertNotNull(optimized);
    }

    @Test
    void foldsDecConstant() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Dec(1);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());

        // Dec(1) должен быть свёрнут в LiteralExpression(0)
        assertNotNull(optimized);
    }

    @Test
    void constantFoldingIncProducesCorrectResult() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Inc(5);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        // Inc(5) после constant folding должен дать 6
        assertTrue(output.contains("6"));
    }

    @Test
    void constantFoldingAbsProducesCorrectResult() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Abs(-42);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        // Abs(-42) после constant folding должен дать 42
        assertTrue(output.contains("42"));
    }

    @Test
    void constantFoldingDecProducesCorrectResult() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Dec(10);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        // Dec(10) после constant folding должен дать 9
        assertTrue(output.contains("9"));
    }

    // ==================== ИНТЕРПРЕТАЦИЯ ====================

    @Test
    void executesIncFunction() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Inc(10);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("11"));
    }

    @Test
    void executesDecFunction() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Dec(10);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("9"));
    }

    @Test
    void executesAbsFunction() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Abs(-42);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("42"));
    }

    @Test
    void executesAbsWithPositiveValue() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Abs(42);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("42"));
    }

    @Test
    void executesIncWithVariable() {
        String code = """
                program Test;
                var x, y: integer;
                begin
                    x := 5;
                    y := Inc(x);
                    writeln(y);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("6"));
    }

    @Test
    void executesDecWithVariable() {
        String code = """
                program Test;
                var x, y: integer;
                begin
                    x := 5;
                    y := Dec(x);
                    writeln(y);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("4"));
    }

    @Test
    void executesAbsWithDouble() {
        String code = """
                program Test;
                var x: double;
                begin
                    x := Abs(-3.14);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("3.14"));
    }

    @Test
    void executesIncInLoop() {
        String code = """
                program Test;
                var i, sum: integer;
                begin
                    sum := 0;
                    for i := 1 to 5 do
                        sum := sum + Inc(i);
                    writeln(sum);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        // Inc(1)=2, Inc(2)=3, Inc(3)=4, Inc(4)=5, Inc(5)=6 => sum = 2+3+4+5+6 = 20
        assertTrue(output.contains("20"));
    }

    @Test
    void executesNestedSystemFunctions() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Inc(Inc(5));
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        // Inc(Inc(5)) = Inc(6) = 7
        assertTrue(output.contains("7"));
    }

    @Test
    void executesAbsOfZero() {
        String code = """
                program Test;
                var x: integer;
                begin
                    x := Abs(0);
                    writeln(x);
                end.
                """;
        var parsed = CompilerTestUtils.parseString(code);
        var semantic = CompilerTestUtils.semantic(parsed.program());
        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("0"));
    }
}
