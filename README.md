# PascalCompiler

Учебный проект по курсу «Теория компиляторов».

Проект реализует компилятор/интерпретатор подмножества Pascal на Java с использованием ANTLR4.
Поддерживаются два варианта исполнения:

1. интерпретатор по AST-дереву;
2. простая виртуальная машина.

## Документация

1. [Входная страница документации](docs/index.md)
2. [Обзор проекта](docs/overview.md)
3. [Язык Pascal: синтаксис и ограничения](docs/language.md)
4. [Архитектура и пайплайн](docs/architecture.md)
5. [Семантический анализ](docs/semantic.md)
6. [Исполнение: AST-интерпретатор и VM](docs/execution.md)
7. [Оптимизации](docs/optimization.md)
8. [Сборка и запуск](docs/build-and-run.md)
9. [Тестирование](docs/testing.md)


## Кратко о проекте

Покрываются этапы:

1. синтаксический анализ и построение AST;
2. семантический анализ и диагностика ошибок;
3. типозависимая трансформация AST;
4. примитивные оптимизации AST;
5. исполнение программы через AST-интерпретатор или VM.

## Технологии

1. Java 17
2. Maven
3. ANTLR 4.13.1
4. JUnit 5

Сборка настроена в [pom.xml](pom.xml).

## Ограничения

1. Проект поддерживает подмножество Pascal, а не полный язык.
2. Интерпретатор по AST остается основным полным backend-вариантом.
3. Простая VM покрывает арифметику, условия и циклы и предназначена для расширения.
4. Нет передачи параметров по ссылке.

## Быстрый запуск

1. Сборка проекта:

- `mvn clean package`

2. Запуск тестов:

- `mvn clean test`

3. Запуск `Main` с AST-интерпретатором:

- `mvn -DskipTests exec:java -Dexec.mainClass=org.nahap.Main -Dexec.args="src/test/resources/valid_basic.pas"`

4. Запуск `Main` с VM:

- `mvn -DskipTests exec:java -Dexec.mainClass=org.nahap.Main -Dexec.args="--vm src/test/resources/valid_control_flow_file.pas"`
