package org.nahap.semantic;

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

public final class SemanticAnalyzer {
    private final List<SemanticDiagnostic> diagnostics = new ArrayList<>();
    private Scope currentScope;
    private int loopDepth;

    public SemanticAnalysisResult analyze(ProgramNode inputProgram) {
        diagnostics.clear();
        currentScope = null;
        loopDepth = 0;

        ProgramNode transformed = analyzeProgram(Objects.requireNonNull(inputProgram, "inputProgram"));
        return new SemanticAnalysisResult(transformed, diagnostics);
    }

    private ProgramNode analyzeProgram(ProgramNode node) {
        BlockNode block = analyzeBlock(node.getBlock(), null);
        return new ProgramNode(node.getName(), block);
    }

    private BlockNode analyzeBlock(BlockNode block, FunctionContext functionContext) {
        Scope previous = currentScope;
        currentScope = new Scope(previous);
        try {
            declareVariables(block.getVariableDeclarations());
            Map<SubroutineDeclaration, CallableSymbol> symbols = declareSubroutineHeaders(block.getSubroutineDeclarations());

            List<SubroutineDeclaration> transformedSubs = new ArrayList<>();
            for (SubroutineDeclaration subroutineDeclaration : block.getSubroutineDeclarations()) {
                transformedSubs.add(analyzeSubroutine(subroutineDeclaration, symbols.get(subroutineDeclaration)));
            }

            CompoundStatement body = (CompoundStatement) analyzeStatement(block.getBody(), functionContext);
            return new BlockNode(block.getVariableDeclarations(), transformedSubs, body);
        } finally {
            currentScope = previous;
        }
    }

    private void declareVariables(List<VariableDeclaration> declarations) {
        for (VariableDeclaration declaration : declarations) {
            SemanticType type = SemanticType.fromTypeNode(declaration.getType());
            if (type.isUnknown()) {
                diagnose("Unknown type in variable declaration: " + declaration.getType().getClass().getSimpleName());
            }

            for (String rawName : declaration.getNames()) {
                String name = normalize(rawName);
                if (currentScope.variables.containsKey(name)) {
                    diagnose("Duplicate variable declaration: " + rawName);
                    continue;
                }
                currentScope.variables.put(name, type);
            }
        }
    }

    private Map<SubroutineDeclaration, CallableSymbol> declareSubroutineHeaders(List<SubroutineDeclaration> declarations) {
        Map<SubroutineDeclaration, CallableSymbol> result = new HashMap<>();
        for (SubroutineDeclaration declaration : declarations) {
            List<SemanticType> paramTypes = new ArrayList<>();
            for (ParameterNode parameter : declaration.getParameters()) {
                paramTypes.add(SemanticType.fromTypeNode(parameter.getType()));
            }

            CallableSymbol symbol;
            if (declaration instanceof FunctionDeclaration functionDeclaration) {
                symbol = CallableSymbol.function(
                        declaration.getName(),
                        paramTypes,
                        SemanticType.fromTypeNode(functionDeclaration.getReturnType()));
            } else {
                symbol = CallableSymbol.procedure(declaration.getName(), paramTypes);
            }

            String key = normalize(declaration.getName());
            if (currentScope.callables.containsKey(key)) {
                diagnose("Duplicate subroutine declaration: " + declaration.getName());
                continue;
            }

            currentScope.callables.put(key, symbol);
            result.put(declaration, symbol);
        }
        return result;
    }

