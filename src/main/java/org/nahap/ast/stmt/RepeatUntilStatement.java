package org.nahap.ast.stmt;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class RepeatUntilStatement extends Statement {
    private final List<Statement> body;
    private final Expression condition;

    public RepeatUntilStatement(List<Statement> body, Expression condition) {
        this.body = List.copyOf(Objects.requireNonNull(body, "body"));
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    public List<Statement> getBody() {
        return body;
    }

    public Expression getCondition() {
        return condition;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitRepeatUntilStatement(this);
    }
}
