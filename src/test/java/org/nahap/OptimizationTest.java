package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.visitor.AstPrinter;
import org.nahap.support.CompilerTestUtils;

class OptimizationTest {
    @Test
    void optimizerFoldsConstantArithmetic() {
        String code = """
                program FoldDemo;
                var x: integer;
                begin
                  x := (1 + 2) * 3;
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        ProgramNode optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String tree = new AstPrinter().print(optimized);
        assertTrue(tree.contains("LiteralExpression(INTEGER): 9"));
    }

    @Test
    void optimizerRemovesIfFalseBranch() {
        String code = """
                program IfFold;
                begin
                  if false then
                    WriteLn('A')
                  else
                    WriteLn('B');
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        ProgramNode optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String tree = new AstPrinter().print(optimized);
        assertFalse(tree.contains("IfStatement"));
        assertTrue(tree.contains("LiteralExpression(STRING): 'B'"));
    }

    @Test
    void optimizerRemovesWhileFalseLoop() {
        String code = """
                program WhileFold;
                begin
                  while false do
                    WriteLn('X');
                  WriteLn('ok');
                end.
                """;

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        ProgramNode optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String tree = new AstPrinter().print(optimized);
        assertFalse(tree.contains("WhileStatement"));
        assertTrue(tree.contains("LiteralExpression(STRING): 'ok'"));
    }
}