    private SubroutineDeclaration analyzeSubroutine(SubroutineDeclaration node, CallableSymbol symbol) {
        Scope previous = currentScope;
        currentScope = new Scope(previous);
        try {
            for (ParameterNode parameter : node.getParameters()) {
                SemanticType type = SemanticType.fromTypeNode(parameter.getType());
                String key = normalize(parameter.getName());
                if (currentScope.variables.containsKey(key)) {
                    diagnose("Duplicate parameter name: " + parameter.getName() + " in " + node.getName());
                    continue;
                }
                currentScope.variables.put(key, type);
            }

            FunctionContext functionContext = null;
            if (node instanceof FunctionDeclaration functionDeclaration) {
                SemanticType returnType = SemanticType.fromTypeNode(functionDeclaration.getReturnType());
                String functionName = normalize(functionDeclaration.getName());
                currentScope.variables.put(functionName, returnType);
                functionContext = new FunctionContext(functionName, returnType);
            }

            BlockNode transformedBlock = analyzeBlock(node.getBlock(), functionContext);
            if (node instanceof FunctionDeclaration functionDeclaration) {
                return new FunctionDeclaration(
                        functionDeclaration.getName(),
                        functionDeclaration.getParameters(),
                        functionDeclaration.getReturnType(),
                        transformedBlock);
            }

            return new ProcedureDeclaration(node.getName(), node.getParameters(), transformedBlock);
        } finally {
            currentScope = previous;
        }
    }

    private Statement analyzeStatement(Statement statement, FunctionContext functionContext) {
        if (statement instanceof CompoundStatement compoundStatement) {
            List<Statement> transformed = new ArrayList<>();
            for (Statement nested : compoundStatement.getStatements()) {
                transformed.add(analyzeStatement(nested, functionContext));
            }
            return new CompoundStatement(transformed);
        }

        if (statement instanceof AssignmentStatement assignmentStatement) {
            return analyzeAssignment(assignmentStatement, functionContext);
        }

        if (statement instanceof IfStatement ifStatement) {
            TypedExpression condition = analyzeExpression(ifStatement.getCondition());
            Expression boolCondition = ensureType(condition, SemanticType.booleanType(), "if condition");
            Statement thenBranch = analyzeStatement(ifStatement.getThenBranch(), functionContext);
            Statement elseBranch = ifStatement.getElseBranch() == null
                    ? null
                    : analyzeStatement(ifStatement.getElseBranch(), functionContext);
            return new IfStatement(boolCondition, thenBranch, elseBranch);
        }

        if (statement instanceof WhileStatement whileStatement) {
            TypedExpression condition = analyzeExpression(whileStatement.getCondition());
            Expression boolCondition = ensureType(condition, SemanticType.booleanType(), "while condition");

            loopDepth++;
            Statement body;
            try {
                body = analyzeStatement(whileStatement.getBody(), functionContext);
            } finally {
                loopDepth--;
            }
            return new WhileStatement(boolCondition, body);
        }

        if (statement instanceof RepeatUntilStatement repeatUntilStatement) {
            loopDepth++;
            List<Statement> transformedBody = new ArrayList<>();
            try {
                for (Statement nested : repeatUntilStatement.getBody()) {
                    transformedBody.add(analyzeStatement(nested, functionContext));
                }
            } finally {
                loopDepth--;
            }

            TypedExpression condition = analyzeExpression(repeatUntilStatement.getCondition());
            Expression boolCondition = ensureType(condition, SemanticType.booleanType(), "repeat-until condition");
            return new RepeatUntilStatement(transformedBody, boolCondition);
        }

        if (statement instanceof ForStatement forStatement) {
            SemanticType iteratorType = resolveVariableType(forStatement.getVariableName());
            if (!isInteger(iteratorType)) {
                diagnose("For-loop variable must be integer: " + forStatement.getVariableName());
            }

            TypedExpression start = analyzeExpression(forStatement.getStartExpression());
            TypedExpression end = analyzeExpression(forStatement.getEndExpression());

            Expression startExpression = ensureType(start, SemanticType.integerType(), "for start expression");
            Expression endExpression = ensureType(end, SemanticType.integerType(), "for end expression");

            loopDepth++;
            Statement body;
            try {
                body = analyzeStatement(forStatement.getBody(), functionContext);
            } finally {
                loopDepth--;
            }

            return new ForStatement(
                    forStatement.getVariableName(),
                    startExpression,
                    endExpression,
                    forStatement.isDescending(),
                    body);
        }

        if (statement instanceof BreakStatement breakStatement) {
            if (loopDepth <= 0) {
                diagnose("break used outside of loop");
            }
            return breakStatement;
        }

        if (statement instanceof ContinueStatement continueStatement) {
            if (loopDepth <= 0) {
                diagnose("continue used outside of loop");
            }
            return continueStatement;
        }

        if (statement instanceof ProcedureCallStatement procedureCallStatement) {
            return analyzeProcedureCall(procedureCallStatement);
        }

        if (statement instanceof WriteStatement writeStatement) {
            List<Expression> arguments = new ArrayList<>();
            for (Expression argument : writeStatement.getArguments()) {
                arguments.add(analyzeExpression(argument).expression());
            }
            return new WriteStatement(writeStatement.isWriteLine(), arguments);
        }

        if (statement instanceof ReadStatement readStatement) {
            List<Expression> targets = new ArrayList<>();
            for (Expression target : readStatement.getTargets()) {
                TypedExpression typedTarget = analyzeTargetExpression(target, "read target");
                if (typedTarget.type().isArray()) {
                    diagnose("Read target must not be array variable: " + target.getClass().getSimpleName());
                }
                targets.add(typedTarget.expression());
            }
            return new ReadStatement(readStatement.isReadLine(), targets);
        }

        diagnose("Unsupported statement node: " + statement.getClass().getSimpleName());
        return statement;
    }

