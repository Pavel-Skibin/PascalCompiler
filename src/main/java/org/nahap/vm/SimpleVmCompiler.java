package org.nahap.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.nahap.ast.BlockNode;
import org.nahap.ast.ParameterNode;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.decl.FunctionDeclaration;
import org.nahap.ast.decl.ProcedureDeclaration;
import org.nahap.ast.decl.SubroutineDeclaration;
import org.nahap.ast.decl.VariableDeclaration;
import org.nahap.ast.expr.BinaryExpression;
import org.nahap.ast.expr.BinaryOperator;
import org.nahap.ast.expr.CastExpression;
import org.nahap.ast.expr.Expression;
import org.nahap.ast.expr.FunctionCallExpression;
import org.nahap.ast.expr.LiteralExpression;
import org.nahap.ast.expr.LiteralType;
import org.nahap.ast.expr.SystemFunctionCallExpression;
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
import org.nahap.ast.visitor.AstVisitor;

public final class SimpleVmCompiler implements AstVisitor<Void> {
    private VmProgramBuilder builder = new VmProgramBuilder();
    private final ArrayList<String> tempVariables = new ArrayList<>();
    private final ArrayList<LoopFrame> loopFrames = new ArrayList<>();
    private String programName;

    public SimpleVmProgram compile(ProgramNode program) {
        Objects.requireNonNull(program, "program");
        builder = new VmProgramBuilder();
        tempVariables.clear();
        loopFrames.clear();
        programName = null;
        program.accept(this);
        return new SimpleVmProgram(programName, builder.variables(), builder.instructions());
    }

    @Override
    public Void visitProgram(ProgramNode node) {
        programName = node.getName();
        node.getBlock().accept(this);
        return null;
    }

