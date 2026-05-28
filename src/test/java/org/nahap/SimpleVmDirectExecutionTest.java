package org.nahap;

import org.junit.jupiter.api.Test;
import org.nahap.vm.SimpleVirtualMachine;
import org.nahap.vm.SimpleVmInstruction;
import org.nahap.vm.SimpleVmOpCode;
import org.nahap.vm.SimpleVmProgram;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleVmDirectExecutionTest {

    @Test
    void executesDirectStringConcatenationAndPrint() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of();

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, "Hello "),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, "world"),
                SimpleVmInstruction.of(SimpleVmOpCode.ADD),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("DirectString", vars, instr);
        String output = new SimpleVirtualMachine().execute(program);

        assertEquals("Hello world" + System.lineSeparator(), output);
    }

    @Test
    void executesDirectArithmeticAndPrint() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of();

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 10L),
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 5L),
                SimpleVmInstruction.of(SimpleVmOpCode.ADD),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("DirectArithmetic", vars, instr);
        String output = new SimpleVirtualMachine().execute(program);

        assertEquals("15" + System.lineSeparator(), output);
    }

    @Test
    void executesVariableStoreLoad() {
        List<SimpleVmProgram.VariableDefinition> vars = List.of(
                new SimpleVmProgram.VariableDefinition("x", "integer")
        );

        List<SimpleVmInstruction> instr = List.of(
                SimpleVmInstruction.of(SimpleVmOpCode.PUSH_CONST, 42L),
                SimpleVmInstruction.of(SimpleVmOpCode.STORE_VAR, "x"),
                SimpleVmInstruction.of(SimpleVmOpCode.LOAD_VAR, "x"),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT),
                SimpleVmInstruction.of(SimpleVmOpCode.PRINT_LINE)
        );

        SimpleVmProgram program = new SimpleVmProgram("DirectVar", vars, instr);
        String output = new SimpleVirtualMachine().execute(program);

        assertEquals("42" + System.lineSeparator(), output);
    }
}
