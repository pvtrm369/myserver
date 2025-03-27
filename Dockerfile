# Use OpenJDK 17 (or your preferred version)
FROM openjdk:17

# Set the working directory
WORKDIR /app

# Copy all source code
COPY . .

# Compile Java files
RUN javac -d . $(find . -name "*.java")

# Set entry point to start the server
CMD ["java", "WebServer"]
