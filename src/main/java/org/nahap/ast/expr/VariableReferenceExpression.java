package org.nahap.ast.expr;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class VariableReferenceExpression extends Expression {
    private final String name;

    public VariableReferenceExpression(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVariableReferenceExpression(this);
    }
}
