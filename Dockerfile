# Use full JDK to ensure Java is available
FROM eclipse-temurin:23-jdk AS build

# Set base working directory
WORKDIR /app

# Install required dependencies
RUN apt-get update && apt-get install -y maven

# Copy the entire bot directory to preserve structure
COPY bot /app/bot

# Set working directory to bot/ to run Maven commands
WORKDIR /app/bot

# Download dependencies (inside bot/)
RUN mvn dependency:go-offline

# Build the bot (inside bot/)
RUN mvn clean package

# Move back to /app to prepare runtime
WORKDIR /app

# Copy the Minecraft Server files separately
COPY Server /app/Server

# Use JDK in runtime to ensure Java is available
FROM eclipse-temurin:23-jdk
WORKDIR /app

# Copy only the final JAR from the build stage
COPY --from=build /app/bot/target/minecraft-discord-bot.jar app.jar

# Copy the Minecraft Server folder
COPY --from=build /app/Server /app/Server

# Set working directory for running the bot
WORKDIR /app

# Expose the Minecraft server port
EXPOSE 25565

# Run only the java app, playit is handled by compose.
CMD ["java", "-jar", "app.jar"]