package org.nahap.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.nahap.ast.ProgramNode;
import org.nahap.optimize.AstOptimizer;
import org.nahap.parser.AstBuilder;
import org.nahap.parser.PascalLexer;
import org.nahap.parser.PascalParser;
import org.nahap.parser.SyntaxErrorListener;
import org.nahap.semantic.SemanticAnalysisResult;
import org.nahap.semantic.SemanticAnalyzer;

public final class CompilerTestUtils {
    private CompilerTestUtils() {
    }

    public static ParseResult parseString(String code) {
        PascalLexer lexer = new PascalLexer(CharStreams.fromString(code));
        PascalParser parser = new PascalParser(new CommonTokenStream(lexer));

        SyntaxErrorListener listener = new SyntaxErrorListener();
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);

        PascalParser.ProgramContext parseTree = parser.program();
        ProgramNode program = listener.hasErrors() ? null : new AstBuilder().build(parseTree);
        return new ParseResult(program, listener);
    }

    public static ParseResult parseFile(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return parseString(content);
    }

    public static SemanticAnalysisResult semantic(ProgramNode program) {
        return new SemanticAnalyzer().analyze(program);
    }

    public static ProgramNode optimize(ProgramNode program) {
        return new AstOptimizer().optimize(program);
    }

    public record ParseResult(ProgramNode program, SyntaxErrorListener syntaxErrors) {
    }
}
