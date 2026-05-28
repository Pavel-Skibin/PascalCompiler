package org.nahap;

import org.junit.jupiter.api.Test;
import org.nahap.vm.SimpleVirtualMachine;
import org.nahap.vm.SimpleVmInstruction;
import org.nahap.vm.SimpleVmOpCode;
import org.nahap.vm.SimpleVmProgram;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleVmFromFileTest {

    @Test
    void loadsBytecodeFromFileAndExecutes() throws Exception {
        List<SimpleVmProgram.VariableDefinition> vars = List.of(
                new SimpleVmProgram.VariableDefinition("i", "integer"),
                new SimpleVmProgram.VariableDefinition("sum", "integer")
        );

        List<SimpleVmInstruction> instr = parseInstructionsFromResource("/manual_loop.vm");

        SimpleVmProgram program = new SimpleVmProgram("FromFileLoop", vars, instr);
        // print bytecode
        System.out.println("[TEST][BYTECODE] Program: " + program.getName());
        var instructions = program.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            var ins = instructions.get(i);
            Object op = ins.operand();
            String operand = op == null ? "" : op.toString();
            System.out.println(String.format("[TEST][BYTECODE] %04d: %-15s %s", i, ins.opcode(), operand));
        }

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] FromFileLoop -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("sum=15" + System.lineSeparator(), output);
    }

    private static List<SimpleVmInstruction> parseInstructionsFromResource(String resourcePath) throws Exception {
        InputStream is = SimpleVmFromFileTest.class.getResourceAsStream(resourcePath);
        if (is == null) throw new IllegalStateException("Resource not found: " + resourcePath);

        List<SimpleVmInstruction> instructions = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
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
                    // For jump targets VM expects Integer indices
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
