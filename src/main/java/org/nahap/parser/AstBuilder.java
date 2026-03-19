package org.nahap.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nahap.ast.ASTNode;
import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.decl.FunctionDeclaration;
import org.nahap.ast.decl.ProcedureDeclaration;
import org.nahap.ast.decl.SubroutineDeclaration;
import org.nahap.ast.decl.VariableDeclaration;
import org.nahap.ast.expr.ArrayAccessExpression;
import org.nahap.ast.expr.BinaryExpression;
import org.nahap.ast.expr.BinaryOperator;
import org.nahap.ast.expr.Expression;
import org.nahap.ast.expr.FunctionCallExpression;
import org.nahap.ast.expr.LiteralExpression;
import org.nahap.ast.expr.LiteralType;
import org.nahap.ast.expr.UnaryExpression;
import org.nahap.ast.expr.UnaryOperator;
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
import org.nahap.ast.type.TypeNode;

public final class AstBuilder extends PascalBaseVisitor<ASTNode> {
    public ProgramNode build(PascalParser.ProgramContext context) {
        return (ProgramNode) visit(context);
    }

    @Override
    public ASTNode visitProgram(PascalParser.ProgramContext ctx) {
        String name = ctx.identifier().getText();
        BlockNode block = asBlock(visit(ctx.block()));
        return new ProgramNode(name, block);
    }

    @Override
    public ASTNode visitBlock(PascalParser.BlockContext ctx) {
        List<VariableDeclaration> variableDeclarations = new ArrayList<>();
        List<SubroutineDeclaration> subroutineDeclarations = new ArrayList<>();

        PascalParser.DeclarationPartContext declarationPart = ctx.declarationPart();
        if (declarationPart != null && declarationPart.varSection() != null) {
            for (PascalParser.VariableDeclarationContext variableDeclarationContext
                    : declarationPart.varSection().variableDeclaration()) {
                variableDeclarations.add((VariableDeclaration) visit(variableDeclarationContext));
            }
        }

        if (declarationPart != null) {
            for (PascalParser.SubroutineDeclarationContext subroutineContext : declarationPart.subroutineDeclaration()) {
                subroutineDeclarations.add((SubroutineDeclaration) visit(subroutineContext));
            }
        }

        CompoundStatement body = (CompoundStatement) visit(ctx.compoundStatement());
        return new BlockNode(variableDeclarations, subroutineDeclarations, body);
    }

    @Override
    public ASTNode visitVariableDeclaration(PascalParser.VariableDeclarationContext ctx) {
        List<String> names = namesFromIdentifierList(ctx.identifierList());
        TypeNode type = asType(visit(ctx.typeSpec()));
        return new VariableDeclaration(names, type);
    }

    @Override
    public ASTNode visitSubroutineDeclaration(PascalParser.SubroutineDeclarationContext ctx) {
        if (ctx.procedureDeclaration() != null) {
            return visit(ctx.procedureDeclaration());
        }
        return visit(ctx.functionDeclaration());
    }

    @Override
    public ASTNode visitProcedureDeclaration(PascalParser.ProcedureDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        List<ParameterNode> parameters = collectParameters(ctx.formalParameters());
        BlockNode block = asBlock(visit(ctx.block()));
        return new ProcedureDeclaration(name, parameters, block);
    }

    @Override
    public ASTNode visitFunctionDeclaration(PascalParser.FunctionDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        List<ParameterNode> parameters = collectParameters(ctx.formalParameters());
        TypeNode returnType = asType(visit(ctx.typeSpec()));
        BlockNode block = asBlock(visit(ctx.block()));
        return new FunctionDeclaration(name, parameters, returnType, block);
    }

    @Override
    public ASTNode visitTypeSpec(PascalParser.TypeSpecContext ctx) {
        if (ctx.primitiveType() != null) {
            return visit(ctx.primitiveType());
        }
        if (ctx.arrayType() != null) {
            return visit(ctx.arrayType());
        }
        return new NamedTypeNode(ctx.identifier().getText());
    }

    @Override
    public ASTNode visitPrimitiveType(PascalParser.PrimitiveTypeContext ctx) {
        return new PrimitiveTypeNode(ctx.getText().toLowerCase(Locale.ROOT));
    }

