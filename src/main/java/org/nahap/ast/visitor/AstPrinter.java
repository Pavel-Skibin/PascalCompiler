package org.nahap.ast.visitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

public final class AstPrinter implements AstVisitor<Void> {
    private final StringBuilder out = new StringBuilder();
    private final Deque<NodeContext> nodeStack = new ArrayDeque<>();
    private final List<Boolean> branchStack = new ArrayList<>();

    public String print(ASTNode root) {
        out.setLength(0);
        nodeStack.clear();
        branchStack.clear();
        visitNode(root, true, true);
        return out.toString();
    }

    private void line(String text) {
        NodeContext context = nodeStack.peek();
        if (context == null || context.root()) {
            out.append(text).append(System.lineSeparator());
            return;
        }

        for (boolean hasNextSibling : branchStack) {
            out.append(hasNextSibling ? "|   " : "    ");
        }
        out.append(context.isLast() ? "`-- " : "|-- ")
                .append(text)
                .append(System.lineSeparator());
    }

    private void withChildren(Runnable action) {
        NodeContext context = nodeStack.peek();
        boolean pushed = false;
        if (context != null && !context.root()) {
            branchStack.add(!context.isLast());
            pushed = true;
        }

        action.run();

        if (pushed) {
            branchStack.remove(branchStack.size() - 1);
        }
    }

    private void visitNode(ASTNode node, boolean isLast, boolean root) {
        nodeStack.push(new NodeContext(isLast, root));
        node.accept(this);
        nodeStack.pop();
    }

    private void printLeaf(String text, boolean isLast) {
        nodeStack.push(new NodeContext(isLast, false));
        line(text);
        nodeStack.pop();
    }

    private void printPseudoNode(String text, boolean isLast, Runnable childrenPrinter) {
        nodeStack.push(new NodeContext(isLast, false));
        line(text);
        withChildren(childrenPrinter);
        nodeStack.pop();
    }

    private void printNode(String caption, ASTNode node, boolean isLast) {
        printPseudoNode(caption, isLast, () -> visitNode(node, true, false));
    }

    private void printNodes(String caption, List<? extends ASTNode> nodes, boolean isLast) {
        printPseudoNode(caption, isLast, () -> {
            if (nodes.isEmpty()) {
                printLeaf("(empty)", true);
                return;
            }
            for (int i = 0; i < nodes.size(); i++) {
                visitNode(nodes.get(i), i == nodes.size() - 1, false);
            }
        });
    }

    private void printExpressions(String caption, List<? extends Expression> expressions, boolean isLast) {
        printPseudoNode(caption, isLast, () -> {
            if (expressions.isEmpty()) {
                printLeaf("(empty)", true);
                return;
            }
            for (int i = 0; i < expressions.size(); i++) {
                visitNode(expressions.get(i), i == expressions.size() - 1, false);
            }
        });
    }

    @Override
    public Void visitProgram(ProgramNode node) {
        line("Program: " + node.getName());
        withChildren(() -> visitNode(node.getBlock(), true, false));
        return null;
    }

    @Override
    public Void visitBlock(BlockNode node) {
        line("Block");
        withChildren(() -> {
            printNodes("VariableDeclarations", node.getVariableDeclarations(), false);
            printNodes("SubroutineDeclarations", node.getSubroutineDeclarations(), false);
            printNode("Body", node.getBody(), true);
        });
        return null;
    }

    @Override
    public Void visitParameter(ParameterNode node) {
        line("Parameter: " + node.getName());
        withChildren(() -> visitNode(node.getType(), true, false));
        return null;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclaration node) {
        line("VariableDeclaration: " + String.join(", ", node.getNames()));
        withChildren(() -> visitNode(node.getType(), true, false));
        return null;
    }

    @Override
    public Void visitProcedureDeclaration(ProcedureDeclaration node) {
        line("ProcedureDeclaration: " + node.getName());
        withChildren(() -> {
            printNodes("Parameters", node.getParameters(), false);
            printNode("Block", node.getBlock(), true);
        });
        return null;
    }

    @Override
    public Void visitFunctionDeclaration(FunctionDeclaration node) {
        line("FunctionDeclaration: " + node.getName());
        withChildren(() -> {
            printNodes("Parameters", node.getParameters(), false);
            printNode("ReturnType", node.getReturnType(), false);
            printNode("Block", node.getBlock(), true);
        });
        return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeNode node) {
        line("PrimitiveType: " + node.getName());
        return null;
    }

