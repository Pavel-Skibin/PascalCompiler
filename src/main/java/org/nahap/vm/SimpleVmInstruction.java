package org.nahap.vm;

public record SimpleVmInstruction(SimpleVmOpCode opcode, Object operand) {
    public SimpleVmInstruction {
        if (opcode == null) {
            throw new IllegalArgumentException("opcode must not be null");
        }
    }

    public static SimpleVmInstruction of(SimpleVmOpCode opcode) {
        return new SimpleVmInstruction(opcode, null);
    }

    public static SimpleVmInstruction of(SimpleVmOpCode opcode, Object operand) {
        return new SimpleVmInstruction(opcode, operand);
    }
}