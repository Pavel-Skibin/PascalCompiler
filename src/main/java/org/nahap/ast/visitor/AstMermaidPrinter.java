package org.nahap.ast.visitor;

import java.util.List;

import org.nahap.ast.ASTNode;
import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.decl.FunctionDeclaration;
import org.nahap.ast.decl.ProcedureDeclaration;
import org.nahap.ast.decl.VariableDeclaration;
import org.nahap.ast.expr.ArrayAccessExpression;
import org.nahap.ast.expr.BinaryExpression;
import org.nahap.ast.expr.Expression;
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
import org.nahap.ast.stmt.Statement;
import org.nahap.ast.stmt.WhileStatement;
import org.nahap.ast.stmt.WriteStatement;
import org.nahap.ast.type.ArrayTypeNode;
import org.nahap.ast.type.NamedTypeNode;
import org.nahap.ast.type.PrimitiveTypeNode;

public final class AstMermaidPrinter implements AstVisitor<String> {
    private final StringBuilder out = new StringBuilder();
    private int nodeCounter;

    public String print(ASTNode root) {
        out.setLength(0);
        nodeCounter = 0;
        out.append("graph LR").append(System.lineSeparator());
        root.accept(this);
        return out.toString();
    }

    private String createNode(String label) {
        String id = "n" + nodeCounter++;
        out.append("  ")
                .append(id)
                .append("[\"")
                .append(escape(label))
                .append("\"]")
                .append(System.lineSeparator());
        return id;
    }

    private String createGroup(String parentId, String label) {
        String groupId = createNode(label);
        link(parentId, groupId, null);
        return groupId;
    }

    private void connect(String parentId, String childLabel, ASTNode childNode) {
        String childId = childNode.accept(this);
        link(parentId, childId, childLabel);
    }

    private void connectExpressions(String parentId, String caption, List<? extends Expression> expressions) {
        String groupId = createGroup(parentId, caption);
        if (expressions.isEmpty()) {
            link(groupId, createNode("(empty)"), null);
            return;
        }
        for (Expression expression : expressions) {
            String expressionId = expression.accept(this);
            link(groupId, expressionId, null);
        }
    }

    private void connectNodes(String parentId, String caption, List<? extends ASTNode> nodes) {
        String groupId = createGroup(parentId, caption);
        if (nodes.isEmpty()) {
            link(groupId, createNode("(empty)"), null);
            return;
        }
        for (ASTNode node : nodes) {
            String childId = node.accept(this);
            link(groupId, childId, null);
        }
    }

    private void link(String fromId, String toId, String edgeLabel) {
        out.append("  ").append(fromId).append(" -->");
        if (edgeLabel != null && !edgeLabel.isBlank()) {
            out.append("|\"").append(escape(edgeLabel)).append("\"|");
        }
        out.append(" ").append(toId).append(System.lineSeparator());
    }

    private String escape(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    @Override
    public String visitProgram(ProgramNode node) {
        String id = createNode("Program: " + node.getName());
        connect(id, "block", node.getBlock());
        return id;
    }

    @Override
    public String visitBlock(BlockNode node) {
        String id = createNode("Block");
        connectNodes(id, "VariableDeclarations", node.getVariableDeclarations());
        connectNodes(id, "SubroutineDeclarations", node.getSubroutineDeclarations());
        connect(id, "Body", node.getBody());
        return id;
    }

    @Override
    public String visitParameter(ParameterNode node) {
        String id = createNode("Parameter: " + node.getName());
        connect(id, "Type", node.getType());
        return id;
    }

    @Override
    public String visitVariableDeclaration(VariableDeclaration node) {
        String id = createNode("VariableDeclaration: " + String.join(", ", node.getNames()));
        connect(id, "Type", node.getType());
        return id;
    }

    @Override
    public String visitProcedureDeclaration(ProcedureDeclaration node) {
        String id = createNode("ProcedureDeclaration: " + node.getName());
        connectNodes(id, "Parameters", node.getParameters());
        connect(id, "Block", node.getBlock());
        return id;
    }

    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node) {
        String id = createNode("FunctionDeclaration: " + node.getName());
        connectNodes(id, "Parameters", node.getParameters());
        connect(id, "ReturnType", node.getReturnType());
        connect(id, "Block", node.getBlock());
        return id;
    }

