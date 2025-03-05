# Minecraft Discord Bot

A Discord bot that integrates with a Minecraft server to provide management commands like starting, stopping, restarting the server, fetching player count, and displaying server status.

## Features

- ✅ Start and Stop Minecraft server via Discord commands
- ✅ Fetch Player Count
- ✅ Idle Shutdown (Automatic server shutdown if no players are online for a set time)
- ✅ Server Restart Command
- ✅ Resource Monitoring (CPU, Memory usage)
- 🔄 Dockerization (In Progress)
- 🔄 Log4j Integration for logging (Planned)
- 🔄 Port Forwarding for Public Access (Deferred)
- 🔄 Testing with JUnit (Planned)
- 🔄 Spring Boot Integration (Planned)
- 🔄 Deployment on AWS or other Cloud Platforms (Planned)

## Tech Stack

- Java (JDA Library)
- Discord API
- ProcessBuilder for server management
- Docker
- Log4j (Upcoming)
- Spring Boot (Upcoming)
- AWS (Upcoming)

## Getting Started

### Prerequisites
- Java 17
- Maven
- Docker (Optional)
- Discord Bot Token

### Setup Instructions
1. Clone the repository:
```
git clone https://github.com/YourUsername/minecraft-discord-bot.git
cd minecraft-discord-bot
```

2. Create a `.env` file with the following contents:
```
DISCORD_TOKEN=your-bot-token-here
```

3. Build the project with Maven:
```
mvn clean package
```

4. Run the bot:
```
java -jar target/minecraft-discord-bot.jar
```

### Docker Setup (Coming Soon) 

## Author
**Kishan Mrug**

- Connect with me on [LinkedIn](https://www.linkedin.com/in/kishan-mrug/)
- Checkout my [Portfolio](https://kishanmrug.dev/)

## License

This project is licensed under the MIT License.