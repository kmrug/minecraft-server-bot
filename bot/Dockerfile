# Use full JDK to ensure Java is available
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy Maven configuration first for caching
COPY pom.xml ./  

# Download dependencies  
RUN mvn dependency:go-offline  

# Now copy the bot's source code
COPY src ./src

# Copy `Server/` from the PARENT DIRECTORY (`../Server`)
COPY ../Server /app/Server  

# Build the bot  
RUN mvn clean package  

# Use JDK in runtime to ensure Java is available
FROM eclipse-temurin:23-jdk
WORKDIR /app

# Copy only the final JAR from the build stage
COPY --from=build /app/target/minecraft-discord-bot.jar app.jar

# Copy the Minecraft Server folder
COPY --from=build /app/Server /app/Server

# Set working directory for running the bot
WORKDIR /app

# Expose the Minecraft server port
EXPOSE 25565

# Run both the bot and the Minecraft server in parallel
CMD ["java", "-jar", "app.jar"]