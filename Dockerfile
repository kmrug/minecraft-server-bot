# Use full JDK to ensure Java is available
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app/bot  # ✅ Set working directory to bot/

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy Maven configuration first for caching
COPY bot/pom.xml ./  

# ✅ Download dependencies (inside bot/)
RUN mvn dependency:go-offline

# Now copy the bot's source code (inside bot/)
COPY bot/src ./src

# Move back to root to copy the Minecraft Server files
WORKDIR /app
COPY Server /app/Server  

# ✅ Move back to bot/ to build the bot
WORKDIR /app/bot
RUN mvn clean package  

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

# Run both the bot and the Minecraft server in parallel
CMD ["java", "-jar", "app.jar"]