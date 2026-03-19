package org.nahap.ast.expr;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class UnaryExpression extends Expression {
    private final UnaryOperator operator;
    private final Expression expression;

    public UnaryExpression(UnaryOperator operator, Expression expression) {
        this.operator = Objects.requireNonNull(operator, "operator");
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public UnaryOperator getOperator() {
        return operator;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitUnaryExpression(this);
    }
}