    private Statement analyzeAssignment(AssignmentStatement node, FunctionContext functionContext) {
        TypedExpression target = analyzeTargetExpression(node.getTarget(), "assignment target");
        TypedExpression value = analyzeExpression(node.getValue());
        Expression convertedValue = ensureType(value, target.type(), "assignment");

        if (functionContext != null
                && node.getTarget() instanceof VariableReferenceExpression variableReference
                && normalize(variableReference.getName()).equals(functionContext.functionName())) {
            convertedValue = ensureType(value, functionContext.returnType(), "function return assignment");
        }

        return new AssignmentStatement(target.expression(), convertedValue);
    }

    private Statement analyzeProcedureCall(ProcedureCallStatement statement) {
        CallableSymbol symbol = resolveCallable(statement.getName());
        if (symbol == null) {
            diagnose("Unknown procedure: " + statement.getName());
            return statement;
        }

        if (symbol.isFunction()) {
            diagnose("Function used as procedure: " + statement.getName());
        }

        List<Expression> transformedArgs = transformArguments(statement.getName(), statement.getArguments(), symbol);
        return new ProcedureCallStatement(statement.getName(), transformedArgs);
    }

    private List<Expression> transformArguments(String callableName, List<Expression> arguments, CallableSymbol symbol) {
        List<Expression> transformed = new ArrayList<>();
        List<SemanticType> parameterTypes = symbol.parameterTypes();

        if (parameterTypes.size() != arguments.size()) {
            diagnose("Wrong argument count for " + callableName + ": expected "
                    + parameterTypes.size() + ", got " + arguments.size());
        }

        int limit = Math.min(parameterTypes.size(), arguments.size());
        for (int index = 0; index < limit; index++) {
            TypedExpression argument = analyzeExpression(arguments.get(index));
            transformed.add(ensureType(argument, parameterTypes.get(index), "argument " + (index + 1) + " of " + callableName));
        }

        for (int index = limit; index < arguments.size(); index++) {
            transformed.add(analyzeExpression(arguments.get(index)).expression());
        }

        return transformed;
    }

