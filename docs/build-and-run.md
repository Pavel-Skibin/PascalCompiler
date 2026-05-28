# Сборка и запуск

## Требования

1. Java 17.
2. Maven.

## Сборка

```bash
mvn clean package
```

## Тестовый прогон

```bash
mvn clean test
```

## Запуск через AST-интерпретатор

```bash
mvn -DskipTests exec:java -Dexec.mainClass=org.nahap.Main -Dexec.args="src/test/resources/valid_basic.pas"
```

## Запуск через VM

```bash
mvn -DskipTests exec:java -Dexec.mainClass=org.nahap.Main -Dexec.args="--vm src/test/resources/valid_control_flow_file.pas"
```

## Как Main выбирает вход

1. Сначала проверяются аргументы командной строки.
2. Затем используется путь из константы `PAS_FILE_PATH`.
3. Затем fallback на встроенный inline-пример.

Основной класс: [Main.java](../src/main/java/org/nahap/Main.java)
