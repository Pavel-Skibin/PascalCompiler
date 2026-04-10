package org.nahap.ast.expr;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class CastExpression extends Expression {
    private final Expression expression;
    private final String targetType;

    public CastExpression(Expression expression, String targetType) {
        this.expression = Objects.requireNonNull(expression, "expression");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    public Expression getExpression() {
        return expression;
    }

    public String getTargetType() {
        return targetType;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitCastExpression(this);
    }
}