    private TypedExpression analyzeExpression(Expression expression) {
        if (expression instanceof LiteralExpression literalExpression) {
            return new TypedExpression(literalExpression, literalType(literalExpression));
        }

        if (expression instanceof VariableReferenceExpression variableReferenceExpression) {
            SemanticType type = resolveVariableType(variableReferenceExpression.getName());
            return new TypedExpression(variableReferenceExpression, type);
        }

        if (expression instanceof ArrayAccessExpression arrayAccessExpression) {
            SemanticType arrayType = resolveVariableType(arrayAccessExpression.getArrayName());
            TypedExpression index = analyzeExpression(arrayAccessExpression.getIndex());
            Expression indexExpr = ensureType(index, SemanticType.integerType(), "array index");

            if (!arrayType.isArray()) {
                diagnose("Variable is not array: " + arrayAccessExpression.getArrayName());
                return new TypedExpression(
                        new ArrayAccessExpression(arrayAccessExpression.getArrayName(), indexExpr),
                        SemanticType.unknownType());
            }

            return new TypedExpression(
                    new ArrayAccessExpression(arrayAccessExpression.getArrayName(), indexExpr),
                    arrayType.getElementType());
        }

        if (expression instanceof UnaryExpression unaryExpression) {
            TypedExpression nested = analyzeExpression(unaryExpression.getExpression());
            UnaryOperator operator = unaryExpression.getOperator();

            return switch (operator) {
                case PLUS, MINUS -> {
                    if (!nested.type().isNumeric()) {
                        diagnose("Unary numeric operator used for non-numeric expression");
                    }
                    yield new TypedExpression(new UnaryExpression(operator, nested.expression()), nested.type());
                }
                case NOT -> {
                    Expression boolExpr = ensureType(nested, SemanticType.booleanType(), "not expression");
                    yield new TypedExpression(new UnaryExpression(operator, boolExpr), SemanticType.booleanType());
                }
            };
        }

        if (expression instanceof CastExpression castExpression) {
            TypedExpression nested = analyzeExpression(castExpression.getExpression());
            SemanticType targetType = SemanticType.fromPrimitiveName(castExpression.getTargetType());
            if (targetType.isUnknown()) {
                diagnose("Unsupported cast target type: " + castExpression.getTargetType());
                return new TypedExpression(castExpression, nested.type());
            }
            return new TypedExpression(new CastExpression(nested.expression(), targetType.toTypeName()), targetType);
        }

        if (expression instanceof BinaryExpression binaryExpression) {
            return analyzeBinaryExpression(binaryExpression);
        }

        if (expression instanceof FunctionCallExpression functionCallExpression) {
            CallableSymbol symbol = resolveCallable(functionCallExpression.getName());
            if (symbol == null) {
                diagnose("Unknown function: " + functionCallExpression.getName());
                List<Expression> args = new ArrayList<>();
                for (Expression argument : functionCallExpression.getArguments()) {
                    args.add(analyzeExpression(argument).expression());
                }
                return new TypedExpression(new FunctionCallExpression(functionCallExpression.getName(), args), SemanticType.unknownType());
            }

            if (!symbol.isFunction()) {
                diagnose("Procedure used as function: " + functionCallExpression.getName());
            }

            List<Expression> transformedArgs = transformArguments(
                    functionCallExpression.getName(),
                    functionCallExpression.getArguments(),
                    symbol);
            return new TypedExpression(
                    new FunctionCallExpression(functionCallExpression.getName(), transformedArgs),
                    symbol.returnType());
        }

        diagnose("Unsupported expression node: " + expression.getClass().getSimpleName());
        return new TypedExpression(expression, SemanticType.unknownType());
    }

    private TypedExpression analyzeBinaryExpression(BinaryExpression binaryExpression) {
        TypedExpression left = analyzeExpression(binaryExpression.getLeft());
        TypedExpression right = analyzeExpression(binaryExpression.getRight());
        BinaryOperator operator = binaryExpression.getOperator();

        if (operator == BinaryOperator.AND || operator == BinaryOperator.OR) {
            Expression leftBool = ensureType(left, SemanticType.booleanType(), operator + " left operand");
            Expression rightBool = ensureType(right, SemanticType.booleanType(), operator + " right operand");
            return new TypedExpression(
                    new BinaryExpression(leftBool, operator, rightBool),
                    SemanticType.booleanType());
        }

        if (operator == BinaryOperator.ADD
                || operator == BinaryOperator.SUBTRACT
                || operator == BinaryOperator.MULTIPLY
                || operator == BinaryOperator.DIVIDE_REAL
                || operator == BinaryOperator.DIVIDE_INT
                || operator == BinaryOperator.MOD) {
            return analyzeArithmeticBinary(binaryExpression, left, right);
        }

        if (operator == BinaryOperator.EQUAL || operator == BinaryOperator.NOT_EQUAL) {
            return analyzeEqualityBinary(operator, left, right);
        }

        return analyzeRelationalBinary(operator, left, right);
    }

