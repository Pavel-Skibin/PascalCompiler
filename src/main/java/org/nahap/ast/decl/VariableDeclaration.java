package org.nahap.ast.decl;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.type.TypeNode;
import org.nahap.ast.visitor.AstVisitor;

public final class VariableDeclaration extends Declaration {
    private final List<String> names;
    private final TypeNode type;

    public VariableDeclaration(List<String> names, TypeNode type) {
        this.names = List.copyOf(Objects.requireNonNull(names, "names"));
        this.type = Objects.requireNonNull(type, "type");
    }

    public List<String> getNames() {
        return names;
    }

    public TypeNode getType() {
        return type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVariableDeclaration(this);
    }
}
