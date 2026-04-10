package org.nahap.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nahap.ast.BlockNode;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.decl.FunctionDeclaration;
import org.nahap.ast.decl.ProcedureDeclaration;
import org.nahap.ast.decl.SubroutineDeclaration;
import org.nahap.ast.expr.ArrayAccessExpression;
import org.nahap.ast.expr.BinaryExpression;
import org.nahap.ast.expr.BinaryOperator;
import org.nahap.ast.expr.CastExpression;
import org.nahap.ast.expr.Expression;
import org.nahap.ast.expr.FunctionCallExpression;
import org.nahap.ast.expr.LiteralExpression;
import org.nahap.ast.expr.LiteralType;
import org.nahap.ast.expr.UnaryExpression;
import org.nahap.ast.expr.UnaryOperator;
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

public final class AstOptimizer {
    public ProgramNode optimize(ProgramNode program) {
        return new ProgramNode(program.getName(), optimizeBlock(program.getBlock()));
    }

    private BlockNode optimizeBlock(BlockNode block) {
        List<SubroutineDeclaration> subroutines = new ArrayList<>();
        for (SubroutineDeclaration subroutine : block.getSubroutineDeclarations()) {
            if (subroutine instanceof FunctionDeclaration functionDeclaration) {
                subroutines.add(new FunctionDeclaration(
                        functionDeclaration.getName(),
                        functionDeclaration.getParameters(),
                        functionDeclaration.getReturnType(),
                        optimizeBlock(functionDeclaration.getBlock())));
            } else if (subroutine instanceof ProcedureDeclaration procedureDeclaration) {
                subroutines.add(new ProcedureDeclaration(
                        procedureDeclaration.getName(),
                        procedureDeclaration.getParameters(),
                        optimizeBlock(procedureDeclaration.getBlock())));
            } else {
                subroutines.add(subroutine);
            }
        }

        CompoundStatement optimizedBody = (CompoundStatement) optimizeStatement(block.getBody());
        return new BlockNode(block.getVariableDeclarations(), subroutines, optimizedBody);
    }

    private Statement optimizeStatement(Statement statement) {
        if (statement instanceof CompoundStatement compoundStatement) {
            List<Statement> optimized = new ArrayList<>();
            for (Statement nested : compoundStatement.getStatements()) {
                Statement optimizedNested = optimizeStatement(nested);
                if (optimizedNested instanceof CompoundStatement nestedCompound) {
                    optimized.addAll(nestedCompound.getStatements());
                } else {
                    optimized.add(optimizedNested);
                }
            }
            return new CompoundStatement(optimized);
        }

        if (statement instanceof AssignmentStatement assignmentStatement) {
            return new AssignmentStatement(
                    optimizeExpression(assignmentStatement.getTarget()),
                    optimizeExpression(assignmentStatement.getValue()));
        }

        if (statement instanceof IfStatement ifStatement) {
            Expression condition = optimizeExpression(ifStatement.getCondition());
            Statement thenBranch = optimizeStatement(ifStatement.getThenBranch());
            Statement elseBranch = ifStatement.getElseBranch() == null ? null : optimizeStatement(ifStatement.getElseBranch());

            Boolean value = tryBooleanLiteral(condition);
            if (value != null) {
                if (value) {
                    return thenBranch;
                }
                return elseBranch == null ? new CompoundStatement(List.of()) : elseBranch;
            }

            return new IfStatement(condition, thenBranch, elseBranch);
        }

        if (statement instanceof WhileStatement whileStatement) {
            Expression condition = optimizeExpression(whileStatement.getCondition());
            Statement body = optimizeStatement(whileStatement.getBody());

            Boolean value = tryBooleanLiteral(condition);
            if (Boolean.FALSE.equals(value)) {
                return new CompoundStatement(List.of());
            }

            return new WhileStatement(condition, body);
        }

        if (statement instanceof RepeatUntilStatement repeatUntilStatement) {
            List<Statement> body = new ArrayList<>();
            for (Statement nested : repeatUntilStatement.getBody()) {
                body.add(optimizeStatement(nested));
            }
            Expression condition = optimizeExpression(repeatUntilStatement.getCondition());
            return new RepeatUntilStatement(body, condition);
        }

        if (statement instanceof ForStatement forStatement) {
            Expression start = optimizeExpression(forStatement.getStartExpression());
            Expression end = optimizeExpression(forStatement.getEndExpression());
            Statement body = optimizeStatement(forStatement.getBody());

            Long startValue = tryIntegerLiteral(start);
            Long endValue = tryIntegerLiteral(end);
            if (startValue != null && endValue != null) {
                boolean empty = forStatement.isDescending() ? startValue < endValue : startValue > endValue;
                if (empty) {
                    return new CompoundStatement(List.of());
                }
            }

            return new ForStatement(forStatement.getVariableName(), start, end, forStatement.isDescending(), body);
        }

        if (statement instanceof ProcedureCallStatement procedureCallStatement) {
            List<Expression> arguments = new ArrayList<>();
            for (Expression argument : procedureCallStatement.getArguments()) {
                arguments.add(optimizeExpression(argument));
            }
            return new ProcedureCallStatement(procedureCallStatement.getName(), arguments);
        }

        if (statement instanceof WriteStatement writeStatement) {
            List<Expression> arguments = new ArrayList<>();
            for (Expression argument : writeStatement.getArguments()) {
                arguments.add(optimizeExpression(argument));
            }
            return new WriteStatement(writeStatement.isWriteLine(), arguments);
        }

        if (statement instanceof ReadStatement readStatement) {
            List<Expression> targets = new ArrayList<>();
            for (Expression target : readStatement.getTargets()) {
                targets.add(optimizeExpression(target));
            }
            return new ReadStatement(readStatement.isReadLine(), targets);
        }

        if (statement instanceof BreakStatement || statement instanceof ContinueStatement) {
            return statement;
        }

        return statement;
    }