    @Override
    public Void visitNamedType(NamedTypeNode node) {
        line("NamedType: " + node.getName());
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeNode node) {
        line("ArrayType: [" + node.getLowerBound() + ".." + node.getUpperBound() + "]");
        withChildren(() -> printNode("ElementType", node.getElementType(), true));
        return null;
    }

    @Override
    public Void visitCompoundStatement(CompoundStatement node) {
        line("CompoundStatement");
        withChildren(() -> {
            if (node.getStatements().isEmpty()) {
                printLeaf("(empty)", true);
                return;
            }
            for (int i = 0; i < node.getStatements().size(); i++) {
                Statement statement = node.getStatements().get(i);
                visitNode(statement, i == node.getStatements().size() - 1, false);
            }
        });
        return null;
    }

    @Override
    public Void visitAssignmentStatement(AssignmentStatement node) {
        line("AssignmentStatement");
        withChildren(() -> {
            printNode("Target", node.getTarget(), false);
            printNode("Value", node.getValue(), true);
        });
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement node) {
        line("IfStatement");
        withChildren(() -> {
            boolean hasElse = node.getElseBranch() != null;
            printNode("Condition", node.getCondition(), false);
            printNode("Then", node.getThenBranch(), !hasElse);
            if (node.getElseBranch() != null) {
                printNode("Else", node.getElseBranch(), true);
            }
        });
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement node) {
        line("WhileStatement");
        withChildren(() -> {
            printNode("Condition", node.getCondition(), false);
            printNode("Body", node.getBody(), true);
        });
        return null;
    }

    @Override
    public Void visitRepeatUntilStatement(RepeatUntilStatement node) {
        line("RepeatUntilStatement");
        withChildren(() -> {
            printNodes("Body", node.getBody(), false);
            printNode("Condition", node.getCondition(), true);
        });
        return null;
    }

    @Override
    public Void visitForStatement(ForStatement node) {
        line("ForStatement: " + node.getVariableName() + (node.isDescending() ? " downto" : " to"));
        withChildren(() -> {
            printNode("Start", node.getStartExpression(), false);
            printNode("End", node.getEndExpression(), false);
            printNode("Body", node.getBody(), true);
        });
        return null;
    }

    @Override
    public Void visitBreakStatement(BreakStatement node) {
        line("BreakStatement");
        return null;
    }

    @Override
    public Void visitContinueStatement(ContinueStatement node) {
        line("ContinueStatement");
        return null;
    }

    @Override
    public Void visitProcedureCallStatement(ProcedureCallStatement node) {
        line("ProcedureCallStatement: " + node.getName());
        withChildren(() -> printExpressions("Arguments", node.getArguments(), true));
        return null;
    }

    @Override
    public Void visitWriteStatement(WriteStatement node) {
        line(node.isWriteLine() ? "WriteLnStatement" : "WriteStatement");
        withChildren(() -> printExpressions("Arguments", node.getArguments(), true));
        return null;
    }

    @Override
    public Void visitReadStatement(ReadStatement node) {
        line(node.isReadLine() ? "ReadLnStatement" : "ReadStatement");
        withChildren(() -> printExpressions("Targets", node.getTargets(), true));
        return null;
    }

    @Override
    public Void visitBinaryExpression(BinaryExpression node) {
        line("BinaryExpression: " + node.getOperator());
        withChildren(() -> {
            printNode("Left", node.getLeft(), false);
            printNode("Right", node.getRight(), true);
        });
        return null;
    }

    @Override
    public Void visitUnaryExpression(UnaryExpression node) {
        line("UnaryExpression: " + node.getOperator());
        withChildren(() -> printNode("Expression", node.getExpression(), true));
        return null;
    }

    @Override
    public Void visitLiteralExpression(LiteralExpression node) {
        line("LiteralExpression(" + node.getType() + "): " + node.getText());
        return null;
    }

    @Override
    public Void visitVariableReferenceExpression(VariableReferenceExpression node) {
        line("VariableReference: " + node.getName());
        return null;
    }

    @Override
    public Void visitArrayAccessExpression(ArrayAccessExpression node) {
        line("ArrayAccess: " + node.getArrayName());
        withChildren(() -> printNode("Index", node.getIndex(), true));
        return null;
    }

    @Override
    public Void visitFunctionCallExpression(FunctionCallExpression node) {
        line("FunctionCall: " + node.getName());
        withChildren(() -> printExpressions("Arguments", node.getArguments(), true));
        return null;
    }

    private record NodeContext(boolean isLast, boolean root) {
    }
}
