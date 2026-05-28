# Обзор проекта

PascalCompiler — учебный проект по реализации компилятора/интерпретатора подмножества Pascal на Java.

## Цель

Проект демонстрирует полный минимальный pipeline обработки программы:

1. синтаксический разбор;
2. построение AST;
3. семантический анализ;
4. типозависимую трансформацию AST;
5. примитивные оптимизации;
6. исполнение программы.

## Реализованные варианты исполнения

1. AST-интерпретатор.
2. Простая виртуальная машина.

## Основные сущности

1. [AST-узлы](../src/main/java/org/nahap/ast)
2. [Visitors](../src/main/java/org/nahap/ast/visitor)
3. [Парсер и AstBuilder](../src/main/java/org/nahap/parser)
4. [Семантика](../src/main/java/org/nahap/semantic)
5. [Оптимизация](../src/main/java/org/nahap/optimize)
6. [AST-интерпретатор](../src/main/java/org/nahap/runtime/PascalInterpreter.java)
7. [Simple VM](../src/main/java/org/nahap/vm)

## Роль Main

Класс [Main](../src/main/java/org/nahap/Main.java) выполняет полный pipeline и выбирает backend исполнения.
