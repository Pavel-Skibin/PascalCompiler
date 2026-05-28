package org.nahap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nahap.support.CompilerTestUtils;
import org.nahap.support.ConsoleTestWatcher;
import org.nahap.vm.SimpleVmCompiler;
import org.nahap.vm.SimpleVmProgram;
import org.nahap.vm.SimpleVmInstruction;
import org.nahap.vm.SimpleVmExecutor;

import java.util.List;

@ExtendWith(ConsoleTestWatcher.class)
class SimpleVmBytecodePrintTest {

    @Test
    void printsBytecodeAndRunsArithmetic() {
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

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());

        SimpleVmProgram program = new SimpleVmCompiler().compile(optimized);
        printProgramBytecode(program);

        String output = new SimpleVmExecutor().execute(optimized);
        System.out.println("[VM][OUTPUT] printsBytecodeAndRunsArithmetic -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("ok=6 mod=2"));
    }

    @Test
    void printsBytecodeAndRunsWhile() {
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

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());

        SimpleVmProgram program = new SimpleVmCompiler().compile(optimized);
        printProgramBytecode(program);

        String output = new SimpleVmExecutor().execute(optimized);
        System.out.println("[VM][OUTPUT] printsBytecodeAndRunsWhile -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("sum=15"));
    }

    @Test
    void printsBytecodeAndRunsFor() {
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

        var parsed = CompilerTestUtils.parseString(code);
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());

        SimpleVmProgram program = new SimpleVmCompiler().compile(optimized);
        printProgramBytecode(program);

        String output = new SimpleVmExecutor().execute(optimized);
        System.out.println("[VM][OUTPUT] printsBytecodeAndRunsFor -> " + output.replace(System.lineSeparator(), "\\n"));
        assertTrue(output.contains("sum=15"));
    }

    private static void printProgramBytecode(SimpleVmProgram program) {
        System.out.println("[VM][BYTECODE] Program: " + program.getName());
        List<SimpleVmProgram.VariableDefinition> vars = program.getVariables();
        if (!vars.isEmpty()) {
            System.out.println("[VM][BYTECODE] Variables:");
            for (var v : vars) {
                System.out.println("[VM][BYTECODE]  - " + v.name() + " : " + v.typeName());
            }
        }

        System.out.println("[VM][BYTECODE] Instructions:");
        var instructions = program.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            SimpleVmInstruction ins = instructions.get(i);
            Object op = ins.operand();
            String operand = op == null ? "" : op.toString();
            System.out.println(String.format("[VM][BYTECODE] %04d: %-15s %s", i, ins.opcode(), operand));
        }
    }
}
