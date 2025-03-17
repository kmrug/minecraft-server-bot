package com.kmrug.discordbot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import  java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.management.OperatingSystemMXBean;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot extends ListenerAdapter {

  private JDA jda;
  private Process serverProcess = null; // Store Minecraft server status
  public static IdleShutdownManager idleShutdownManager;
  private String channelName;
  String logFileName;

  // Map to store channels by server id (or another identifier)
  public final Map<String, TextChannel> serverChannels = new HashMap<>();

  protected static final Logger logger = LogManager.getLogger(Bot.class);

  public Bot(JDA jda) {
    this.jda = jda;
  }

  public static void main(String[] args) throws Exception {

    String token = System.getenv("DISCORD_TOKEN"); // For Docker

    if (token == null || token.isEmpty()) {
      Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load(); // Loads from .env file (only for local use)
      token = dotenv.get("DISCORD_TOKEN"); // Fallback to local .env file
    }

    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("DISCORD_TOKEN is missing! Set it as an environment variable.");
    }

    JDA jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .setActivity(Activity.playing("Custom minecraft servers"))
        .build()
        .awaitReady();

    logger.info("Discord bot is online!");

    // Create bot instance and pass the JDA instance to it
    Bot botInstance = new Bot(jda);

    // Add event listeners (Slash commands, message listeners, etc.)
    jda.addEventListener(new CommandListener(botInstance));
    jda.upsertCommand("startserver", "Starts the Minecraft server").queue();
    jda.upsertCommand("stopserver", "Stops the Minecraft server").queue();
    jda.upsertCommand("restartserver", "Restarts the Minecraft server").queue();
    jda.upsertCommand("serverstatus", "Checks if the Minecraft server is running").queue();
    jda.upsertCommand("playercount", "Displays the numbers of players online").queue();

    // Start IdleShutdownManager with 60 minute timeout
    idleShutdownManager = new IdleShutdownManager(botInstance, 60);
  }

  public String getLogTimeStamp() {

    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    String timestamp = now.format(formatter);

    // Create file name based on date and time
    String fileName = "log_" + timestamp + ".log";
    return fileName;
  }

  public void startMinecraftServer(SlashCommandInteractionEvent event) {

    // Debug: Log the current directory contents before starting the server
    File serverDir = new File("/app/Server");
    if (!serverDir.exists()) {
      System.out.println("[DEBUG] Server directory does NOT exist: " + serverDir.getAbsolutePath());
    } else {
      System.out.println("[DEBUG] Server directory exists: " + serverDir.getAbsolutePath());
      System.out.println("[DEBUG] Listing contents of Server directory:");
      for (File file : serverDir.listFiles()) {
        System.out.println(" - " + file.getName());
      }
    }

    // Debug: Check if Java is available
    try {
      ProcessBuilder checkJava = new ProcessBuilder("java", "-version");
      checkJava.redirectErrorStream(true);
      Process process = checkJava.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("[DEBUG] Java Check: " + line);
      }
    } catch (IOException e) {
      System.out.println("[DEBUG] Java command not found! " + e.getMessage());
    }

    long startTime = System.currentTimeMillis(); // Start time

    if (serverProcess != null && serverProcess.isAlive()) {
      event.getChannel().sendMessage("⚠️ Minecraft server is already running!").queue();
      logger.warn("Minecraft server is already running!");
      return;
    }

    // Get the channel where the command was executed
    MessageChannel channel = event.getChannel();

    // Ensure the channel is a TextChannel
    if (channel instanceof TextChannel textChannel) {
      channelName = textChannel.getName();
    } else {
      logger.warn("This command can only be used in a text channel!");
      event.getChannel().sendMessage("❌ This command can only be used in a text channel!").queue();
    }

    try {

      // Define server file location
      File serverJar = new File("./Server/server.jar");
      File logFile = new File("./Server/logs/latest.log");

      if (logFile.exists()) {
        logFile.delete();
        logger.info("Deleted old latest.log");
      }

      logFileName = getLogTimeStamp();

      // Start the server process
      ProcessBuilder processBuilder = new ProcessBuilder(
          "java", "-Xmx1024M", "-Xms1024M", "-jar", serverJar.getAbsolutePath(),
          "nogui");

      processBuilder.directory(serverJar.getParentFile()); // set working dir
      processBuilder.redirectOutput(logFile);
      processBuilder.redirectError(logFile);

      // Redirect output to see server logs in bot console
      processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
      processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
      serverProcess = processBuilder.start();

      // Wait for server to be ready for monitoring latest.log
      boolean serverReady = false;

      Pattern serverStarted = Pattern.compile("Done \\((\\d+\\.\\d+)s\\)! For help, type \"help\"");

      while (!serverReady) {
        Thread.sleep(1000);
        if (!logFile.exists()) {
          continue;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
          String line;
          while ((line = reader.readLine()) != null) {
            Matcher matcher = serverStarted.matcher(line);
            if (matcher.find()) {
              serverReady = true;
              long endTime = System.currentTimeMillis(); // End time
              long executionTime = endTime - startTime; // Calculate elapsed time
              double executionTimeInSeconds = executionTime / 1000.0;
              DecimalFormat df = new DecimalFormat("0.00"); // Format to 2 decimal places
              event.getChannel()
                  .sendMessage(
                      "✅ Minecraft server is ready! (Initialized in " + df.format(executionTimeInSeconds)
                          + " seconds.)")
                  .queue();
              break;

            }
          }
        } catch (IOException e) {
          logger.error("[BOT ERROR] Failed to read latest.log: " + e);
        }
      }

      idleShutdownManager.startTimer();

      if (getPlayerCount(event, true) > 0) {
        idleShutdownManager.resetTimer();
      }

    } catch (IOException | InterruptedException e) {
      event.getChannel().sendMessage("❌ Failed to start the Minecraft server: " + e.getMessage()).queue();
      logger.error("[BOT ERROR] Failed to start server: " + e);
    }
  }

  public void stopMinecraftServer(String stopMethod) {

    TextChannel channel = jda.getTextChannelsByName(channelName, true).get(0);
    if (serverProcess == null || !serverProcess.isAlive()) {
      logger.warn("No Minecraft server is currently running!");
      channel.sendMessage("⚠️ No Minecraft server is currently running!").queue();
      return;
    }

    // Path of the latest.log file
    File latestLog = new File("./Server/logs/latest.log");

    try {
      // Send "stop" command to the minecraft server
      OutputStream outputStream = serverProcess.getOutputStream();
      outputStream.write("stop\n".getBytes());
      outputStream.flush();

      // Wait for the server to shut down
      int exitCode = serverProcess.waitFor();

      // Log the shutdown in the bot console
      logger.info("Minecraft server stopped with exit code: " + exitCode);

      if (latestLog.exists()) {
        File renamedLog = new File("./Server/logs/" + logFileName);
        Files.move(latestLog.toPath(), renamedLog.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Log file renamed to: " + renamedLog.getName());
      }

      // Reset process variable
      serverProcess = null;

      if (stopMethod.equals("ManualStop")) {
        String manualStop = "✅ Minecraft server has safely shut down.";
        channel.sendMessage(manualStop).queue();
        idleShutdownManager.stopTimer();
        logger.info("Minecraft server has safely shut down.");
      } else if (stopMethod.equals("IdleStop")) {
        String idle = "❌ Minecraft server was stopped due to inactivity.";
        channel.sendMessage(idle).queue();
        logger.info("Minecraft server was stopped due to inactivity.");
      }

    } catch (IOException e) {
      channel.sendMessage("❌ Failed to stop the Minecraft server due to I/O error.").queue();
      logger.error("[BOT ERROR] I/O Exception while stopping server: " + e);
    } catch (InterruptedException e) {
      channel.sendMessage("❌ Server shutdown process was interrupted.").queue();
      logger.error("[BOT ERROR] InterruptedException while stopping server: " + e);
    } catch (Exception e) {
      channel.sendMessage("❌ Unexpected error while stopping the server.").queue();
      logger.error("[BOT ERROR] Unexpected error: " + e);
    }
  }

  public void restartMinecraftServer(SlashCommandInteractionEvent event) {

    if (serverProcess == null || !serverProcess.isAlive()) {
      event.getChannel().sendMessage("❌ Minecraft server is offline").queue();
      logger.warn("Minecraft server is offline");
      return;
    }

    try {
      // Stop the minecraft server
      stopMinecraftServer("ManualStop");

      // Wait for a few seconds
      Thread.sleep(3000);
      event.getChannel().sendMessage("🛠️ Restarting Minecraft server").queue();
      logger.info("Restarting Minecraft server...");

      // Start server
      startMinecraftServer(event);

    } catch (InterruptedException e) {
      event.getChannel().sendMessage("❌ Server restart process was interrupted.").queue();
      logger.error("[BOT ERROR] InterruptedException while restarting server: " + e);
    }

  }

  public void checkServerStatus(SlashCommandInteractionEvent event) {

    boolean portOpen = isPortOpen("localhost", 25565);
    boolean processRunning = (serverProcess != null && serverProcess.isAlive());

    if (!processRunning) {
      event.getHook().sendMessage("⚠️ Minecraft server is offline").queue();
      logger.warn("Server is offline.");
    }

    else if (portOpen && processRunning) {

      OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

      String archName = osBean.getArch();
      double cpuLoad = osBean.getCpuLoad();
      double systemLoad = osBean.getSystemLoadAverage(); // System CPU load (average over the last minute)
      int processors = osBean.getAvailableProcessors();
      long totalMemory = osBean.getTotalMemorySize() / (1024 * 1024); // Total memory in MB
      long freeMemory = osBean.getFreeMemorySize() / (1024 * 1024); // Free memory in MB
      long usedMemory = totalMemory - freeMemory; // Used RAM in MB

      // Uptime calculation
      RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
      long uptimeMillis = runtimeMXBean.getUptime();
      String uptime = String.format("%02d hours, %02d minutes",
          TimeUnit.MILLISECONDS.toHours(uptimeMillis),
          TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60);

      // Create Embed
      EmbedBuilder embedBuilder = new EmbedBuilder();
      embedBuilder.setTitle("Minecraft Server Status")
          .setDescription("✅ Minecraft server is currently running and accepting connections!")
          .setColor(Color.GREEN)
          .addField("Uptime", uptime, false)
          .addField("Arch Name", archName, false)
          .addField("CPU Load", String.format("%.2f%%", cpuLoad * 100), false)
          .addField("System Load Average", systemLoad == -1.0 ? "N/A" : String.format("%.2f%%", systemLoad * 100),
              false)
          .addField("Processors", String.format("%d", processors), false)
          .addField("Total Memory", String.format("%d MB", totalMemory), true)
          .addField("Used Memory", String.format("%d MB", usedMemory), true)
          .addField("Free Memory", String.format("%d MB", freeMemory), true)
          .setFooter("Keep calm and mine on ⛏️");

      // Send Embed
      event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();

      System.out.println("[BOT] Server is running and online");
      logger.info("Server is running and online");
    }

    else if (processRunning) {
      event.getChannel().sendMessage("⚠️ Minecraft server is running, but the port is closed.").queue();
      logger.error("[BOT WARNING] Server process is running, but port is closed.");
    }

  }

  private boolean isPortOpen(String host, int port) {
    try (Socket socket = new Socket(host, port)) {
      return true; // Successfully connected, port is open
    } catch (IOException e) {
      return false; // Failed to connect, port is closed
    }
  }

  public int getPlayerCount(SlashCommandInteractionEvent event, boolean idleServer) {

    if (serverProcess == null || !serverProcess.isAlive()) {
      event.getChannel().sendMessage("❌ No Minecraft server is currently running!").queue();
      logger.warn("No server is running.");
      return -1;
    }

    File logFile = new File("./Server/logs/latest.log");

    if (!logFile.exists()) {
      event.getChannel().sendMessage("⚠️ Could not find the server log file.").queue();
      logger.error("[BOT ERROR] latest.log not found.");
      return -1;
    }
    int playersOnline = 0;
    try {

      // **Step 1: Send "list" command to force log update**
      OutputStream outputStream = serverProcess.getOutputStream();
      outputStream.write("list\n".getBytes());
      outputStream.flush(); // Forces Minecraft to update `latest.log`
      Thread.sleep(1500);

      // **Step 2: Read latest.log to get player count**
      BufferedReader reader = new BufferedReader(new FileReader(logFile));
      String line;
      String latestPlayerCount = "⚠️ Player count not found in logs.";

      Pattern playerCountPattern = Pattern.compile("There are (\\d+) of a max of (\\d+) players online");

      while ((line = reader.readLine()) != null) {

        Matcher matcher = playerCountPattern.matcher(line);

        if (matcher.find()) {
          playersOnline = Integer.parseInt(matcher.group(1));
          int maxPlayer = Integer.parseInt(matcher.group(2));
          latestPlayerCount = "📊 Players Online: " + playersOnline + "/" + maxPlayer;
        }
      }

      reader.close();
      if (!idleServer) {
        event.getChannel().sendMessage(latestPlayerCount).queue();
        logger.info(latestPlayerCount);
      }

    } catch (IOException | InterruptedException | NumberFormatException e) {
      event.getChannel().sendMessage("❌ Failed to read server logs.").queue();
      logger.error("[BOT ERROR] Failed to read latest.log: " + e);
    }
    return playersOnline;
  }
}
