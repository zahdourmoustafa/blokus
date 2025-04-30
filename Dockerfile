# Use Maven image for building
FROM maven:3.9-eclipse-temurin-17 as builder

# Set the working directory
WORKDIR /app

# Copy the POM file
COPY pom.xml .

# Download dependencies (separate layer for caching)
RUN mvn dependency:go-offline -B

# Copy the project source
COPY src ./src

# Package the application
RUN mvn package -DskipTests

# Use a smaller JRE image for the final stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the packaged jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port (default Spring Boot port)
EXPOSE 8080

# Define the entry point
ENTRYPOINT ["java", "-jar", "app.jar"] 