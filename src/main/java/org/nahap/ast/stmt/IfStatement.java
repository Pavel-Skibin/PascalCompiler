package org.nahap.ast.stmt;

import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class IfStatement extends Statement {
    private final Expression condition;
    private final Statement thenBranch;
    private final Statement elseBranch;

    public IfStatement(Expression condition, Statement thenBranch, Statement elseBranch) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.thenBranch = Objects.requireNonNull(thenBranch, "thenBranch");
        this.elseBranch = elseBranch;
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getThenBranch() {
        return thenBranch;
    }

    public Statement getElseBranch() {
        return elseBranch;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIfStatement(this);
    }
}
