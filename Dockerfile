# Use full JDK to ensure Java is available
FROM eclipse-temurin:23-jdk AS build

# Set base working directory
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# ✅ Install Playit.gg TCP tunneling agent for Port Forwarding as Railway does not natively support it
RUN curl -o playit-linux.tar.gz https://playit.gg/downloads/playit-linux.tar.gz && \
    tar -xvzf playit-linux.tar.gz && \
    mv playit /usr/local/bin/playit && \
    chmod +x /usr/local/bin/playit

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

# Run both the bot and the Minecraft server in parallel
CMD playit & java -jar app.jar