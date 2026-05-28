package org.nahap.vm;

import java.util.Objects;

import org.nahap.ast.ProgramNode;

public final class SimpleVmExecutor {
    private final SimpleVmCompiler compiler;
    private final SimpleVirtualMachine virtualMachine;

    public SimpleVmExecutor() {
        this(new SimpleVmCompiler(), new SimpleVirtualMachine());
    }

    public SimpleVmExecutor(SimpleVmCompiler compiler, SimpleVirtualMachine virtualMachine) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.virtualMachine = Objects.requireNonNull(virtualMachine, "virtualMachine");
    }

    public String execute(ProgramNode program) {
        SimpleVmProgram bytecode = compiler.compile(program);
        return virtualMachine.execute(bytecode);
    }
}