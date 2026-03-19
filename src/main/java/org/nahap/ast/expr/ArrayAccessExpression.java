package org.nahap.ast.expr;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class ArrayAccessExpression extends Expression {
    private final String arrayName;
    private final Expression index;

    public ArrayAccessExpression(String arrayName, Expression index) {
        this.arrayName = Objects.requireNonNull(arrayName, "arrayName");
        this.index = Objects.requireNonNull(index, "index");
    }

    public String getArrayName() {
        return arrayName;
    }

    public Expression getIndex() {
        return index;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitArrayAccessExpression(this);
    }
}
