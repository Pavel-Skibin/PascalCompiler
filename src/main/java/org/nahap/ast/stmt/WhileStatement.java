package org.nahap.ast.stmt;

import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class WhileStatement extends Statement {
    private final Expression condition;
    private final Statement body;

    public WhileStatement(Expression condition, Statement body) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.body = Objects.requireNonNull(body, "body");
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getBody() {
        return body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitWhileStatement(this);
    }
}
