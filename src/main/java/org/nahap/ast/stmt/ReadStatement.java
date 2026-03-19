package org.nahap.ast.stmt;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class ReadStatement extends Statement {
    private final boolean readLine;
    private final List<Expression> targets;

    public ReadStatement(boolean readLine, List<Expression> targets) {
        this.readLine = readLine;
        this.targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
    }

    public boolean isReadLine() {
        return readLine;
    }

    public List<Expression> getTargets() {
        return targets;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitReadStatement(this);
    }
}
