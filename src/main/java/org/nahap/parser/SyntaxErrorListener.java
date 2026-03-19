package org.nahap.parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public final class SyntaxErrorListener extends BaseErrorListener {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
        String tokenText = "<unknown>";
        if (offendingSymbol instanceof Token token) {
            tokenText = token.getText();
        }
        errors.add("Syntax error at line " + line + ":" + charPositionInLine
                + " - unexpected token '" + tokenText + "'");
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    public void printErrors(PrintStream out) {
        for (String error : errors) {
            out.println(error);
        }
    }
}
