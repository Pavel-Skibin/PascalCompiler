package org.nahap.ast.expr;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class FunctionCallExpression extends Expression {
    private final String name;
    private final List<Expression> arguments;

    public FunctionCallExpression(String name, List<Expression> arguments) {
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
        return visitor.visitFunctionCallExpression(this);
    }
}
