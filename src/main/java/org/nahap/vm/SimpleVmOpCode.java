package org.nahap.vm;

public enum SimpleVmOpCode {
    PUSH_CONST,
    LOAD_VAR,
    STORE_VAR,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE_INT,
    MOD,
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    AND,
    OR,
    NEGATE,
    NOT,
    CAST,
    JUMP,
    JUMP_IF_FALSE,
    PRINT,
    PRINT_LINE
}