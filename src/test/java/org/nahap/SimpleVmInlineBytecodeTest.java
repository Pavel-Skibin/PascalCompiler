package org.nahap;

import org.junit.jupiter.api.Test;
import org.nahap.vm.SimpleVirtualMachine;
import org.nahap.vm.SimpleVmInstruction;
import org.nahap.vm.SimpleVmOpCode;
import org.nahap.vm.SimpleVmProgram;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleVmInlineBytecodeTest {

    @Test
    void executesInlineBytecodeLoop() throws Exception {
        String code = """
                PUSH_CONST 1
                STORE_VAR i
                PUSH_CONST 0
                STORE_VAR sum
                LOAD_VAR i
                PUSH_CONST 5
                LESS_OR_EQUAL
                JUMP_IF_FALSE 17
                LOAD_VAR sum
                LOAD_VAR i
                ADD
                STORE_VAR sum
                LOAD_VAR i
                PUSH_CONST 1
                ADD
                STORE_VAR i
                JUMP 4
                PUSH_CONST sum=
                PRINT
                LOAD_VAR sum
                PRINT
                PRINT_LINE
                """;

        List<SimpleVmProgram.VariableDefinition> vars = List.of(
                new SimpleVmProgram.VariableDefinition("i", "integer"),
                new SimpleVmProgram.VariableDefinition("sum", "integer")
        );

        List<SimpleVmInstruction> instr = parseInstructionsFromString(code);

        SimpleVmProgram program = new SimpleVmProgram("InlineLoop", vars, instr);
        printProgramBytecode(program);

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] InlineLoop -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("sum=15" + System.lineSeparator(), output);
    }

    private static void printProgramBytecode(SimpleVmProgram program) {
        System.out.println("[TEST][BYTECODE] Program: " + program.getName());
        var instructions = program.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            var ins = instructions.get(i);
            Object op = ins.operand();
            String operand = op == null ? "" : op.toString();
            System.out.println(String.format("[TEST][BYTECODE] %04d: %-15s %s", i, ins.opcode(), operand));
        }
    }

    @Test
    void executesInlineStringConcat() throws Exception {
        String code = """
                PUSH_CONST Hello
                PUSH_CONST VM
                ADD
                PRINT
                PRINT_LINE
                """;

        List<SimpleVmProgram.VariableDefinition> vars = List.of();
        List<SimpleVmInstruction> instr = parseInstructionsFromString(code);
        SimpleVmProgram program = new SimpleVmProgram("InlineConcat", vars, instr);
        printProgramBytecode(program);

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] InlineConcat -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("HelloVM" + System.lineSeparator(), output);
    }

    @Test
    void executesInlineLogicComparison() throws Exception {
        String code = """
                PUSH_CONST 2
                PUSH_CONST 3
                LESS_THAN
                PRINT
                PRINT_LINE
                """;

        List<SimpleVmProgram.VariableDefinition> vars = List.of();
        List<SimpleVmInstruction> instr = parseInstructionsFromString(code);
        SimpleVmProgram program = new SimpleVmProgram("InlineLogic", vars, instr);
        printProgramBytecode(program);

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] InlineLogic -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("true" + System.lineSeparator(), output);
    }

    @Test
    void executesInlineDivideAndMod() throws Exception {
        String codeDiv = """
                PUSH_CONST 10
                PUSH_CONST 3
                DIVIDE_INT
                PRINT
                PRINT_LINE
                """;

        String codeMod = """
                PUSH_CONST 10
                PUSH_CONST 3
                MOD
                PRINT
                PRINT_LINE
                """;

        List<SimpleVmProgram.VariableDefinition> vars = List.of();

        var instrDiv = parseInstructionsFromString(codeDiv);
        var programDiv = new SimpleVmProgram("InlineDiv", vars, instrDiv);
        printProgramBytecode(programDiv);
        String outDiv = new SimpleVirtualMachine().execute(programDiv);
        System.out.println("[TEST][OUTPUT] InlineDiv -> " + outDiv.replace(System.lineSeparator(), "\\n"));
        assertEquals("3" + System.lineSeparator(), outDiv);

        var instrMod = parseInstructionsFromString(codeMod);
        var programMod = new SimpleVmProgram("InlineMod", vars, instrMod);
        printProgramBytecode(programMod);
        String outMod = new SimpleVirtualMachine().execute(programMod);
        System.out.println("[TEST][OUTPUT] InlineMod -> " + outMod.replace(System.lineSeparator(), "\\n"));
        assertEquals("1" + System.lineSeparator(), outMod);
    }

    private static List<SimpleVmInstruction> parseInstructionsFromString(String code) throws Exception {
        List<SimpleVmInstruction> instructions = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new StringReader(code))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+", 2);
                String op = parts[0];
                String operandText = parts.length > 1 ? parts[1] : null;

                SimpleVmOpCode opcode = SimpleVmOpCode.valueOf(op);

                Object operand = null;
                if (operandText != null) {
                    if (opcode == SimpleVmOpCode.JUMP || opcode == SimpleVmOpCode.JUMP_IF_FALSE) {
                        if (operandText.matches("-?\\d+")) {
                            operand = Integer.parseInt(operandText);
                        } else {
                            throw new IllegalArgumentException("Invalid jump target: " + operandText);
                        }
                    } else if (operandText.matches("-?\\d+")) {
                        operand = Long.parseLong(operandText);
                    } else if (operandText.matches("-?\\d+\\.\\d+")) {
                        operand = Double.parseDouble(operandText);
                    } else {
                        operand = operandText;
                    }
                }

                if (operand == null) {
                    instructions.add(SimpleVmInstruction.of(opcode));
                } else {
                    instructions.add(SimpleVmInstruction.of(opcode, operand));
                }
            }
        }
        return List.copyOf(instructions);
    }
}
