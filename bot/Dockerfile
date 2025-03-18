# Use full JDK to ensure Java is available
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

# ✅ Copy the Server directory (now correctly inside bot/)  
COPY Server /app/Server  

# ✅ Now, set execution permissions after copying Server files
RUN chmod +x /app/Server/server.jar
RUN chmod -R 777 /app/Server

# Build the project  
RUN mvn clean package  

# ✅ Use JDK in runtime to ensure Java is available
FROM eclipse-temurin:23-jdk
WORKDIR /app

# Copy only the final JAR from the build stage
COPY --from=build /app/target/minecraft-discord-bot.jar app.jar

# ✅ Copy the Minecraft Server folder
COPY --from=build /app/Server /app/Server

# ✅ Ensure Java has execution permissions for the Minecraft server
RUN chmod +x /app/Server/server.jar
RUN chmod -R 777 /app/Server

# ✅ Set working directory for running Minecraft server
WORKDIR /app/Server

# ✅ Expose the Minecraft server port
EXPOSE 25565

# ✅ Run the Minecraft Server
CMD ["java", "-jar", "server.jar"]
