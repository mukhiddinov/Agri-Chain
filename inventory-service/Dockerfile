# Use a lightweight JRE image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR file from the Maven build output
COPY target/inventory-service-1.0.0-SNAPSHOT.jar app.jar

# Expose the ports the application runs on
EXPOSE 8082
EXPOSE 9090

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
