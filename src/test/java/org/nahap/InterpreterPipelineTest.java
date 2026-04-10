package org.nahap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.nahap.runtime.PascalInterpreter;
import org.nahap.support.CompilerTestUtils;

class InterpreterPipelineTest {
    @Test
    void fullPipelineExecutesValidBasicProgram() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_basic.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("result: 32"));
    }

    @Test
    void fullPipelineExecutesNestedCallsProgram() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_nested.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("inner=6"));
    }

    @Test
    void semanticErrorsBlockExecutionInPipeline() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/invalid_semantic.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertTrue(semantic.hasErrors());
    }

    @Test
    void fullPipelineExecutesRecursionProgramFromFile() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_recursion_factorial_file.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("fact=720"));
    }

    @Test
    void fullPipelineExecutesControlFlowProgramFromFile() throws Exception {
        var parsed = CompilerTestUtils.parseFile(Path.of("src/test/resources/valid_control_flow_file.pas"));
        assertFalse(parsed.syntaxErrors().hasErrors());

        var semantic = CompilerTestUtils.semantic(parsed.program());
        assertFalse(semantic.hasErrors());

        var optimized = CompilerTestUtils.optimize(semantic.getProgram());
        String output = new PascalInterpreter().execute(optimized);

        assertTrue(output.contains("ok=20"));
    }
}
