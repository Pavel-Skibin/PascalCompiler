package org.nahap.vm;

import java.util.List;
import java.util.Objects;

public final class SimpleVmProgram {
    private final String name;
    private final List<VariableDefinition> variables;
    private final List<SimpleVmInstruction> instructions;

    public SimpleVmProgram(String name, List<VariableDefinition> variables, List<SimpleVmInstruction> instructions) {
        this.name = Objects.requireNonNull(name, "name");
        this.variables = List.copyOf(Objects.requireNonNull(variables, "variables"));
        this.instructions = List.copyOf(Objects.requireNonNull(instructions, "instructions"));
    }

    public String getName() {
        return name;
    }

    public List<VariableDefinition> getVariables() {
        return variables;
    }

    public List<SimpleVmInstruction> getInstructions() {
        return instructions;
    }

    public record VariableDefinition(String name, String typeName) {
        public VariableDefinition {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            if (typeName == null || typeName.isBlank()) {
                throw new IllegalArgumentException("typeName must not be blank");
            }
        }
    }
}