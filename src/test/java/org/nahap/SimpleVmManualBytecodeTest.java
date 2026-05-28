package org.nahap;

import org.junit.jupiter.api.Test;
import org.nahap.vm.SimpleVirtualMachine;
import org.nahap.vm.SimpleVmInstruction;
import org.nahap.vm.SimpleVmOpCode;
import org.nahap.vm.SimpleVmProgram;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleVmManualBytecodeTest {

    @Test
    void manualBytecodePrintedAsStringAndExecutes() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of(
                new SimpleVmProgram.VariableDefinition("i", "integer"),
                new SimpleVmProgram.VariableDefinition("sum", "integer")
        );

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 1L),
                SimpleVmInstruction.of(SimpleVmOpCode.STORE_VAR, "i"),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 0L),
                SimpleVmInstruction.of(SimpleVmOpCode.STORE_VAR, "sum"),
                SimpleVmInstruction.of(SimpleVmOpCode.LOAD_VAR, "i"),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 5L),
                SimpleVmInstruction.of(SimpleVmOpCode.LESS_OR_EQUAL),
                SimpleVmInstruction.of(SimpleVmOpCode.JUMP_IF_FALSE, 17),
                SimpleVmInstruction.of(SimpleVmOpCode.LOAD_VAR, "sum"),
                SimpleVmInstruction.of(SimpleVmOpCode.LOAD_VAR, "i"),
                SimpleVmInstruction.of(SimpleVmOpCode.ADD),
                SimpleVmInstruction.of(SimpleVmOpCode.STORE_VAR, "sum"),
                SimpleVmInstruction.of(SimpleVmOpCode.LOAD_VAR, "i"),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 1L),
                SimpleVmInstruction.of(SimpleVmOpCode.ADD),
                SimpleVmInstruction.of(SimpleVmOpCode.STORE_VAR, "i"),
                SimpleVmInstruction.of(SimpleVmOpCode.JUMP, 4),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, "sum="),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.LOAD_VAR, "sum"),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("ManualLoop", vars, instr);

        String printed = formatBytecode(program);

        String expected = String.join(System.lineSeparator(), List.of(
                "[VM][BYTECODE] 0000: PUSH_CONST      1",
                "[VM][BYTECODE] 0001: STORE_VAR       i",
                "[VM][BYTECODE] 0002: PUSH_CONST      0",
                "[VM][BYTECODE] 0003: STORE_VAR       sum",
                "[VM][BYTECODE] 0004: LOAD_VAR        i",
                "[VM][BYTECODE] 0005: PUSH_CONST      5",
                "[VM][BYTECODE] 0006: LESS_OR_EQUAL   ",
                "[VM][BYTECODE] 0007: JUMP_IF_FALSE   17",
                "[VM][BYTECODE] 0008: LOAD_VAR        sum",
                "[VM][BYTECODE] 0009: LOAD_VAR        i",
                "[VM][BYTECODE] 0010: ADD             ",
                "[VM][BYTECODE] 0011: STORE_VAR       sum",
                "[VM][BYTECODE] 0012: LOAD_VAR        i",
                "[VM][BYTECODE] 0013: PUSH_CONST      1",
                "[VM][BYTECODE] 0014: ADD             ",
                "[VM][BYTECODE] 0015: STORE_VAR       i",
                "[VM][BYTECODE] 0016: JUMP            4",
                "[VM][BYTECODE] 0017: PUSH_CONST      sum=",
                "[VM][BYTECODE] 0018: PRINT           ",
                "[VM][BYTECODE] 0019: LOAD_VAR        sum",
                "[VM][BYTECODE] 0020: PRINT           ",
                "[VM][BYTECODE] 0021: PRINT_LINE      "
        )) + System.lineSeparator();

        assertEquals(expected, printed);

        String output = new SimpleVirtualMachine().execute(program);
        assertEquals("sum=15" + System.lineSeparator(), output);
    }

    private static String formatBytecode(SimpleVmProgram program) {
        StringBuilder sb = new StringBuilder();
        var instructions = program.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            var ins = instructions.get(i);
            Object op = ins.operand();
            String operand = op == null ? "" : op.toString();
            sb.append(String.format("[VM][BYTECODE] %04d: %-15s %s", i, ins.opcode(), operand));
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