    private TypedExpression analyzeArithmeticBinary(
            BinaryExpression expression,
            TypedExpression left,
            TypedExpression right) {
        BinaryOperator operator = expression.getOperator();

        if (operator == BinaryOperator.DIVIDE_INT || operator == BinaryOperator.MOD) {
            Expression castLeft = ensureType(left, SemanticType.integerType(), operator + " left operand");
            Expression castRight = ensureType(right, SemanticType.integerType(), operator + " right operand");
            return new TypedExpression(new BinaryExpression(castLeft, operator, castRight), SemanticType.integerType());
        }

        if (!left.type().isNumeric() || !right.type().isNumeric()) {
            diagnose("Numeric operator " + operator + " requires numeric operands");
            return new TypedExpression(new BinaryExpression(left.expression(), operator, right.expression()), SemanticType.unknownType());
        }

        SemanticType resultType = (operator == BinaryOperator.DIVIDE_REAL
                || left.type().equals(SemanticType.doubleType())
                || right.type().equals(SemanticType.doubleType()))
                        ? SemanticType.doubleType()
                        : SemanticType.integerType();

        Expression convertedLeft = ensureType(left, resultType, operator + " left operand");
        Expression convertedRight = ensureType(right, resultType, operator + " right operand");
        return new TypedExpression(new BinaryExpression(convertedLeft, operator, convertedRight), resultType);
    }

    private TypedExpression analyzeEqualityBinary(BinaryOperator operator, TypedExpression left, TypedExpression right) {
        if (left.type().isNumeric() && right.type().isNumeric()) {
            SemanticType common = (left.type().equals(SemanticType.doubleType()) || right.type().equals(SemanticType.doubleType()))
                    ? SemanticType.doubleType()
                    : SemanticType.integerType();
            Expression convertedLeft = ensureType(left, common, operator + " left operand");
            Expression convertedRight = ensureType(right, common, operator + " right operand");
            return new TypedExpression(
                    new BinaryExpression(convertedLeft, operator, convertedRight),
                    SemanticType.booleanType());
        }

        if (!isComparableSameType(left.type(), right.type())) {
            diagnose("Incompatible types in equality comparison: " + left.type() + " and " + right.type());
        }

        return new TypedExpression(
                new BinaryExpression(left.expression(), operator, right.expression()),
                SemanticType.booleanType());
    }

    private TypedExpression analyzeRelationalBinary(BinaryOperator operator, TypedExpression left, TypedExpression right) {
        if (left.type().isNumeric() && right.type().isNumeric()) {
            SemanticType common = (left.type().equals(SemanticType.doubleType()) || right.type().equals(SemanticType.doubleType()))
                    ? SemanticType.doubleType()
                    : SemanticType.integerType();
            Expression convertedLeft = ensureType(left, common, operator + " left operand");
            Expression convertedRight = ensureType(right, common, operator + " right operand");
            return new TypedExpression(
                    new BinaryExpression(convertedLeft, operator, convertedRight),
                    SemanticType.booleanType());
        }

        if (isStringLike(left.type()) && isStringLike(right.type())) {
            return new TypedExpression(
                    new BinaryExpression(left.expression(), operator, right.expression()),
                    SemanticType.booleanType());
        }

        diagnose("Relational operator " + operator + " requires numeric or string operands");
        return new TypedExpression(
                new BinaryExpression(left.expression(), operator, right.expression()),
                SemanticType.booleanType());
    }