    @Override
    public ASTNode visitArrayType(PascalParser.ArrayTypeContext ctx) {
        int lower = Integer.parseInt(ctx.INTEGER_LITERAL(0).getText());
        int upper = Integer.parseInt(ctx.INTEGER_LITERAL(1).getText());
        TypeNode elementType = asType(visit(ctx.typeSpec()));
        return new ArrayTypeNode(lower, upper, elementType);
    }

    @Override
    public ASTNode visitCompoundStatement(PascalParser.CompoundStatementContext ctx) {
        List<Statement> statements = new ArrayList<>();
        if (ctx.statementSequence() != null) {
            for (PascalParser.StatementContext statementContext : ctx.statementSequence().statement()) {
                statements.add(asStatement(visit(statementContext)));
            }
        }
        return new CompoundStatement(statements);
    }

    @Override
    public ASTNode visitStatement(PascalParser.StatementContext ctx) {
        if (ctx.compoundStatement() != null) {
            return visit(ctx.compoundStatement());
        }
        if (ctx.assignmentStatement() != null) {
            return visit(ctx.assignmentStatement());
        }
        if (ctx.ifStatement() != null) {
            return visit(ctx.ifStatement());
        }
        if (ctx.whileStatement() != null) {
            return visit(ctx.whileStatement());
        }
        if (ctx.repeatStatement() != null) {
            return visit(ctx.repeatStatement());
        }
        if (ctx.forStatement() != null) {
            return visit(ctx.forStatement());
        }
        if (ctx.breakStatement() != null) {
            return visit(ctx.breakStatement());
        }
        if (ctx.continueStatement() != null) {
            return visit(ctx.continueStatement());
        }
        if (ctx.procedureCallStatement() != null) {
            return visit(ctx.procedureCallStatement());
        }
        if (ctx.ioStatement() != null) {
            return visit(ctx.ioStatement());
        }
        throw new IllegalStateException("Unsupported statement: " + ctx.getText());
    }

    @Override
    public ASTNode visitAssignmentStatement(PascalParser.AssignmentStatementContext ctx) {
        Expression target = asExpression(visit(ctx.variable()));
        Expression value = asExpression(visit(ctx.expression()));
        return new AssignmentStatement(target, value);
    }

    @Override
    public ASTNode visitIfStatement(PascalParser.IfStatementContext ctx) {
        Expression condition = asExpression(visit(ctx.expression()));
        Statement thenBranch = asStatement(visit(ctx.statement(0)));
        Statement elseBranch = ctx.statement().size() > 1 ? asStatement(visit(ctx.statement(1))) : null;
        return new IfStatement(condition, thenBranch, elseBranch);
    }

    @Override
    public ASTNode visitWhileStatement(PascalParser.WhileStatementContext ctx) {
        Expression condition = asExpression(visit(ctx.expression()));
        Statement body = asStatement(visit(ctx.statement()));
        return new WhileStatement(condition, body);
    }

    @Override
    public ASTNode visitRepeatStatement(PascalParser.RepeatStatementContext ctx) {
        List<Statement> body = new ArrayList<>();
        for (PascalParser.StatementContext statementContext : ctx.statementSequence().statement()) {
            body.add(asStatement(visit(statementContext)));
        }
        Expression condition = asExpression(visit(ctx.expression()));
        return new RepeatUntilStatement(body, condition);
    }

    @Override
    public ASTNode visitForStatement(PascalParser.ForStatementContext ctx) {
        String variableName = ctx.identifier().getText();
        Expression startExpression = asExpression(visit(ctx.expression(0)));
        Expression endExpression = asExpression(visit(ctx.expression(1)));
        boolean descending = ctx.DOWNTO() != null;
        Statement body = asStatement(visit(ctx.statement()));
        return new ForStatement(variableName, startExpression, endExpression, descending, body);
    }

    @Override
    public ASTNode visitBreakStatement(PascalParser.BreakStatementContext ctx) {
        return new BreakStatement();
    }

    @Override
    public ASTNode visitContinueStatement(PascalParser.ContinueStatementContext ctx) {
        return new ContinueStatement();
    }

    @Override
    public ASTNode visitProcedureCallStatement(PascalParser.ProcedureCallStatementContext ctx) {
        String name = ctx.identifier().getText();
        List<Expression> arguments = ctx.argumentList() == null
                ? List.of()
                : collectExpressionList(ctx.argumentList().expression());
        return new ProcedureCallStatement(name, arguments);
    }

