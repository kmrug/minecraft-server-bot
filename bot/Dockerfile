# Base Stage
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy Maven configuration first for caching
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline

# Now copy the rest of the project
COPY src ./src
COPY Server /app/Server

# Build the project
RUN mvn clean package

# Runtime Stage
FROM eclipse-temurin:23-jre
WORKDIR /app
WORKDIR /app/Server

# Copy only the final JAR from the build stage
COPY  --from=build /app/target/minecraft-discord-bot.jar app.jar

# Expose ports (for later networking)
EXPOSE 25565

# 4. Run the Application
CMD [ "java", "-jar", "app.jar" ]