    private TypedExpression analyzeTargetExpression(Expression expression, String context) {
        if (expression instanceof VariableReferenceExpression || expression instanceof ArrayAccessExpression) {
            return analyzeExpression(expression);
        }

        diagnose("Invalid " + context + ": expected variable or array access");
        return new TypedExpression(expression, SemanticType.unknownType());
    }

    private Expression ensureType(TypedExpression expression, SemanticType expectedType, String context) {
        if (isAssignable(expectedType, expression.type())) {
            if (!expectedType.equals(expression.type())) {
                return new CastExpression(expression.expression(), expectedType.toTypeName());
            }
            return expression.expression();
        }

        diagnose("Type mismatch in " + context + ": expected " + expectedType + ", got " + expression.type());
        return expression.expression();
    }

    private boolean isAssignable(SemanticType expected, SemanticType actual) {
        if (expected.isUnknown() || actual.isUnknown()) {
            return true;
        }

        if (expected.equals(actual)) {
            return true;
        }

        if (expected.equals(SemanticType.doubleType()) && actual.equals(SemanticType.integerType())) {
            return true;
        }

        if (expected.equals(SemanticType.stringType()) && actual.equals(SemanticType.charType())) {
            return true;
        }

        return false;
    }

    private boolean isComparableSameType(SemanticType left, SemanticType right) {
        if (left.isUnknown() || right.isUnknown()) {
            return true;
        }
        return left.equals(right);
    }

    private boolean isStringLike(SemanticType type) {
        return type.equals(SemanticType.stringType()) || type.equals(SemanticType.charType());
    }

    private boolean isInteger(SemanticType type) {
        return type.equals(SemanticType.integerType());
    }

    private SemanticType literalType(LiteralExpression literalExpression) {
        LiteralType literalType = literalExpression.getType();
        return switch (literalType) {
            case INTEGER -> SemanticType.integerType();
            case REAL -> SemanticType.doubleType();
            case STRING -> SemanticType.stringType();
            case BOOLEAN -> SemanticType.booleanType();
        };
    }

    private SemanticType resolveVariableType(String rawName) {
        String name = normalize(rawName);
        for (Scope scope = currentScope; scope != null; scope = scope.parent) {
            SemanticType type = scope.variables.get(name);
            if (type != null) {
                return type;
            }
        }

        diagnose("Unknown variable: " + rawName);
        return SemanticType.unknownType();
    }

    private CallableSymbol resolveCallable(String rawName) {
        String name = normalize(rawName);
        for (Scope scope = currentScope; scope != null; scope = scope.parent) {
            CallableSymbol symbol = scope.callables.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    private void diagnose(String message) {
        diagnostics.add(new SemanticDiagnostic(message));
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private record TypedExpression(Expression expression, SemanticType type) {
    }

    private record FunctionContext(String functionName, SemanticType returnType) {
    }

    private static final class Scope {
        private final Scope parent;
        private final Map<String, SemanticType> variables = new HashMap<>();
        private final Map<String, CallableSymbol> callables = new HashMap<>();

        private Scope(Scope parent) {
            this.parent = parent;
        }
    }

    private static final class CallableSymbol {
        private final String name;
        private final List<SemanticType> parameterTypes;
        private final SemanticType returnType;

        private CallableSymbol(String name, List<SemanticType> parameterTypes, SemanticType returnType) {
            this.name = name;
            this.parameterTypes = List.copyOf(parameterTypes);
            this.returnType = returnType;
        }

        static CallableSymbol procedure(String name, List<SemanticType> parameterTypes) {
            return new CallableSymbol(name, parameterTypes, SemanticType.voidType());
        }

        static CallableSymbol function(String name, List<SemanticType> parameterTypes, SemanticType returnType) {
            return new CallableSymbol(name, parameterTypes, returnType);
        }

        boolean isFunction() {
            return returnType.getKind() != SemanticType.Kind.VOID;
        }

        List<SemanticType> parameterTypes() {
            return parameterTypes;
        }

        SemanticType returnType() {
            return returnType;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