    private Expression optimizeExpression(Expression expression) {
        if (expression instanceof BinaryExpression binaryExpression) {
            Expression left = optimizeExpression(binaryExpression.getLeft());
            Expression right = optimizeExpression(binaryExpression.getRight());
            LiteralExpression folded = tryFoldBinary(binaryExpression.getOperator(), left, right);
            return folded != null ? folded : new BinaryExpression(left, binaryExpression.getOperator(), right);
        }

        if (expression instanceof UnaryExpression unaryExpression) {
            Expression nested = optimizeExpression(unaryExpression.getExpression());
            LiteralExpression folded = tryFoldUnary(unaryExpression.getOperator(), nested);
            return folded != null ? folded : new UnaryExpression(unaryExpression.getOperator(), nested);
        }

        if (expression instanceof CastExpression castExpression) {
            Expression nested = optimizeExpression(castExpression.getExpression());
            LiteralExpression folded = tryFoldCast(castExpression.getTargetType(), nested);
            return folded != null ? folded : new CastExpression(nested, castExpression.getTargetType());
        }

        if (expression instanceof ArrayAccessExpression arrayAccessExpression) {
            return new ArrayAccessExpression(
                    arrayAccessExpression.getArrayName(),
                    optimizeExpression(arrayAccessExpression.getIndex()));
        }

        if (expression instanceof FunctionCallExpression functionCallExpression) {
            List<Expression> args = new ArrayList<>();
            for (Expression argument : functionCallExpression.getArguments()) {
                args.add(optimizeExpression(argument));
            }
            return new FunctionCallExpression(functionCallExpression.getName(), args);
        }

        return expression;
    }

    private LiteralExpression tryFoldUnary(UnaryOperator operator, Expression nested) {
        if (!(nested instanceof LiteralExpression literal)) {
            return null;
        }

        return switch (operator) {
            case PLUS -> {
                if (literal.getType() == LiteralType.INTEGER || literal.getType() == LiteralType.REAL) {
                    yield literal;
                }
                yield null;
            }
            case MINUS -> {
                if (literal.getType() == LiteralType.INTEGER) {
                    yield new LiteralExpression(Long.toString(-Long.parseLong(literal.getText())), LiteralType.INTEGER);
                }
                if (literal.getType() == LiteralType.REAL) {
                    yield new LiteralExpression(Double.toString(-Double.parseDouble(literal.getText())), LiteralType.REAL);
                }
                yield null;
            }
            case NOT -> {
                if (literal.getType() == LiteralType.BOOLEAN) {
                    boolean value = Boolean.parseBoolean(literal.getText().toLowerCase(Locale.ROOT));
                    yield new LiteralExpression(Boolean.toString(!value), LiteralType.BOOLEAN);
                }
                yield null;
            }
        };
    }

    private LiteralExpression tryFoldCast(String targetType, Expression nested) {
        if (!(nested instanceof LiteralExpression literal)) {
            return null;
        }

        String normalized = targetType.toLowerCase(Locale.ROOT);
        if ("double".equals(normalized) && literal.getType() == LiteralType.INTEGER) {
            return new LiteralExpression(Double.toString(Long.parseLong(literal.getText())), LiteralType.REAL);
        }
        if ("integer".equals(normalized) && literal.getType() == LiteralType.REAL) {
            double value = Double.parseDouble(literal.getText());
            long rounded = Math.round(value);
            if (rounded == value) {
                return new LiteralExpression(Long.toString(rounded), LiteralType.INTEGER);
            }
        }
        return null;
    }

