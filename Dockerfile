# --- Этап 1: Сборка приложения через Maven ---
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

# Указываем рабочую папку внутри контейнера
WORKDIR /app

# Копируем настройки зависимостей (pom.xml)
COPY pom.xml .
COPY checkstyle.xml .

# Скачиваем все библиотеки заранее (для кеширования слоев Docker)
RUN mvn dependency:go-offline -B

# Копируем исходный код нашего приложения
COPY src ./src

# Компилируем проект и собираем JAR-файл, пропуская тесты (так как мы их уже гоняли в IDE)
RUN mvn clean package

# --- Этап 2: Запуск готового приложения ---
FROM eclipse-temurin:25-alpine

WORKDIR /app

# Копируем собранный JAR-файл из первого этапа (builder) в наш чистый контейнер запуска
COPY --from=builder /app/target/*.jar app.jar

# Указываем порт, который приложение будет слушать
EXPOSE 8080

# Команда для запуска нашего тайм-трекера
ENTRYPOINT ["java", "-jar", "app.jar"]