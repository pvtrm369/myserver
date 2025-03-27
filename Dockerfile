# Use an official Java runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy everything from your project directory into the container
COPY . .

# Compile all Java files inside src/ folder
RUN javac -d . src/*.java

# Expose port 8080 so Railway can access it
EXPOSE 8080

# Start the server when the container runs
CMD ["java", "WebServer"]
