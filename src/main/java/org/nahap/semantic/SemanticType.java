package org.nahap.semantic;

import java.util.Locale;
import java.util.Objects;

import org.nahap.ast.type.ArrayTypeNode;
import org.nahap.ast.type.NamedTypeNode;
import org.nahap.ast.type.PrimitiveTypeNode;
import org.nahap.ast.type.TypeNode;

public final class SemanticType {
    public enum Kind {
        INTEGER,
        DOUBLE,
        BOOLEAN,
        STRING,
        CHAR,
        ARRAY,
        UNKNOWN,
        VOID
    }

    private static final SemanticType INTEGER = new SemanticType(Kind.INTEGER, null, 0, 0, "integer");
    private static final SemanticType DOUBLE = new SemanticType(Kind.DOUBLE, null, 0, 0, "double");
    private static final SemanticType BOOLEAN = new SemanticType(Kind.BOOLEAN, null, 0, 0, "boolean");
    private static final SemanticType STRING = new SemanticType(Kind.STRING, null, 0, 0, "string");
    private static final SemanticType CHAR = new SemanticType(Kind.CHAR, null, 0, 0, "char");
    private static final SemanticType UNKNOWN = new SemanticType(Kind.UNKNOWN, null, 0, 0, "unknown");
    private static final SemanticType VOID = new SemanticType(Kind.VOID, null, 0, 0, "void");

    private final Kind kind;
    private final SemanticType elementType;
    private final int lowerBound;
    private final int upperBound;
    private final String displayName;

    private SemanticType(Kind kind, SemanticType elementType, int lowerBound, int upperBound, String displayName) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.elementType = elementType;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    public static SemanticType integerType() {
        return INTEGER;
    }

    public static SemanticType doubleType() {
        return DOUBLE;
    }

    public static SemanticType booleanType() {
        return BOOLEAN;
    }

    public static SemanticType stringType() {
        return STRING;
    }

    public static SemanticType charType() {
        return CHAR;
    }

    public static SemanticType unknownType() {
        return UNKNOWN;
    }

    public static SemanticType voidType() {
        return VOID;
    }

    public static SemanticType arrayType(int lowerBound, int upperBound, SemanticType elementType) {
        return new SemanticType(
                Kind.ARRAY,
                Objects.requireNonNull(elementType, "elementType"),
                lowerBound,
                upperBound,
                "array[" + lowerBound + ".." + upperBound + "] of " + elementType.displayName);
    }

    public static SemanticType fromTypeNode(TypeNode typeNode) {
        if (typeNode instanceof PrimitiveTypeNode primitive) {
            return fromPrimitiveName(primitive.getName());
        }

        if (typeNode instanceof ArrayTypeNode arrayTypeNode) {
            SemanticType element = fromTypeNode(arrayTypeNode.getElementType());
            return arrayType(arrayTypeNode.getLowerBound(), arrayTypeNode.getUpperBound(), element);
        }

        if (typeNode instanceof NamedTypeNode namedTypeNode) {
            return fromPrimitiveName(namedTypeNode.getName());
        }

        return unknownType();
    }

    public static SemanticType fromPrimitiveName(String primitiveName) {
        String normalized = primitiveName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "integer" -> integerType();
            case "double", "real" -> doubleType();
            case "boolean" -> booleanType();
            case "string" -> stringType();
            case "char" -> charType();
            default -> unknownType();
        };
    }

    public Kind getKind() {
        return kind;
    }

    public SemanticType getElementType() {
        return elementType;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public boolean isNumeric() {
        return kind == Kind.INTEGER || kind == Kind.DOUBLE;
    }

    public boolean isBoolean() {
        return kind == Kind.BOOLEAN;
    }

    public boolean isUnknown() {
        return kind == Kind.UNKNOWN;
    }

    public boolean isArray() {
        return kind == Kind.ARRAY;
    }

    public String toTypeName() {
        return switch (kind) {
            case INTEGER -> "integer";
            case DOUBLE -> "double";
            case BOOLEAN -> "boolean";
            case STRING -> "string";
            case CHAR -> "char";
            case ARRAY -> displayName;
            case UNKNOWN -> "unknown";
            case VOID -> "void";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SemanticType other)) {
            return false;
        }
        return kind == other.kind
                && lowerBound == other.lowerBound
                && upperBound == other.upperBound
                && Objects.equals(elementType, other.elementType)
                && Objects.equals(displayName, other.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, elementType, lowerBound, upperBound, displayName);
    }
}