    @Override
    public Void visitBlock(BlockNode node) {
        if (!node.getSubroutineDeclarations().isEmpty()) {
            throw new IllegalStateException("Simple VM does not support procedures or functions yet");
        }

        for (VariableDeclaration declaration : node.getVariableDeclarations()) {
            registerVariables(declaration);
        }

        node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visitParameter(ParameterNode node) {
        throw unsupported("parameters");
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclaration node) {
        return null;
    }

    @Override
    public Void visitProcedureDeclaration(ProcedureDeclaration node) {
        throw unsupported("procedure declarations");
    }

    @Override
    public Void visitFunctionDeclaration(FunctionDeclaration node) {
        throw unsupported("function declarations");
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeNode node) {
        return null;
    }

    @Override
    public Void visitNamedType(NamedTypeNode node) {
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeNode node) {
        throw unsupported("array variables");
    }

    @Override
    public Void visitCompoundStatement(CompoundStatement node) {
        for (Statement statement : node.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visitAssignmentStatement(AssignmentStatement node) {
        VariableReferenceExpression variable = requireVariableTarget(node.getTarget());
        node.getValue().accept(this);
        builder.emit(SimpleVmOpCode.STORE_VAR, normalize(variable.getName()));
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement node) {
        String elseLabel = builder.newLabel("if_else");
        String endLabel = builder.newLabel("if_end");

        node.getCondition().accept(this);
        builder.emitJump(SimpleVmOpCode.JUMP_IF_FALSE, elseLabel);
        node.getThenBranch().accept(this);
        builder.emitJump(SimpleVmOpCode.JUMP, endLabel);
        builder.markLabel(elseLabel);
        if (node.getElseBranch() != null) {
            node.getElseBranch().accept(this);
        }
        builder.markLabel(endLabel);
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement node) {
        String startLabel = builder.newLabel("while_start");
        String endLabel = builder.newLabel("while_end");

        loopFrames.add(new LoopFrame(startLabel, startLabel, endLabel));
        builder.markLabel(startLabel);
        node.getCondition().accept(this);
        builder.emitJump(SimpleVmOpCode.JUMP_IF_FALSE, endLabel);
        node.getBody().accept(this);
        builder.emitJump(SimpleVmOpCode.JUMP, startLabel);
        builder.markLabel(endLabel);
        loopFrames.remove(loopFrames.size() - 1);
        return null;
    }

    @Override
    public Void visitRepeatUntilStatement(RepeatUntilStatement node) {
        String startLabel = builder.newLabel("repeat_start");
        String conditionLabel = builder.newLabel("repeat_condition");
        String endLabel = builder.newLabel("repeat_end");

        loopFrames.add(new LoopFrame(conditionLabel, conditionLabel, endLabel));
        builder.markLabel(startLabel);
        for (Statement statement : node.getBody()) {
            statement.accept(this);
        }
        builder.markLabel(conditionLabel);
        node.getCondition().accept(this);
        builder.emitJump(SimpleVmOpCode.JUMP_IF_FALSE, startLabel);
        builder.markLabel(endLabel);
        loopFrames.remove(loopFrames.size() - 1);
        return null;
    }

    @Override
    public Void visitForStatement(ForStatement node) {
        VariableReferenceExpression variable = new VariableReferenceExpression(node.getVariableName());
        String endVariable = declareTemporaryVariable("for_end");
        String startLabel = builder.newLabel("for_start");
        String updateLabel = builder.newLabel("for_update");
        String endLabel = builder.newLabel("for_end_label");

        node.getStartExpression().accept(this);
        builder.emit(SimpleVmOpCode.STORE_VAR, normalize(variable.getName()));
        node.getEndExpression().accept(this);
        builder.emit(SimpleVmOpCode.STORE_VAR, endVariable);

        loopFrames.add(new LoopFrame(updateLabel, updateLabel, endLabel));
        builder.markLabel(startLabel);
        builder.emit(SimpleVmOpCode.LOAD_VAR, normalize(variable.getName()));
        builder.emit(SimpleVmOpCode.LOAD_VAR, endVariable);
        builder.emit(node.isDescending() ? SimpleVmOpCode.GREATER_OR_EQUAL : SimpleVmOpCode.LESS_OR_EQUAL);
        builder.emitJump(SimpleVmOpCode.JUMP_IF_FALSE, endLabel);

        node.getBody().accept(this);
        builder.markLabel(updateLabel);
        builder.emit(SimpleVmOpCode.LOAD_VAR, normalize(variable.getName()));
        builder.emit(SimpleVmOpCode.PUSH_CONST, 1L);
        builder.emit(node.isDescending() ? SimpleVmOpCode.SUBTRACT : SimpleVmOpCode.ADD);
        builder.emit(SimpleVmOpCode.STORE_VAR, normalize(variable.getName()));
        builder.emitJump(SimpleVmOpCode.JUMP, startLabel);
        builder.markLabel(endLabel);
        loopFrames.remove(loopFrames.size() - 1);
        return null;
    }

    @Override
    public Void visitBreakStatement(BreakStatement node) {
        builder.emitJump(SimpleVmOpCode.JUMP, currentLoop().breakLabel());
        return null;
    }

    @Override
    public Void visitContinueStatement(ContinueStatement node) {
        builder.emitJump(SimpleVmOpCode.JUMP, currentLoop().continueLabel());
        return null;
    }

    @Override
    public Void visitProcedureCallStatement(ProcedureCallStatement node) {
        throw unsupported("procedure calls");
    }

    @Override
    public Void visitWriteStatement(WriteStatement node) {
        for (Expression argument : node.getArguments()) {
            argument.accept(this);
            builder.emit(SimpleVmOpCode.PRINT);
        }
        if (node.isWriteLine()) {
            builder.emit(SimpleVmOpCode.PRINT_LINE);
        }
        return null;
    }

    @Override
    public Void visitReadStatement(ReadStatement node) {
        throw unsupported("read statements");
    }

    @Override
    public Void visitBinaryExpression(BinaryExpression node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        builder.emit(mapBinary(node.getOperator()));
        return null;
    }

    @Override
    public Void visitCastExpression(CastExpression node) {
        node.getExpression().accept(this);
        builder.emit(SimpleVmOpCode.CAST, node.getTargetType());
        return null;
    }

    @Override
    public Void visitUnaryExpression(UnaryExpression node) {
        node.getExpression().accept(this);
        if (node.getOperator() == UnaryOperator.MINUS) {
            builder.emit(SimpleVmOpCode.NEGATE);
        } else if (node.getOperator() == UnaryOperator.NOT) {
            builder.emit(SimpleVmOpCode.NOT);
        }
        return null;
    }

    @Override
    public Void visitLiteralExpression(LiteralExpression node) {
        builder.emit(SimpleVmOpCode.PUSH_CONST, literalValue(node));
        return null;
    }

    @Override
    public Void visitVariableReferenceExpression(VariableReferenceExpression node) {
        builder.emit(SimpleVmOpCode.LOAD_VAR, normalize(node.getName()));
        return null;
    }

    @Override
    public Void visitArrayAccessExpression(org.nahap.ast.expr.ArrayAccessExpression node) {
        throw unsupported("array access");
    }

    @Override
    public Void visitFunctionCallExpression(FunctionCallExpression node) {
        throw unsupported("function calls");
    }

    @Override
    public Void visitSystemFunctionCallExpression(SystemFunctionCallExpression node) {
        throw unsupported("system functions");
    }

    private void registerVariables(VariableDeclaration declaration) {
        String typeName = typeName(declaration.getType());
        for (String name : declaration.getNames()) {
            String normalized = normalize(name);
            builder.addVariable(normalized, typeName);
        }
    }

    private VariableReferenceExpression requireVariableTarget(Expression expression) {
        if (expression instanceof VariableReferenceExpression variableReference) {
            return variableReference;
        }
        throw unsupported("complex assignment targets");
    }

    private String declareTemporaryVariable(String prefix) {
        String name = normalize("__" + prefix + "_" + tempVariables.size());
        tempVariables.add(name);
        builder.addVariable(name, "integer");
        return name;
    }

    private LoopFrame currentLoop() {
        if (loopFrames.isEmpty()) {
            throw new IllegalStateException("break/continue used outside loop");
        }
        return loopFrames.get(loopFrames.size() - 1);
    }

    private SimpleVmOpCode mapBinary(BinaryOperator operator) {
        return switch (operator) {
            case ADD -> SimpleVmOpCode.ADD;
            case SUBTRACT -> SimpleVmOpCode.SUBTRACT;
            case MULTIPLY -> SimpleVmOpCode.MULTIPLY;
            case DIVIDE_INT -> SimpleVmOpCode.DIVIDE_INT;
            case MOD -> SimpleVmOpCode.MOD;
            case EQUAL -> SimpleVmOpCode.EQUAL;
            case NOT_EQUAL -> SimpleVmOpCode.NOT_EQUAL;
            case LESS_THAN -> SimpleVmOpCode.LESS_THAN;
            case LESS_OR_EQUAL -> SimpleVmOpCode.LESS_OR_EQUAL;
            case GREATER_THAN -> SimpleVmOpCode.GREATER_THAN;
            case GREATER_OR_EQUAL -> SimpleVmOpCode.GREATER_OR_EQUAL;
            case AND -> SimpleVmOpCode.AND;
            case OR -> SimpleVmOpCode.OR;
            case DIVIDE_REAL -> throw unsupported("real division");
        };
    }

    private Object literalValue(LiteralExpression literal) {
        return switch (literal.getType()) {
            case INTEGER -> Long.parseLong(literal.getText());
            case REAL -> Double.parseDouble(literal.getText());
            case STRING -> decodeString(literal.getText());
            case BOOLEAN -> Boolean.parseBoolean(literal.getText().toLowerCase(Locale.ROOT));
        };
    }

    private String typeName(TypeNode typeNode) {
        if (typeNode instanceof PrimitiveTypeNode primitiveTypeNode) {
            return primitiveTypeNode.getName().toLowerCase(Locale.ROOT);
        }

        if (typeNode instanceof NamedTypeNode namedTypeNode) {
            return namedTypeNode.getName().toLowerCase(Locale.ROOT);
        }

        throw unsupported("array variables");
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private String decodeString(String text) {
        if (text.length() >= 2 && text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\'') {
            return text.substring(1, text.length() - 1).replace("''", "'");
        }
        return text;
    }

    private IllegalStateException unsupported(String feature) {
        return new IllegalStateException("Simple VM does not support " + feature + " yet");
    }

    private record LoopFrame(String continueLabel, String updateLabel, String breakLabel) {
    }

    private static final class VmProgramBuilder {
        private final List<SimpleVmInstruction> instructions = new ArrayList<>();
        private final List<SimpleVmProgram.VariableDefinition> variables = new ArrayList<>();
        private final Map<String, Integer> labels = new HashMap<>();
        private int labelCounter;

        void addVariable(String name, String typeName) {
            variables.add(new SimpleVmProgram.VariableDefinition(name, typeName));
        }

        List<SimpleVmProgram.VariableDefinition> variables() {
            return List.copyOf(variables);
        }

        List<SimpleVmInstruction> instructions() {
            List<SimpleVmInstruction> resolved = new ArrayList<>(instructions.size());
            for (SimpleVmInstruction instruction : instructions) {
                if ((instruction.opcode() == SimpleVmOpCode.JUMP || instruction.opcode() == SimpleVmOpCode.JUMP_IF_FALSE)
                        && instruction.operand() instanceof String label) {
                    Integer target = labels.get(label);
                    if (target == null) {
                        throw new IllegalStateException("Unresolved label: " + label);
                    }
                    resolved.add(SimpleVmInstruction.of(instruction.opcode(), target));
                } else {
                    resolved.add(instruction);
                }
            }
            return List.copyOf(resolved);
        }

        String newLabel(String prefix) {
            return prefix + '_' + labelCounter++;
        }

        void markLabel(String label) {
            labels.put(label, instructions.size());
        }

        void emit(SimpleVmOpCode opcode) {
            instructions.add(SimpleVmInstruction.of(opcode));
        }

        void emit(SimpleVmOpCode opcode, Object operand) {
            instructions.add(SimpleVmInstruction.of(opcode, operand));
        }

        void emitJump(SimpleVmOpCode opcode, String label) {
            instructions.add(SimpleVmInstruction.of(opcode, label));
        }
    }
}