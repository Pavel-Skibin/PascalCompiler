package org.nahap.ast.decl;

import java.util.List;

import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;
import org.nahap.ast.visitor.AstVisitor;

public final class ProcedureDeclaration extends SubroutineDeclaration {
    public ProcedureDeclaration(String name, List<ParameterNode> parameters, BlockNode block) {
        super(name, parameters, block);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitProcedureDeclaration(this);
    }
}
