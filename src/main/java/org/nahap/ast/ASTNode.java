package org.nahap.ast;

import org.nahap.ast.visitor.AstVisitor;

public interface ASTNode {
    <T> T accept(AstVisitor<T> visitor);
}
