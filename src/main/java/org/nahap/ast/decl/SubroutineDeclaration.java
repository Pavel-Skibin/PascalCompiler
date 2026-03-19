package org.nahap.ast.decl;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;

public abstract class SubroutineDeclaration extends Declaration {
    private final String name;
    private final List<ParameterNode> parameters;
    private final BlockNode block;

    protected SubroutineDeclaration(String name, List<ParameterNode> parameters, BlockNode block) {
        this.name = Objects.requireNonNull(name, "name");
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        this.block = Objects.requireNonNull(block, "block");
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public BlockNode getBlock() {
        return block;
    }
}
