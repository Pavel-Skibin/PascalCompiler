package org.nahap;

import org.junit.jupiter.api.Test;
import org.nahap.vm.SimpleVirtualMachine;
import org.nahap.vm.SimpleVmInstruction;
import org.nahap.vm.SimpleVmOpCode;
import org.nahap.vm.SimpleVmProgram;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleVmExtraTests {

    @Test
    void logicalOperationsPrintToConsole() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of();

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, true),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, false),
                SimpleVmInstruction.of(SimpleVmOpCode.AND),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("LogicalOps", vars, instr);
        printProgram(program);

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] LogicalOps -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("false" + System.lineSeparator(), output);
    }

    @Test
    void stringConcatPrintToConsole() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of();

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, "Hello"),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, " "),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, "VM"),
                SimpleVmInstruction.of(SimpleVmOpCode.ADD),
                SimpleVmInstruction.of(SimpleVmOpCode.ADD),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("Concat", vars, instr);
        printProgram(program);

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] Concat -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("Hello VM" + System.lineSeparator(), output);
    }

    @Test
    void divisionAndModPrintToConsole() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of();

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 7L),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 3L),
                SimpleVmInstruction.of(SimpleVmOpCode.DIVIDE_INT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 7L),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 3L),
                SimpleVmInstruction.of(SimpleVmOpCode.MOD),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("DivMod", vars, instr);
        printProgram(program);

        String output = new SimpleVirtualMachine().execute(program);
        System.out.println("[TEST][OUTPUT] DivMod -> " + output.replace(System.lineSeparator(), "\\n"));
        assertEquals("2" + System.lineSeparator() + "1" + System.lineSeparator(), output);
    }

    private static void printProgram(SimpleVmProgram program) {
        System.out.println("[TEST][BYTECODE] Program: " + program.getName());
        var instructions = program.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            var ins = instructions.get(i);
            Object op = ins.operand();
            String operand = op == null ? "" : op.toString();
            System.out.println(String.format("[TEST][BYTECODE] %04d: %-15s %s", i, ins.opcode(), operand));
        }
    }
}
