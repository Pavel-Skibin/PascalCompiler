# Тестирование

Проект содержит несколько групп тестов, которые покрывают разные этапы pipeline.

## Синтаксис

1. [SyntaxAnalysisTest.java](../src/test/java/org/nahap/SyntaxAnalysisTest.java)
2. Проверяются валидные программы и синтаксические ошибки.

## Семантика

1. [SemanticAnalysisTest.java](../src/test/java/org/nahap/SemanticAnalysisTest.java)
2. Проверяются типы, области видимости, вызовы и вставка `CastExpression`.

## Оптимизации

1. [OptimizationTest.java](../src/test/java/org/nahap/OptimizationTest.java)
2. Проверяются constant folding и упрощение ветвлений.

## Интерпретация AST

1. [InterpreterPipelineTest.java](../src/test/java/org/nahap/InterpreterPipelineTest.java)
2. Проверяется полный pipeline до AST-интерпретатора.

## Virtual Machine

1. [SimpleVmBackendTest.java](../src/test/java/org/nahap/SimpleVmBackendTest.java)
2. Проверяются арифметика, сравнения, `if`, `while`, `for`, `repeat until`.

## Показательные кейсы

1. [InterpreterShowcaseTest.java](../src/test/java/org/nahap/InterpreterShowcaseTest.java)
2. [AdvancedCompilerCasesTest.java](../src/test/java/org/nahap/AdvancedCompilerCasesTest.java)
3. [SystemFunctionsTest.java](../src/test/java/org/nahap/SystemFunctionsTest.java)

## Тестовые ресурсы

Файлы `.pas` лежат в [src/test/resources](../src/test/resources).

## Статус

На момент последней проверки: 65 тестов, 0 падений.
