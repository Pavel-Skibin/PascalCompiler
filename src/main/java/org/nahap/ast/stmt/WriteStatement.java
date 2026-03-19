package org.nahap.ast.stmt;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class WriteStatement extends Statement {
    private final boolean writeLine;
    private final List<Expression> arguments;

    public WriteStatement(boolean writeLine, List<Expression> arguments) {
        this.writeLine = writeLine;
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public boolean isWriteLine() {
        return writeLine;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitWriteStatement(this);
    }
}
