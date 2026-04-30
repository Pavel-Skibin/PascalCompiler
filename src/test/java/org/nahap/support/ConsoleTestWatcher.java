package org.nahap.support;

import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public final class ConsoleTestWatcher implements BeforeTestExecutionCallback, TestWatcher {
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        System.out.println("[TEST][START] " + displayName(context));
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.println("[TEST][PASS]  " + displayName(context));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String message = cause == null ? "unknown error" : safeMessage(cause);
        System.out.println("[TEST][FAIL]  " + displayName(context) + " -> " + message);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String message = cause == null ? "aborted" : safeMessage(cause);
        System.out.println("[TEST][ABORT] " + displayName(context) + " -> " + message);
    }

    private static String displayName(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "." + context.getDisplayName();
    }

    private static String safeMessage(Throwable cause) {
        String text = Optional.ofNullable(cause.getMessage()).orElse(cause.getClass().getSimpleName());
        text = text.replace(System.lineSeparator(), " | ");
        return text.length() > 240 ? text.substring(0, 240) + "..." : text;
    }
}
