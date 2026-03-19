package org.nahap.ast.expr;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class LiteralExpression extends Expression {
    private final String text;
    private final LiteralType type;

    public LiteralExpression(String text, LiteralType type) {
        this.text = Objects.requireNonNull(text, "text");
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getText() {
        return text;
    }

    public LiteralType getType() {
        return type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitLiteralExpression(this);
    }
}
