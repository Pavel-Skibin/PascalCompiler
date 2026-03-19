package org.nahap.ast;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class ProgramNode implements ASTNode {
    private final String name;
    private final BlockNode block;

    public ProgramNode(String name, BlockNode block) {
        this.name = Objects.requireNonNull(name, "name");
        this.block = Objects.requireNonNull(block, "block");
    }

    public String getName() {
        return name;
    }

    public BlockNode getBlock() {
        return block;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitProgram(this);
    }
}
