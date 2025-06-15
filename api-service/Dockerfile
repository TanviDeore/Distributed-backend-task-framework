# --- STAGE 1: Build the Spring Boot application ---
FROM maven:3.9.6-amazoncorretto-17 AS builder

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source & build
COPY src ./src
RUN mvn clean package -DskipTests

# --- STAGE 2: Create the final, lean runtime image ---
FROM amazoncorretto:17-alpine-jdk

# Create a non-root user
RUN addgroup -g 1000 appgroup \
 && adduser -u 1000 -G appgroup -D appuser

USER appuser:appgroup
WORKDIR /app

# Copy the runnable JAR from the build stage
COPY --from=builder /app/target/*.jar app.jar

# Expose and configure the port your Spring Boot app listens on
ARG SERVER_PORT=8090
ENV SERVER_PORT=${SERVER_PORT}
EXPOSE ${SERVER_PORT}

# (Optional) Activate your prod profile
ENV SPRING_PROFILES_ACTIVE=prod

# Launch the fat JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
