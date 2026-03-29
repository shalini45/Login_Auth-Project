FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean install -DskipTests

CMD ["java", "-Xmx512m", "-jar", "target/auth-service-1.0.0.jar"]
