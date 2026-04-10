package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.nahap.support.CompilerTestUtils;

class SyntaxAnalysisTest {
    @Test
    void validProgramHasNoSyntaxErrors() throws Exception {
        var result = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_basic.pas"));
        assertFalse(result.syntaxErrors().hasErrors());
        assertNotNull(result.program());
    }

    @Test
    void invalidProgramHasSyntaxErrors() throws Exception {
        var result = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_missing_end.pas"));
        assertTrue(result.syntaxErrors().hasErrors());
        assertTrue(result.syntaxErrors().getErrors().get(0).contains("Syntax error"));
    }

    @Test
    void parserHandlesNestedSubroutinesSyntax() throws Exception {
        var result = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_nested.pas"));
        assertFalse(result.syntaxErrors().hasErrors());
        assertNotNull(result.program());
    }
}
