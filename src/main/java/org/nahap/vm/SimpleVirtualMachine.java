package org.nahap.vm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SimpleVirtualMachine {
    public String execute(SimpleVmProgram program) {
        Objects.requireNonNull(program, "program");

        Map<String, Object> variables = initializeVariables(program);
        Deque<Object> stack = new ArrayDeque<>();
        StringBuilder output = new StringBuilder();

        int instructionPointer = 0;
        while (instructionPointer < program.getInstructions().size()) {
            SimpleVmInstruction instruction = program.getInstructions().get(instructionPointer);
            switch (instruction.opcode()) {
                case PUSH_CONST -> stack.push(instruction.operand());
                case LOAD_VAR -> stack.push(getVariable(variables, instruction.operand()));
                case STORE_VAR -> setVariable(variables, instruction.operand(), pop(stack));
                case ADD -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(add(operands[0], operands[1]));
                }
                case SUBTRACT -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(subtract(operands[0], operands[1]));
                }
                case MULTIPLY -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(multiply(operands[0], operands[1]));
                }
                case DIVIDE_INT -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(divide(operands[0], operands[1]));
                }
                case MOD -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(mod(operands[0], operands[1]));
                }
                case EQUAL -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(equalsValue(operands[0], operands[1]));
                }
                case NOT_EQUAL -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(!equalsValue(operands[0], operands[1]));
                }
                case LESS_THAN -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(compare(operands[0], operands[1]) < 0);
                }
                case LESS_OR_EQUAL -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(compare(operands[0], operands[1]) <= 0);
                }
                case GREATER_THAN -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(compare(operands[0], operands[1]) > 0);
                }
                case GREATER_OR_EQUAL -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(compare(operands[0], operands[1]) >= 0);
                }
                case AND -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(toBoolean(operands[0]) && toBoolean(operands[1]));
                }
                case OR -> {
                    Object[] operands = popBinaryOperands(stack);
                    stack.push(toBoolean(operands[0]) || toBoolean(operands[1]));
                }
                case NEGATE -> stack.push(negate(pop(stack)));
                case NOT -> stack.push(!toBoolean(pop(stack)));
                case CAST -> stack.push(cast(pop(stack), String.valueOf(instruction.operand())));
                case JUMP -> {
                    instructionPointer = jumpTarget(program, instruction.operand());
                    continue;
                }
                case JUMP_IF_FALSE -> {
                    if (!toBoolean(pop(stack))) {
                        instructionPointer = jumpTarget(program, instruction.operand());
                        continue;
                    }
                }
                case PRINT -> output.append(stringify(pop(stack)));
                case PRINT_LINE -> output.append(System.lineSeparator());
            }

            instructionPointer++;
        }

        return output.toString();
    }

    private Map<String, Object> initializeVariables(SimpleVmProgram program) {
        Map<String, Object> variables = new HashMap<>();
        for (SimpleVmProgram.VariableDefinition variable : program.getVariables()) {
            variables.put(variable.name(), defaultValue(variable.typeName()));
        }
        return variables;
    }

    private int jumpTarget(SimpleVmProgram program, Object operand) {
        if (operand instanceof Integer target) {
            if (target < 0 || target > program.getInstructions().size()) {
                throw new IllegalStateException("Invalid jump target: " + target);
            }
            return target;
        }

        throw new IllegalStateException("Jump operand must be a resolved target index");
    }

    private Object pop(Deque<Object> stack) {
        Object value = stack.pollFirst();
        if (value == null) {
            throw new IllegalStateException("VM stack underflow");
        }
        return value;
    }

    private Object[] popBinaryOperands(Deque<Object> stack) {
        Object right = pop(stack);
        Object left = pop(stack);
        return new Object[] { left, right };
    }

    private Object getVariable(Map<String, Object> variables, Object operand) {
        String name = normalize(String.valueOf(operand));
        if (!variables.containsKey(name)) {
            throw new IllegalStateException("Unknown variable: " + name);
        }
        return variables.get(name);
    }

    private void setVariable(Map<String, Object> variables, Object operand, Object value) {
        String name = normalize(String.valueOf(operand));
        if (!variables.containsKey(name)) {
            throw new IllegalStateException("Unknown variable: " + name);
        }
        variables.put(name, value);
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

    private Object divide(Object left, Object right) {
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

    private Object negate(Object value) {
        if (value instanceof Double || value instanceof Float) {
            return -toDouble(value);
        }
        return -toLong(value);
    }

    private Object cast(Object value, String targetType) {
        return switch (targetType.toLowerCase(Locale.ROOT)) {
            case "integer" -> toLong(value);
            case "double" -> toDouble(value);
            case "boolean" -> toBoolean(value);
            case "string" -> stringify(value);
            case "char" -> {
                String text = stringify(value);
                yield text.isEmpty() ? "\0" : text.substring(0, 1);
            }
            default -> throw new IllegalStateException("Unsupported cast target: " + targetType);
        };
    }

    private Object defaultValue(String typeName) {
        return switch (typeName.toLowerCase(Locale.ROOT)) {
            case "integer" -> 0L;
            case "double" -> 0.0;
            case "boolean" -> false;
            case "string" -> "";
            case "char" -> "\0";
            default -> throw new IllegalStateException("Unsupported variable type: " + typeName);
        };
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

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}