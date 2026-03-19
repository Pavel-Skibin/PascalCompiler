package org.nahap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.nahap.ast.ProgramNode;
import org.nahap.ast.visitor.AstMermaidPrinter;
import org.nahap.ast.visitor.AstPrinter;
import org.nahap.parser.AstBuilder;
import org.nahap.parser.PascalLexer;
import org.nahap.parser.PascalParser;
import org.nahap.parser.SyntaxErrorListener;
import org.nahap.runtime.PascalInterpreter;

public class Main {
    private static final String AST_MARKDOWN_FILE = "ast.md";

    private static final String PAS_FILE_PATH = "src/test/resources/valid_loops_io.pas";

    private static final String INLINE_PASCAL_CODE =
            """
            program Demo;
            begin
            WriteLn('Hello');
            end.
            """;

    public static void main(String[] args) {
        try {
            CharStream input = resolveInput(args);
            PascalLexer lexer = new PascalLexer(input);
            PascalParser parser = new PascalParser(new CommonTokenStream(lexer));

            SyntaxErrorListener errorListener = new SyntaxErrorListener();
            lexer.removeErrorListeners();
            parser.removeErrorListeners();
            lexer.addErrorListener(errorListener);
            parser.addErrorListener(errorListener);

            PascalParser.ProgramContext parseTree = parser.program();
            if (errorListener.hasErrors()) {
                errorListener.printErrors(System.err);
                System.exit(1);
                return;
            }

            AstBuilder builder = new AstBuilder();
            ProgramNode program = builder.build(parseTree);

            AstPrinter astPrinter = new AstPrinter();
            System.out.print(astPrinter.print(program));

            AstMermaidPrinter mermaidPrinter = new AstMermaidPrinter();
            String markdown = toMarkdown(mermaidPrinter.print(program));
            saveAstMarkdown(markdown);

            PascalInterpreter interpreter = new PascalInterpreter();
            String executionOutput = interpreter.execute(program);

            System.out.println("Program output:");
            if (executionOutput.isEmpty()) {
                System.out.println("(empty)");
            } else {
                System.out.print(executionOutput);
                if (!executionOutput.endsWith(System.lineSeparator())) {
                    System.out.println();
                }
            }
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Cannot read input file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String toMarkdown(String mermaid) {
        String normalized = mermaid.endsWith(System.lineSeparator()) ? mermaid : mermaid + System.lineSeparator();
        return "```mermaid" + System.lineSeparator()
                + normalized
                + "```" + System.lineSeparator();
    }

    private static Path saveAstMarkdown(String markdown) throws IOException {
        Path outputPath = Path.of(AST_MARKDOWN_FILE);
        Files.writeString(outputPath, markdown, StandardCharsets.UTF_8);
        return outputPath;
    }

    private static CharStream resolveInput(String[] args) throws IOException {
        if (args.length > 0) {
            Path argumentPath = resolveExistingPath(args[0]);
            if (argumentPath != null) {
                return CharStreams.fromPath(argumentPath, StandardCharsets.UTF_8);
            }
        }

        Path configuredPath = resolveExistingPath(PAS_FILE_PATH);
        if (configuredPath != null) {
            return CharStreams.fromPath(configuredPath, StandardCharsets.UTF_8);
        }

        if (!INLINE_PASCAL_CODE.isBlank()) {
            return CharStreams.fromString(INLINE_PASCAL_CODE);
        }

        throw new IllegalStateException(
                "No Pascal source found. Current working directory: "
                        + Path.of("").toAbsolutePath().normalize()
                        + ". Set PAS_FILE_PATH or fill INLINE_PASCAL_CODE text block in Main.");
    }

    private static Path resolveExistingPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        for (Path candidate : buildPathCandidates(rawPath)) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<Path> buildPathCandidates(String rawPath) {
        List<Path> candidates = new ArrayList<>();
        Path direct = Path.of(rawPath).normalize();
        addCandidate(candidates, direct);

        if (!direct.isAbsolute()) {
            Path cwd = Path.of("").toAbsolutePath().normalize();
            addCandidate(candidates, cwd.resolve(rawPath).normalize());

            Path projectRoot = findProjectRoot(cwd);
            if (projectRoot != null) {
                addCandidate(candidates, projectRoot.resolve(rawPath).normalize());
            }
        }

        return candidates;
    }

    private static void addCandidate(List<Path> candidates, Path candidate) {
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private static Path findProjectRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}