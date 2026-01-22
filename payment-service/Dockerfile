# Use a lightweight JRE image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR file from the Maven build output
COPY target/payment-service-1.0.0-SNAPSHOT.jar app.jar

# Expose the port the application runs on
EXPOSE 8083

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
