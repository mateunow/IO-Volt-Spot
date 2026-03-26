FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]