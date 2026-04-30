package org.nahap.ast.expr;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class SystemFunctionCallExpression extends Expression {
    public enum SystemFunction {
        INC,
        DEC,
        ABS
    }

    private final SystemFunction function;
    private final Expression argument;

    public SystemFunctionCallExpression(SystemFunction function, Expression argument) {
        this.function = Objects.requireNonNull(function, "function");
        this.argument = Objects.requireNonNull(argument, "argument");
    }

    public SystemFunction getFunction() {
        return function;
    }

    public Expression getArgument() {
        return argument;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitSystemFunctionCallExpression(this);
    }
}