    @Override
    public ASTNode visitIoStatement(PascalParser.IoStatementContext ctx) {
        if (ctx.WRITE() != null || ctx.WRITELN() != null) {
            List<Expression> arguments = ctx.argumentList() == null
                    ? List.of()
                    : collectExpressionList(ctx.argumentList().expression());
            return new WriteStatement(ctx.WRITELN() != null, arguments);
        }

        List<Expression> targets = new ArrayList<>();
        if (ctx.variableList() != null) {
            for (PascalParser.VariableContext variableContext : ctx.variableList().variable()) {
                targets.add(asExpression(visit(variableContext)));
            }
        }
        return new ReadStatement(ctx.READLN() != null, targets);
    }

    @Override
    public ASTNode visitExpression(PascalParser.ExpressionContext ctx) {
        return visit(ctx.orExpression());
    }

    @Override
    public ASTNode visitOrExpression(PascalParser.OrExpressionContext ctx) {
        Expression current = asExpression(visit(ctx.andExpression(0)));
        for (int i = 1; i < ctx.andExpression().size(); i++) {
            Expression right = asExpression(visit(ctx.andExpression(i)));
            current = new BinaryExpression(current, BinaryOperator.OR, right);
        }
        return current;
    }

    @Override
    public ASTNode visitAndExpression(PascalParser.AndExpressionContext ctx) {
        Expression current = asExpression(visit(ctx.equalityExpression(0)));
        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            Expression right = asExpression(visit(ctx.equalityExpression(i)));
            current = new BinaryExpression(current, BinaryOperator.AND, right);
        }
        return current;
    }

    @Override
    public ASTNode visitEqualityExpression(PascalParser.EqualityExpressionContext ctx) {
        Expression current = asExpression(visit(ctx.relationalExpression(0)));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            Expression right = asExpression(visit(ctx.relationalExpression(i)));
            String opText = ctx.getChild(2 * i - 1).getText().toLowerCase(Locale.ROOT);
            BinaryOperator operator = switch (opText) {
                case "=" -> BinaryOperator.EQUAL;
                case "<>" -> BinaryOperator.NOT_EQUAL;
                default -> throw new IllegalStateException("Unsupported equality operator: " + opText);
            };
            current = new BinaryExpression(current, operator, right);
        }
        return current;
    }

    @Override
    public ASTNode visitRelationalExpression(PascalParser.RelationalExpressionContext ctx) {
        Expression current = asExpression(visit(ctx.additiveExpression(0)));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            Expression right = asExpression(visit(ctx.additiveExpression(i)));
            String opText = ctx.getChild(2 * i - 1).getText().toLowerCase(Locale.ROOT);
            BinaryOperator operator = switch (opText) {
                case "<" -> BinaryOperator.LESS_THAN;
                case "<=" -> BinaryOperator.LESS_OR_EQUAL;
                case ">" -> BinaryOperator.GREATER_THAN;
                case ">=" -> BinaryOperator.GREATER_OR_EQUAL;
                default -> throw new IllegalStateException("Unsupported relational operator: " + opText);
            };
            current = new BinaryExpression(current, operator, right);
        }
        return current;
    }

    @Override
    public ASTNode visitAdditiveExpression(PascalParser.AdditiveExpressionContext ctx) {
        Expression current = asExpression(visit(ctx.multiplicativeExpression(0)));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            Expression right = asExpression(visit(ctx.multiplicativeExpression(i)));
            String opText = ctx.getChild(2 * i - 1).getText();
            BinaryOperator operator = switch (opText) {
                case "+" -> BinaryOperator.ADD;
                case "-" -> BinaryOperator.SUBTRACT;
                default -> throw new IllegalStateException("Unsupported additive operator: " + opText);
            };
            current = new BinaryExpression(current, operator, right);
        }
        return current;
    }

    @Override
    public ASTNode visitMultiplicativeExpression(PascalParser.MultiplicativeExpressionContext ctx) {
        Expression current = asExpression(visit(ctx.unaryExpression(0)));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            Expression right = asExpression(visit(ctx.unaryExpression(i)));
            String opText = ctx.getChild(2 * i - 1).getText().toLowerCase(Locale.ROOT);
            BinaryOperator operator = switch (opText) {
                case "*" -> BinaryOperator.MULTIPLY;
                case "/" -> BinaryOperator.DIVIDE_REAL;
                case "div" -> BinaryOperator.DIVIDE_INT;
                case "mod" -> BinaryOperator.MOD;
                default -> throw new IllegalStateException("Unsupported multiplicative operator: " + opText);
            };
            current = new BinaryExpression(current, operator, right);
        }
        return current;
    }

    @Override
    public ASTNode visitUnaryExpression(PascalParser.UnaryExpressionContext ctx) {
        if (ctx.primary() != null) {
            return visit(ctx.primary());
        }

        Expression expression = asExpression(visit(ctx.unaryExpression()));
        String opText = ctx.getChild(0).getText().toLowerCase(Locale.ROOT);
        UnaryOperator operator = switch (opText) {
            case "+" -> UnaryOperator.PLUS;
            case "-" -> UnaryOperator.MINUS;
            case "not" -> UnaryOperator.NOT;
            default -> throw new IllegalStateException("Unsupported unary operator: " + opText);
        };
        return new UnaryExpression(operator, expression);
    }

    @Override
    public ASTNode visitPrimary(PascalParser.PrimaryContext ctx) {
        if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        }
        if (ctx.variable() != null) {
            return visit(ctx.variable());
        }
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        return visit(ctx.expression());
    }

    @Override
    public ASTNode visitFunctionCall(PascalParser.FunctionCallContext ctx) {
        String name = ctx.identifier().getText();
        List<Expression> arguments = ctx.argumentList() == null
                ? List.of()
                : collectExpressionList(ctx.argumentList().expression());
        return new FunctionCallExpression(name, arguments);
    }

    @Override
    public ASTNode visitVariable(PascalParser.VariableContext ctx) {
        String name = ctx.identifier().getText();
        if (ctx.expression() == null) {
            return new VariableReferenceExpression(name);
        }
        Expression index = asExpression(visit(ctx.expression()));
        return new ArrayAccessExpression(name, index);
    }

    @Override
    public ASTNode visitLiteral(PascalParser.LiteralContext ctx) {
        if (ctx.INTEGER_LITERAL() != null) {
            return new LiteralExpression(ctx.INTEGER_LITERAL().getText(), LiteralType.INTEGER);
        }
        if (ctx.REAL_LITERAL() != null) {
            return new LiteralExpression(ctx.REAL_LITERAL().getText(), LiteralType.REAL);
        }
        if (ctx.STRING_LITERAL() != null) {
            return new LiteralExpression(ctx.STRING_LITERAL().getText(), LiteralType.STRING);
        }
        return new LiteralExpression(ctx.getText().toLowerCase(Locale.ROOT), LiteralType.BOOLEAN);
    }

    private List<String> namesFromIdentifierList(PascalParser.IdentifierListContext context) {
        List<String> names = new ArrayList<>();
        for (PascalParser.IdentifierContext identifierContext : context.identifier()) {
            names.add(identifierContext.getText());
        }
        return names;
    }

    private List<ParameterNode> collectParameters(PascalParser.FormalParametersContext context) {
        if (context == null) {
            return List.of();
        }

        List<ParameterNode> parameters = new ArrayList<>();
        for (PascalParser.FormalParameterSectionContext sectionContext : context.formalParameterSection()) {
            TypeNode type = asType(visit(sectionContext.typeSpec()));
            for (String parameterName : namesFromIdentifierList(sectionContext.identifierList())) {
                parameters.add(new ParameterNode(parameterName, type));
            }
        }
        return parameters;
    }

    private List<Expression> collectExpressionList(List<PascalParser.ExpressionContext> expressionContexts) {
        List<Expression> expressions = new ArrayList<>();
        for (PascalParser.ExpressionContext expressionContext : expressionContexts) {
            expressions.add(asExpression(visit(expressionContext)));
        }
        return expressions;
    }

    private Expression asExpression(ASTNode node) {
        return (Expression) node;
    }

    private Statement asStatement(ASTNode node) {
        return (Statement) node;
    }

    private TypeNode asType(ASTNode node) {
        return (TypeNode) node;
    }

    private BlockNode asBlock(ASTNode node) {
        return (BlockNode) node;
    }

    @Override
    protected ASTNode defaultResult() {
        return null;
    }

    @Override
    protected ASTNode aggregateResult(ASTNode aggregate, ASTNode nextResult) {
        return nextResult != null ? nextResult : aggregate;
    }
}