    @Override
    public String visitPrimitiveType(PrimitiveTypeNode node) {
        return createNode("PrimitiveType: " + node.getName());
    }

    @Override
    public String visitNamedType(NamedTypeNode node) {
        return createNode("NamedType: " + node.getName());
    }

    @Override
    public String visitArrayType(ArrayTypeNode node) {
        String id = createNode("ArrayType: [" + node.getLowerBound() + ".." + node.getUpperBound() + "]");
        connect(id, "ElementType", node.getElementType());
        return id;
    }

    @Override
    public String visitCompoundStatement(CompoundStatement node) {
        String id = createNode("CompoundStatement");
        if (node.getStatements().isEmpty()) {
            link(id, createNode("(empty)"), null);
            return id;
        }
        for (Statement statement : node.getStatements()) {
            String childId = statement.accept(this);
            link(id, childId, null);
        }
        return id;
    }

    @Override
    public String visitAssignmentStatement(AssignmentStatement node) {
        String id = createNode("AssignmentStatement");
        connect(id, "Target", node.getTarget());
        connect(id, "Value", node.getValue());
        return id;
    }

    @Override
    public String visitIfStatement(IfStatement node) {
        String id = createNode("IfStatement");
        connect(id, "Condition", node.getCondition());
        connect(id, "Then", node.getThenBranch());
        if (node.getElseBranch() != null) {
            connect(id, "Else", node.getElseBranch());
        }
        return id;
    }

    @Override
    public String visitWhileStatement(WhileStatement node) {
        String id = createNode("WhileStatement");
        connect(id, "Condition", node.getCondition());
        connect(id, "Body", node.getBody());
        return id;
    }

    @Override
    public String visitRepeatUntilStatement(RepeatUntilStatement node) {
        String id = createNode("RepeatUntilStatement");
        connectNodes(id, "Body", node.getBody());
        connect(id, "Condition", node.getCondition());
        return id;
    }

    @Override
    public String visitForStatement(ForStatement node) {
        String id = createNode("ForStatement: " + node.getVariableName() + (node.isDescending() ? " downto" : " to"));
        connect(id, "Start", node.getStartExpression());
        connect(id, "End", node.getEndExpression());
        connect(id, "Body", node.getBody());
        return id;
    }

    @Override
    public String visitBreakStatement(BreakStatement node) {
        return createNode("BreakStatement");
    }

    @Override
    public String visitContinueStatement(ContinueStatement node) {
        return createNode("ContinueStatement");
    }

    @Override
    public String visitProcedureCallStatement(ProcedureCallStatement node) {
        String id = createNode("ProcedureCallStatement: " + node.getName());
        connectExpressions(id, "Arguments", node.getArguments());
        return id;
    }

    @Override
    public String visitWriteStatement(WriteStatement node) {
        String id = createNode(node.isWriteLine() ? "WriteLnStatement" : "WriteStatement");
        connectExpressions(id, "Arguments", node.getArguments());
        return id;
    }

    @Override
    public String visitReadStatement(ReadStatement node) {
        String id = createNode(node.isReadLine() ? "ReadLnStatement" : "ReadStatement");
        connectExpressions(id, "Targets", node.getTargets());
        return id;
    }

    @Override
    public String visitBinaryExpression(BinaryExpression node) {
        String id = createNode("BinaryExpression: " + node.getOperator());
        connect(id, "Left", node.getLeft());
        connect(id, "Right", node.getRight());
        return id;
    }

    @Override
    public String visitUnaryExpression(UnaryExpression node) {
        String id = createNode("UnaryExpression: " + node.getOperator());
        connect(id, "Expression", node.getExpression());
        return id;
    }

    @Override
    public String visitLiteralExpression(LiteralExpression node) {
        return createNode("LiteralExpression(" + node.getType() + "): " + node.getText());
    }

    @Override
    public String visitVariableReferenceExpression(VariableReferenceExpression node) {
        return createNode("VariableReference: " + node.getName());
    }

    @Override
    public String visitArrayAccessExpression(ArrayAccessExpression node) {
        String id = createNode("ArrayAccess: " + node.getArrayName());
        connect(id, "Index", node.getIndex());
        return id;
    }

    @Override
    public String visitFunctionCallExpression(FunctionCallExpression node) {
        String id = createNode("FunctionCall: " + node.getName());
        connectExpressions(id, "Arguments", node.getArguments());
        return id;
    }
}
