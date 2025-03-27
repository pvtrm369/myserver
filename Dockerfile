# Use OpenJDK 17
FROM openjdk:17

# Set the working directory
WORKDIR /app

# Install `findutils` (if necessary)
RUN apt-get update && apt-get install -y findutils

# Copy source files to /app/src
COPY src/ src/

# Create bin directory for compiled classes
RUN mkdir -p bin

# Compile Java files and output to bin/
RUN find src -name "*.java" | xargs javac -d bin

# Set entry point to start the server
CMD ["java", "-cp", "bin", "WebServer"]
