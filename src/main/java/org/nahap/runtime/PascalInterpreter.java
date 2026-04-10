package org.nahap.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

public final class PascalInterpreter implements AstVisitor<Object> {
    private static final BreakSignal BREAK_SIGNAL = new BreakSignal();
    private static final ContinueSignal CONTINUE_SIGNAL = new ContinueSignal();

    private final StringBuilder output = new StringBuilder();
    private final BufferedReader inputReader;
    private final Deque<String> tokenBuffer = new ArrayDeque<>();

    private Scope currentScope;

    public PascalInterpreter() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
    }

    PascalInterpreter(BufferedReader inputReader) {
        this.inputReader = Objects.requireNonNull(inputReader, "inputReader");
    }

    public String execute(ProgramNode program) {
        output.setLength(0);
        tokenBuffer.clear();

        Scope previous = currentScope;
        currentScope = null;
        try {
            program.accept(this);
            return output.toString();
        } catch (BreakSignal | ContinueSignal signal) {
            throw new IllegalStateException("break/continue used outside loop");
        } finally {
            currentScope = previous;
        }
    }

    @Override
    public Object visitProgram(ProgramNode node) {
        return node.getBlock().accept(this);
    }

    @Override
    public Object visitBlock(BlockNode node) {
        Scope previous = currentScope;
        currentScope = new Scope(previous);
        try {
            declareVariables(node.getVariableDeclarations());
            declareSubroutines(node.getSubroutineDeclarations());
            node.getBody().accept(this);
            return null;
        } finally {
            currentScope = previous;
        }
    }

    @Override
    public Object visitParameter(ParameterNode node) {
        return null;
    }

    @Override
    public Object visitVariableDeclaration(VariableDeclaration node) {
        return null;
    }

    @Override
    public Object visitProcedureDeclaration(ProcedureDeclaration node) {
        return null;
    }

    @Override
    public Object visitFunctionDeclaration(FunctionDeclaration node) {
        return null;
    }

    @Override
    public Object visitPrimitiveType(PrimitiveTypeNode node) {
        return null;
    }

    @Override
    public Object visitNamedType(NamedTypeNode node) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayTypeNode node) {
        return null;
    }

    @Override
    public Object visitCompoundStatement(CompoundStatement node) {
        for (Statement statement : node.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement node) {
        Object value = evaluate(node.getValue());
        assignTarget(node.getTarget(), value);
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement node) {
        if (toBoolean(evaluate(node.getCondition()))) {
            node.getThenBranch().accept(this);
            return null;
        }
        if (node.getElseBranch() != null) {
            node.getElseBranch().accept(this);
        }
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement node) {
        while (toBoolean(evaluate(node.getCondition()))) {
            try {
                node.getBody().accept(this);
            } catch (ContinueSignal signal) {
                continue;
            } catch (BreakSignal signal) {
                break;
            }
        }
        return null;
    }

    @Override
    public Object visitRepeatUntilStatement(RepeatUntilStatement node) {
        do {
            try {
                for (Statement statement : node.getBody()) {
                    statement.accept(this);
                }
            } catch (ContinueSignal signal) {
            } catch (BreakSignal signal) {
                break;
            }
        } while (!toBoolean(evaluate(node.getCondition())));
        return null;
    }

    @Override
    public Object visitForStatement(ForStatement node) {
        long start = toLong(evaluate(node.getStartExpression()));
        long end = toLong(evaluate(node.getEndExpression()));

        if (node.isDescending()) {
            for (long value = start; value >= end; value--) {
                assignVariable(node.getVariableName(), value);
                try {
                    node.getBody().accept(this);
                } catch (ContinueSignal signal) {
                    continue;
                } catch (BreakSignal signal) {
                    break;
                }
            }
            return null;
        }

        for (long value = start; value <= end; value++) {
            assignVariable(node.getVariableName(), value);
            try {
                node.getBody().accept(this);
            } catch (ContinueSignal signal) {
                continue;
            } catch (BreakSignal signal) {
                break;
            }
        }
        return null;
    }

    @Override
    public Object visitBreakStatement(BreakStatement node) {
        throw BREAK_SIGNAL;
    }

    @Override
    public Object visitContinueStatement(ContinueStatement node) {
        throw CONTINUE_SIGNAL;
    }

    @Override
    public Object visitProcedureCallStatement(ProcedureCallStatement node) {
        ProcedureBinding binding = resolveProcedure(node.getName());
        List<Object> argumentValues = evaluateExpressions(node.getArguments());
        invokeProcedure(binding, argumentValues);
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement node) {
        for (Expression argument : node.getArguments()) {
            output.append(stringify(evaluate(argument)));
        }
        if (node.isWriteLine()) {
            output.append(System.lineSeparator());
        }
        return null;
    }

    @Override
    public Object visitReadStatement(ReadStatement node) {
        try {
            if (node.getTargets().isEmpty()) {
                if (node.isReadLine()) {
                    inputReader.readLine();
                } else {
                    nextInputToken();
                }
                return null;
            }

            if (node.isReadLine()) {
                readLineTargets(node.getTargets());
                return null;
            }

            for (Expression target : node.getTargets()) {
                String token = nextInputToken();
                Object sample = readTarget(target);
                assignTarget(target, parseInputToken(token, sample));
            }
            return null;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read from stdin: " + e.getMessage(), e);
        }
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression node) {
        Object left = evaluate(node.getLeft());
        Object right = evaluate(node.getRight());

        return switch (node.getOperator()) {
            case ADD -> add(left, right);
            case SUBTRACT -> subtract(left, right);
            case MULTIPLY -> multiply(left, right);
            case DIVIDE_REAL -> divideReal(left, right);
            case DIVIDE_INT -> divideInt(left, right);
            case MOD -> mod(left, right);
            case EQUAL -> equalsValue(left, right);
            case NOT_EQUAL -> !equalsValue(left, right);
            case LESS_THAN -> compare(left, right) < 0;
            case LESS_OR_EQUAL -> compare(left, right) <= 0;
            case GREATER_THAN -> compare(left, right) > 0;
            case GREATER_OR_EQUAL -> compare(left, right) >= 0;
            case AND -> toBoolean(left) && toBoolean(right);
            case OR -> toBoolean(left) || toBoolean(right);
        };
    }

    @Override
    public Object visitUnaryExpression(UnaryExpression node) {
        Object value = evaluate(node.getExpression());
        return switch (node.getOperator()) {
            case PLUS -> plus(value);
            case MINUS -> minus(value);
            case NOT -> !toBoolean(value);
        };
    }

    @Override
    public Object visitCastExpression(CastExpression node) {
        Object value = evaluate(node.getExpression());
        String targetType = node.getTargetType().toLowerCase(Locale.ROOT);
        return switch (targetType) {
            case "integer" -> toLong(value);
            case "double" -> toDouble(value);
            case "boolean" -> toBoolean(value);
            case "string" -> stringify(value);
            case "char" -> {
                String text = stringify(value);
                if (text.isEmpty()) {
                    yield "\0";
                }
                yield text.substring(0, 1);
            }
            default -> value;
        };
    }

    @Override
    public Object visitLiteralExpression(LiteralExpression node) {
        return switch (node.getType()) {
            case INTEGER -> Long.parseLong(node.getText());
            case REAL -> Double.parseDouble(node.getText());
            case STRING -> decodeStringLiteral(node.getText());
            case BOOLEAN -> Boolean.parseBoolean(node.getText().toLowerCase(Locale.ROOT));
        };
    }

    @Override
    public Object visitVariableReferenceExpression(VariableReferenceExpression node) {
        return resolveVariable(node.getName());
    }

    @Override
    public Object visitArrayAccessExpression(ArrayAccessExpression node) {
        PascalArray array = resolveArray(node.getArrayName());
        long index = toLong(evaluate(node.getIndex()));
        return array.get(index);
    }

    @Override
    public Object visitFunctionCallExpression(FunctionCallExpression node) {
        FunctionBinding binding = resolveFunction(node.getName());
        List<Object> argumentValues = evaluateExpressions(node.getArguments());
        return invokeFunction(binding, argumentValues);
    }

    private Object evaluate(Expression expression) {
        return expression.accept(this);
    }

    private List<Object> evaluateExpressions(List<Expression> expressions) {
        List<Object> values = new ArrayList<>(expressions.size());
        for (Expression expression : expressions) {
            values.add(evaluate(expression));
        }
        return values;
    }

    private void declareVariables(List<VariableDeclaration> declarations) {
        for (VariableDeclaration declaration : declarations) {
            for (String name : declaration.getNames()) {
                defineVariable(name, createDefaultValue(declaration.getType()));
            }
        }
    }

    private void declareSubroutines(List<SubroutineDeclaration> declarations) {
        for (SubroutineDeclaration declaration : declarations) {
            if (declaration instanceof ProcedureDeclaration procedure) {
                currentScope.procedures.put(normalize(procedure.getName()), new ProcedureBinding(procedure, currentScope));
            } else if (declaration instanceof FunctionDeclaration function) {
                currentScope.functions.put(normalize(function.getName()), new FunctionBinding(function, currentScope));
            }
        }
    }

    private void invokeProcedure(ProcedureBinding binding, List<Object> arguments) {
        Scope invocationScope = new Scope(binding.closure);
        bindParameters(binding.declaration.getParameters(), arguments, invocationScope, binding.declaration.getName());

        Scope previous = currentScope;
        currentScope = invocationScope;
        try {
            binding.declaration.getBlock().accept(this);
        } finally {
            currentScope = previous;
        }
    }

    private Object invokeFunction(FunctionBinding binding, List<Object> arguments) {
        Scope invocationScope = new Scope(binding.closure);
        bindParameters(binding.declaration.getParameters(), arguments, invocationScope, binding.declaration.getName());

        String functionName = normalize(binding.declaration.getName());
        invocationScope.variables.put(functionName, createDefaultValue(binding.declaration.getReturnType()));

        Scope previous = currentScope;
        currentScope = invocationScope;
        try {
            binding.declaration.getBlock().accept(this);
        } finally {
            currentScope = previous;
        }

        return invocationScope.variables.get(functionName);
    }

    private void bindParameters(
            List<ParameterNode> parameters,
            List<Object> arguments,
            Scope invocationScope,
            String callableName) {
        if (parameters.size() != arguments.size()) {
            throw new IllegalStateException(
                    "Wrong argument count for "
                            + callableName
                            + ": expected "
                            + parameters.size()
                            + ", got "
                            + arguments.size());
        }

        for (int index = 0; index < parameters.size(); index++) {
            ParameterNode parameter = parameters.get(index);
            invocationScope.variables.put(normalize(parameter.getName()), copyValue(arguments.get(index)));
        }
    }

    private ProcedureBinding resolveProcedure(String name) {
        String key = normalize(name);
        for (Scope scope = currentScope; scope != null; scope = scope.parent) {
            ProcedureBinding binding = scope.procedures.get(key);
            if (binding != null) {
                return binding;
            }
        }
        throw new IllegalStateException("Unknown procedure: " + name);
    }

    private FunctionBinding resolveFunction(String name) {
        String key = normalize(name);
        for (Scope scope = currentScope; scope != null; scope = scope.parent) {
            FunctionBinding binding = scope.functions.get(key);
            if (binding != null) {
                return binding;
            }
        }
        throw new IllegalStateException("Unknown function: " + name);
    }

    private Object createDefaultValue(TypeNode type) {
        if (type instanceof PrimitiveTypeNode primitive) {
            return switch (primitive.getName().toLowerCase(Locale.ROOT)) {
                case "integer" -> 0L;
                case "double" -> 0.0;
                case "boolean" -> false;
                case "string" -> "";
                case "char" -> "\0";
                default -> null;
            };
        }

        if (type instanceof ArrayTypeNode arrayType) {
            PascalArray array = new PascalArray(arrayType.getLowerBound(), arrayType.getUpperBound());
            for (int index = arrayType.getLowerBound(); index <= arrayType.getUpperBound(); index++) {
                array.set(index, createDefaultValue(arrayType.getElementType()));
            }
            return array;
        }

        return null;
    }

    private void defineVariable(String name, Object value) {
        currentScope.variables.put(normalize(name), value);
    }

    private void assignVariable(String name, Object value) {
        String key = normalize(name);
        Scope owner = findVariableOwner(key);
        if (owner == null) {
            throw new IllegalStateException("Unknown variable: " + name);
        }
        owner.variables.put(key, value);
    }

    private Object resolveVariable(String name) {
        String key = normalize(name);
        Scope owner = findVariableOwner(key);
        if (owner == null) {
            throw new IllegalStateException("Unknown variable: " + name);
        }
        return owner.variables.get(key);
    }

    private Scope findVariableOwner(String key) {
        for (Scope scope = currentScope; scope != null; scope = scope.parent) {
            if (scope.variables.containsKey(key)) {
                return scope;
            }
        }
        return null;
    }

    private void assignTarget(Expression target, Object value) {
        if (target instanceof VariableReferenceExpression variableReference) {
            assignVariable(variableReference.getName(), value);
            return;
        }

        if (target instanceof ArrayAccessExpression arrayAccess) {
            PascalArray array = resolveArray(arrayAccess.getArrayName());
            long index = toLong(evaluate(arrayAccess.getIndex()));
            array.set(index, value);
            return;
        }

        throw new IllegalStateException("Unsupported assignment target");
    }

    private Object readTarget(Expression target) {
        if (target instanceof VariableReferenceExpression variableReference) {
            return resolveVariable(variableReference.getName());
        }

        if (target instanceof ArrayAccessExpression arrayAccess) {
            PascalArray array = resolveArray(arrayAccess.getArrayName());
            long index = toLong(evaluate(arrayAccess.getIndex()));
            return array.get(index);
        }

        throw new IllegalStateException("Unsupported read target");
    }

    private PascalArray resolveArray(String name) {
        Object value = resolveVariable(name);
        if (value instanceof PascalArray array) {
            return array;
        }
        throw new IllegalStateException("Variable is not an array: " + name);
    }

    private void readLineTargets(List<Expression> targets) throws IOException {
        String line = inputReader.readLine();
        if (line == null) {
            throw new IllegalStateException("No input available for ReadLn");
        }

        if (targets.size() == 1) {
            Expression target = targets.get(0);
            Object sample = readTarget(target);
            assignTarget(target, parseInputToken(line, sample));
            return;
        }

        String[] tokens = tokenize(line);
        if (tokens.length < targets.size()) {
            throw new IllegalStateException("Not enough values for ReadLn");
        }

        for (int index = 0; index < targets.size(); index++) {
            Expression target = targets.get(index);
            Object sample = readTarget(target);
            assignTarget(target, parseInputToken(tokens[index], sample));
        }
    }

    private String nextInputToken() throws IOException {
        while (tokenBuffer.isEmpty()) {
            String line = inputReader.readLine();
            if (line == null) {
                throw new IllegalStateException("No input available for Read");
            }

            String[] tokens = tokenize(line);
            for (String token : tokens) {
                tokenBuffer.addLast(token);
            }
        }

        return tokenBuffer.removeFirst();
    }

    private String[] tokenize(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s+");
    }

    private Object parseInputToken(String token, Object sampleValue) {
        if (sampleValue instanceof Long) {
            return Long.parseLong(token);
        }
        if (sampleValue instanceof Double) {
            return Double.parseDouble(token);
        }
        if (sampleValue instanceof Boolean) {
            return parseBoolean(token);
        }
        return token;
    }

    private boolean parseBoolean(String token) {
        if ("true".equalsIgnoreCase(token)) {
            return true;
        }
        if ("false".equalsIgnoreCase(token)) {
            return false;
        }
        throw new IllegalStateException("Invalid boolean value: " + token);
    }

    private Object plus(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return toDouble(value);
        }
        return toLong(value);
    }

    private Object minus(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return -toDouble(value);
        }
        return -toLong(value);
    }

    private Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return stringify(left) + stringify(right);
        }
        if (isReal(left) || isReal(right)) {
            return toDouble(left) + toDouble(right);
        }
        return toLong(left) + toLong(right);
    }

    private Object subtract(Object left, Object right) {
        if (isReal(left) || isReal(right)) {
            return toDouble(left) - toDouble(right);
        }
        return toLong(left) - toLong(right);
    }

    private Object multiply(Object left, Object right) {
        if (isReal(left) || isReal(right)) {
            return toDouble(left) * toDouble(right);
        }
        return toLong(left) * toLong(right);
    }

    private Object divideReal(Object left, Object right) {
        return toDouble(left) / toDouble(right);
    }

    private Object divideInt(Object left, Object right) {
        return toLong(left) / toLong(right);
    }

    private Object mod(Object left, Object right) {
        return toLong(left) % toLong(right);
    }

    private boolean equalsValue(Object left, Object right) {
        if (isNumeric(left) && isNumeric(right)) {
            if (isReal(left) || isReal(right)) {
                return Double.compare(toDouble(left), toDouble(right)) == 0;
            }
            return toLong(left) == toLong(right);
        }
        return Objects.equals(left, right);
    }

    private int compare(Object left, Object right) {
        if (isNumeric(left) && isNumeric(right)) {
            if (isReal(left) || isReal(right)) {
                return Double.compare(toDouble(left), toDouble(right));
            }
            return Long.compare(toLong(left), toLong(right));
        }

        if (left instanceof String leftString && right instanceof String rightString) {
            return leftString.compareTo(rightString);
        }

        throw new IllegalStateException("Cannot compare values: " + left + " and " + right);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalStateException("Expected boolean value but got: " + value);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            if (value instanceof Double || value instanceof Float) {
                double real = number.doubleValue();
                if (real != Math.rint(real)) {
                    throw new IllegalStateException("Expected integer value but got: " + value);
                }
            }
            return number.longValue();
        }
        throw new IllegalStateException("Expected numeric value but got: " + value);
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException("Expected numeric value but got: " + value);
    }

    private boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    private boolean isReal(Object value) {
        return value instanceof Double || value instanceof Float;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Double number) {
            if (number == Math.rint(number)) {
                return Long.toString(number.longValue());
            }
            return Double.toString(number);
        }
        if (value instanceof Boolean bool) {
            return bool ? "true" : "false";
        }
        return String.valueOf(value);
    }

    private Object copyValue(Object value) {
        if (value instanceof PascalArray array) {
            return array.copy();
        }
        return value;
    }

    private String decodeStringLiteral(String literalText) {
        if (literalText.length() >= 2 && literalText.charAt(0) == '\''
                && literalText.charAt(literalText.length() - 1) == '\'') {
            String body = literalText.substring(1, literalText.length() - 1);
            return body.replace("''", "'");
        }
        return literalText;
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static final class Scope {
        private final Scope parent;
        private final Map<String, Object> variables = new HashMap<>();
        private final Map<String, ProcedureBinding> procedures = new HashMap<>();
        private final Map<String, FunctionBinding> functions = new HashMap<>();

        private Scope(Scope parent) {
            this.parent = parent;
        }
    }

    private static final class ProcedureBinding {
        private final ProcedureDeclaration declaration;
        private final Scope closure;

        private ProcedureBinding(ProcedureDeclaration declaration, Scope closure) {
            this.declaration = declaration;
            this.closure = closure;
        }
    }

    private static final class FunctionBinding {
        private final FunctionDeclaration declaration;
        private final Scope closure;

        private FunctionBinding(FunctionDeclaration declaration, Scope closure) {
            this.declaration = declaration;
            this.closure = closure;
        }
    }

    private static final class PascalArray {
        private final int lowerBound;
        private final int upperBound;
        private final Object[] values;

        private PascalArray(int lowerBound, int upperBound) {
            if (upperBound < lowerBound) {
                throw new IllegalStateException("Invalid array bounds: [" + lowerBound + ".." + upperBound + "]");
            }
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.values = new Object[upperBound - lowerBound + 1];
        }

        private Object get(long index) {
            int offset = toOffset(index);
            return values[offset];
        }

        private void set(long index, Object value) {
            int offset = toOffset(index);
            values[offset] = value;
        }

        private PascalArray copy() {
            PascalArray copy = new PascalArray(lowerBound, upperBound);
            for (int index = 0; index < values.length; index++) {
                copy.values[index] = values[index] instanceof PascalArray array ? array.copy() : values[index];
            }
            return copy;
        }

        private int toOffset(long index) {
            if (index < lowerBound || index > upperBound) {
                throw new IllegalStateException(
                        "Array index out of bounds: " + index + " for [" + lowerBound + ".." + upperBound + "]");
            }
            return (int) (index - lowerBound);
        }
    }

    private static final class BreakSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private BreakSignal() {
            super(null, null, false, false);
        }
    }

    private static final class ContinueSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private ContinueSignal() {
            super(null, null, false, false);
        }
    }
}
