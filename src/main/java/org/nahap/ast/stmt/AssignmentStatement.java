package org.nahap.ast.stmt;

import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class AssignmentStatement extends Statement {
    private final Expression target;
    private final Expression value;

    public AssignmentStatement(Expression target, Expression value) {
        this.target = Objects.requireNonNull(target, "target");
        this.value = Objects.requireNonNull(value, "value");
    }

    public Expression getTarget() {
        return target;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitAssignmentStatement(this);
    }
}
