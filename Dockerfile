# Use an official Java runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy all project files
COPY . .

# Compile the Java files (Modify for Gradle/Maven)
RUN javac -d . src/*.java

# Expose the application port
EXPOSE 8080

# Command to run the server
CMD ["java", "WebServer"]