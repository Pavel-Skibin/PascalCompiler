package org.nahap.ast.stmt;

import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class ForStatement extends Statement {
    private final String variableName;
    private final Expression startExpression;
    private final Expression endExpression;
    private final boolean descending;
    private final Statement body;

    public ForStatement(
            String variableName,
            Expression startExpression,
            Expression endExpression,
            boolean descending,
            Statement body) {
        this.variableName = Objects.requireNonNull(variableName, "variableName");
        this.startExpression = Objects.requireNonNull(startExpression, "startExpression");
        this.endExpression = Objects.requireNonNull(endExpression, "endExpression");
        this.descending = descending;
        this.body = Objects.requireNonNull(body, "body");
    }

    public String getVariableName() {
        return variableName;
    }

    public Expression getStartExpression() {
        return startExpression;
    }

    public Expression getEndExpression() {
        return endExpression;
    }

    public boolean isDescending() {
        return descending;
    }

    public Statement getBody() {
        return body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitForStatement(this);
    }
}
