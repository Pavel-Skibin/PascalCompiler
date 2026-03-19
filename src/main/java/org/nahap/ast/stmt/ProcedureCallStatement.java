package org.nahap.ast.stmt;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.expr.Expression;
import org.nahap.ast.visitor.AstVisitor;

public final class ProcedureCallStatement extends Statement {
    private final String name;
    private final List<Expression> arguments;

    public ProcedureCallStatement(String name, List<Expression> arguments) {
        this.name = Objects.requireNonNull(name, "name");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public String getName() {
        return name;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitProcedureCallStatement(this);
    }
}
