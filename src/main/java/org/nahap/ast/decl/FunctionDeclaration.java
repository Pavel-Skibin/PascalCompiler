package org.nahap.ast.decl;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;
import org.nahap.ast.type.TypeNode;
import org.nahap.ast.visitor.AstVisitor;

public final class FunctionDeclaration extends SubroutineDeclaration {
    private final TypeNode returnType;

    public FunctionDeclaration(String name, List<ParameterNode> parameters, TypeNode returnType, BlockNode block) {
        super(name, parameters, block);
        this.returnType = Objects.requireNonNull(returnType, "returnType");
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitFunctionDeclaration(this);
    }
}
