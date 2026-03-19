package org.nahap.ast;

import java.util.Objects;

import org.nahap.ast.type.TypeNode;
import org.nahap.ast.visitor.AstVisitor;

public final class ParameterNode implements ASTNode {
    private final String name;
    private final TypeNode type;

    public ParameterNode(String name, TypeNode type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitParameter(this);
    }
}