    private LiteralExpression tryFoldBinary(BinaryOperator operator, Expression left, Expression right) {
        if (!(left instanceof LiteralExpression leftLiteral) || !(right instanceof LiteralExpression rightLiteral)) {
            return null;
        }

        if (leftLiteral.getType() == LiteralType.BOOLEAN && rightLiteral.getType() == LiteralType.BOOLEAN) {
            boolean leftBool = Boolean.parseBoolean(leftLiteral.getText().toLowerCase(Locale.ROOT));
            boolean rightBool = Boolean.parseBoolean(rightLiteral.getText().toLowerCase(Locale.ROOT));
            return switch (operator) {
                case AND -> new LiteralExpression(Boolean.toString(leftBool && rightBool), LiteralType.BOOLEAN);
                case OR -> new LiteralExpression(Boolean.toString(leftBool || rightBool), LiteralType.BOOLEAN);
                case EQUAL -> new LiteralExpression(Boolean.toString(leftBool == rightBool), LiteralType.BOOLEAN);
                case NOT_EQUAL -> new LiteralExpression(Boolean.toString(leftBool != rightBool), LiteralType.BOOLEAN);
                default -> null;
            };
        }

        if (isNumericLiteral(leftLiteral) && isNumericLiteral(rightLiteral)) {
            boolean real = leftLiteral.getType() == LiteralType.REAL || rightLiteral.getType() == LiteralType.REAL;
            double leftValue = Double.parseDouble(leftLiteral.getText());
            double rightValue = Double.parseDouble(rightLiteral.getText());

            return switch (operator) {
                case ADD -> numberLiteral(leftValue + rightValue, real);
                case SUBTRACT -> numberLiteral(leftValue - rightValue, real);
                case MULTIPLY -> numberLiteral(leftValue * rightValue, real);
                case DIVIDE_REAL -> numberLiteral(leftValue / rightValue, true);
                case DIVIDE_INT -> new LiteralExpression(Long.toString((long) leftValue / (long) rightValue), LiteralType.INTEGER);
                case MOD -> new LiteralExpression(Long.toString((long) leftValue % (long) rightValue), LiteralType.INTEGER);
                case EQUAL -> new LiteralExpression(Boolean.toString(Double.compare(leftValue, rightValue) == 0), LiteralType.BOOLEAN);
                case NOT_EQUAL -> new LiteralExpression(Boolean.toString(Double.compare(leftValue, rightValue) != 0), LiteralType.BOOLEAN);
                case LESS_THAN -> new LiteralExpression(Boolean.toString(leftValue < rightValue), LiteralType.BOOLEAN);
                case LESS_OR_EQUAL -> new LiteralExpression(Boolean.toString(leftValue <= rightValue), LiteralType.BOOLEAN);
                case GREATER_THAN -> new LiteralExpression(Boolean.toString(leftValue > rightValue), LiteralType.BOOLEAN);
                case GREATER_OR_EQUAL -> new LiteralExpression(Boolean.toString(leftValue >= rightValue), LiteralType.BOOLEAN);
                default -> null;
            };
        }

        if (leftLiteral.getType() == LiteralType.STRING && rightLiteral.getType() == LiteralType.STRING) {
            String leftValue = leftLiteral.getText();
            String rightValue = rightLiteral.getText();
            return switch (operator) {
                case EQUAL -> new LiteralExpression(Boolean.toString(leftValue.equals(rightValue)), LiteralType.BOOLEAN);
                case NOT_EQUAL -> new LiteralExpression(Boolean.toString(!leftValue.equals(rightValue)), LiteralType.BOOLEAN);
                default -> null;
            };
        }

        return null;
    }

    private boolean isNumericLiteral(LiteralExpression literalExpression) {
        return literalExpression.getType() == LiteralType.INTEGER || literalExpression.getType() == LiteralType.REAL;
    }

    private LiteralExpression numberLiteral(double value, boolean real) {
        if (real) {
            return new LiteralExpression(Double.toString(value), LiteralType.REAL);
        }
        return new LiteralExpression(Long.toString((long) value), LiteralType.INTEGER);
    }

    private Boolean tryBooleanLiteral(Expression expression) {
        if (expression instanceof LiteralExpression literalExpression
                && literalExpression.getType() == LiteralType.BOOLEAN) {
            return Boolean.parseBoolean(literalExpression.getText().toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private Long tryIntegerLiteral(Expression expression) {
        if (expression instanceof LiteralExpression literalExpression
                && literalExpression.getType() == LiteralType.INTEGER) {
            return Long.parseLong(literalExpression.getText());
        }
        return null;
    }
}
