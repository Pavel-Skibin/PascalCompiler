package org.nahap.ast.stmt;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class CompoundStatement extends Statement {
    private final List<Statement> statements;

    public CompoundStatement(List<Statement> statements) {
        this.statements = List.copyOf(Objects.requireNonNull(statements, "statements"));
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitCompoundStatement(this);
    }
}
