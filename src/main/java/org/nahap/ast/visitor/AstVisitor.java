package org.nahap.ast.visitor;

import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.decl.FunctionDeclaration;
import org.nahap.ast.decl.ProcedureDeclaration;
import org.nahap.ast.decl.VariableDeclaration;
import org.nahap.ast.expr.ArrayAccessExpression;
import org.nahap.ast.expr.BinaryExpression;
import org.nahap.ast.expr.FunctionCallExpression;
import org.nahap.ast.expr.LiteralExpression;
import org.nahap.ast.expr.UnaryExpression;
import org.nahap.ast.expr.VariableReferenceExpression;
import org.nahap.ast.stmt.AssignmentStatement;
import org.nahap.ast.stmt.BreakStatement;
import org.nahap.ast.stmt.CompoundStatement;
import org.nahap.ast.stmt.ContinueStatement;
import org.nahap.ast.stmt.ForStatement;
import org.nahap.ast.stmt.IfStatement;
import org.nahap.ast.stmt.ProcedureCallStatement;
import org.nahap.ast.stmt.ReadStatement;
import org.nahap.ast.stmt.RepeatUntilStatement;
import org.nahap.ast.stmt.WhileStatement;
import org.nahap.ast.stmt.WriteStatement;
import org.nahap.ast.type.ArrayTypeNode;
import org.nahap.ast.type.NamedTypeNode;
import org.nahap.ast.type.PrimitiveTypeNode;

public interface AstVisitor<T> {
    T visitProgram(ProgramNode node);

    T visitBlock(BlockNode node);

    T visitParameter(ParameterNode node);

    T visitVariableDeclaration(VariableDeclaration node);

    T visitProcedureDeclaration(ProcedureDeclaration node);

    T visitFunctionDeclaration(FunctionDeclaration node);

    T visitPrimitiveType(PrimitiveTypeNode node);

    T visitNamedType(NamedTypeNode node);

    T visitArrayType(ArrayTypeNode node);

    T visitCompoundStatement(CompoundStatement node);

    T visitAssignmentStatement(AssignmentStatement node);

    T visitIfStatement(IfStatement node);

    T visitWhileStatement(WhileStatement node);

    T visitRepeatUntilStatement(RepeatUntilStatement node);

    T visitForStatement(ForStatement node);

    T visitBreakStatement(BreakStatement node);

    T visitContinueStatement(ContinueStatement node);

    T visitProcedureCallStatement(ProcedureCallStatement node);

    T visitWriteStatement(WriteStatement node);

    T visitReadStatement(ReadStatement node);

    T visitBinaryExpression(BinaryExpression node);

    T visitUnaryExpression(UnaryExpression node);

    T visitLiteralExpression(LiteralExpression node);

    T visitVariableReferenceExpression(VariableReferenceExpression node);

    T visitArrayAccessExpression(ArrayAccessExpression node);

    T visitFunctionCallExpression(FunctionCallExpression node);
}
