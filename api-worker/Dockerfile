# --- STAGE 1: Build the Spring Boot application ---
FROM maven:3.9.6-amazoncorretto-17 AS build
WORKDIR /app

# Cache and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests

# --- STAGE 2: Create the final runtime image ---
FROM amazoncorretto:17-alpine-jdk

# Create a non-root user for security
RUN addgroup -g 1000 appgroup \
 && adduser -u 1000 -G appgroup -D appuser
USER appuser:appgroup

WORKDIR /app

# Copy the fully packaged, executable Spring Boot JAR
COPY --from=build /app/target/*.jar app.jar

# (Optional) Expose default Spring Boot port if you want,
# but not strictly required—ECS will still map whatever port you configure in your Task Definition.
# EXPOSE 8080

# Launch the fat JAR directly; Spring will read server.port from application.properties
ENTRYPOINT ["java", "-jar", "app.jar"]
