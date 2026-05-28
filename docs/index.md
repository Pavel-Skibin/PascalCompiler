# Документация PascalCompiler

Это входная страница документации проекта.

## Разделы

1. [Обзор проекта](overview.md)
2. [Язык Pascal](language.md)
3. [Архитектура и пайплайн](architecture.md)
4. [Семантический анализ](semantic.md)
5. [Исполнение: AST и VM](execution.md)
6. [Оптимизации](optimization.md)
7. [Сборка и запуск](build-and-run.md)
8. [Тестирование](testing.md)


## Кратко

Проект реализует:

1. анализ Pascal-подмножества;
2. семантическую проверку;
3. типозависимую трансформацию AST;
4. оптимизацию AST;
5. два backend-варианта исполнения:
   - AST-интерпретатор;
   - простую виртуальную машину.

Основная точка входа: [../src/main/java/org/nahap/Main.java](../src/main/java/org/nahap/Main.java)
