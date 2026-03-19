package org.nahap.ast.stmt;

import org.nahap.ast.visitor.AstVisitor;

public final class ContinueStatement extends Statement {
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitContinueStatement(this);
    }
}
