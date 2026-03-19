package org.nahap.ast.type;

import java.util.Objects;

import org.nahap.ast.visitor.AstVisitor;

public final class ArrayTypeNode extends TypeNode {
    private final int lowerBound;
    private final int upperBound;
    private final TypeNode elementType;

    public ArrayTypeNode(int lowerBound, int upperBound, TypeNode elementType) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.elementType = Objects.requireNonNull(elementType, "elementType");
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public TypeNode getElementType() {
        return elementType;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitArrayType(this);
    }
}
