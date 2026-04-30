package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nahap.runtime.PascalInterpreter;
import org.nahap.support.CompilerTestUtils;
import org.nahap.support.ConsoleTestWatcher;

@ExtendWith(ConsoleTestWatcher.class)
class InterpreterShowcaseTest {

    @Test
    void interpretsArithmeticCoreOperations() {
        String code = """
                program ArithmeticShowcase;
                var
                  a, b, c, d, e: integer;
                begin
                  a := 10 + 5;
                  b := a - 3;
                  c := b * 2;
                  d := c div 4;
                  e := c mod 5;
                  WriteLn('arith=', d, ' mod=', e);
                end.
                """;

        String output = execute(code);
        System.out.println("[SHOWCASE][OUTPUT] interpretsArithmeticCoreOperations -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("arith=6 mod=4"));
    }

    @Test
    void interpretsNestedProceduresAndScopes() {
        String code = """
                program NestedShowcase;
                var
                  x: integer;

                procedure Outer;
                var
                  x: integer;

                  procedure Inner;
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

        String output = execute(code);
        System.out.println("[SHOWCASE][OUTPUT] interpretsNestedProceduresAndScopes -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("inner=30"));
        assertTrue(output.contains("outer=30"));
        assertTrue(output.contains("global=10"));
    }

    @Test
    void interpretsRecursion() {
        String code = """
                program RecursionShowcase;
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
                  result := Fact(6);
                  WriteLn('fact=', result);
                end.
                """;

        String output = execute(code);
        System.out.println("[SHOWCASE][OUTPUT] interpretsRecursion -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("fact=720"));
    }

    @Test
    void interpretsStringsAndNumbersOutput() {
        String code = """
                program StringShowcase;
                var
                  title: string;
                  n: integer;
                begin
                  title := 'Pascal';
                  n := 3;
                  WriteLn('title=', title);
                  WriteLn('n=', n);
                end.
                """;

        String output = execute(code);
        System.out.println("[SHOWCASE][OUTPUT] interpretsStringsAndNumbersOutput -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("title=Pascal"));
        assertTrue(output.contains("n=3"));
    }

    @Test
    void interpretsArrayAndLoop() {
        String code = """
                program ArrayLoopShowcase;
                var
                  arr: array [1..3] of integer;
                  i, sum: integer;
                begin
                  sum := 0;
                  for i := 1 to 3 do
                  begin
                    arr[i] := i * 10;
                    sum := sum + arr[i];
                  end;
                  WriteLn('sum=', sum);
                end.
                """;

        String output = execute(code);
        System.out.println("[SHOWCASE][OUTPUT] interpretsArrayAndLoop -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("sum=60"));
    }



    private static String execute(String code) {
        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        return new PascalInterpreter().execute(optimized);
    }
}
