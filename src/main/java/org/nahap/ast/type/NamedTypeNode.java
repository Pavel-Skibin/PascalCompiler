package org.nahap.ast.type;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class NamedTypeNode extends TypeNode {
    private final String name;

    public NamedTypeNode(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitNamedType(this);
    }
}
