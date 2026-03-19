package org.nahap.ast;

import java.util.List;
import java.util.Objects;

import org.nahap.ast.decl.SubroutineDeclaration;
import org.nahap.ast.decl.VariableDeclaration;
import org.nahap.ast.stmt.CompoundStatement;
import org.nahap.ast.visitor.AstVisitor;

public final class BlockNode implements ASTNode {
    private final List<VariableDeclaration> variableDeclarations;
    private final List<SubroutineDeclaration> subroutineDeclarations;
    private final CompoundStatement body;

    public BlockNode(
            List<VariableDeclaration> variableDeclarations,
            List<SubroutineDeclaration> subroutineDeclarations,
            CompoundStatement body) {
        this.variableDeclarations = List.copyOf(Objects.requireNonNull(variableDeclarations, "variableDeclarations"));
        this.subroutineDeclarations = List.copyOf(Objects.requireNonNull(subroutineDeclarations, "subroutineDeclarations"));
        this.body = Objects.requireNonNull(body, "body");
    }

    public List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    public List<SubroutineDeclaration> getSubroutineDeclarations() {
        return subroutineDeclarations;
    }

    public CompoundStatement getBody() {
        return body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
