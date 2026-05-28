package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nahap.support.CompilerTestUtils;
import org.nahap.support.ConsoleTestWatcher;
import org.nahap.vm.SimpleVmExecutor;

@ExtendWith(ConsoleTestWatcher.class)
class SimpleVmBackendTest {

    @Test
    void vmExecutesArithmeticAndCondition() {
        String code = """
                program VmArithmeticIf;
                var
                  a, b, c: integer;
                begin
                  a := 10 + 5 - 3;
                  b := a * 2 div 4;
                  c := b mod 4;
                  if b = 6 then
                    WriteLn('ok=', b, ' mod=', c)
                  else
                    WriteLn('bad');
                end.
                """;

        String output = executeWithVm(code);
        System.out.println("[VM][OUTPUT] vmExecutesArithmeticAndCondition -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("ok=6 mod=2"));
    }

    @Test
    void vmExecutesWhileLoop() {
        String code = """
                program VmWhileLoop;
                var
                  i, sum: integer;
                begin
                  i := 1;
                  sum := 0;
                  while i <= 5 do
                  begin
                    sum := sum + i;
                    i := i + 1;
                  end;
                  WriteLn('sum=', sum);
                end.
                """;

        String output = executeWithVm(code);
        System.out.println("[VM][OUTPUT] vmExecutesWhileLoop -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("sum=15"));
    }

    @Test
    void vmExecutesForLoop() {
        String code = """
                program VmForLoop;
                var
                  i, sum: integer;
                begin
                  sum := 0;
                  for i := 5 downto 1 do
                    sum := sum + i;
                  WriteLn('sum=', sum);
                end.
                """;

        String output = executeWithVm(code);
        System.out.println("[VM][OUTPUT] vmExecutesForLoop -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("sum=15"));
    }

    @Test
    void vmExecutesRepeatUntilLoop() {
        String code = """
                program VmRepeatUntil;
                var
                  i: integer;
                begin
                  i := 0;
                  repeat
                    i := i + 1;
                  until i = 4;
                  WriteLn('i=', i);
                end.
                """;

        String output = executeWithVm(code);
        System.out.println("[VM][OUTPUT] vmExecutesRepeatUntilLoop -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("i=4"));
    }

    private static String executeWithVm(String code) {
        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        return new SimpleVmExecutor().execute(optimized);
    }
}
