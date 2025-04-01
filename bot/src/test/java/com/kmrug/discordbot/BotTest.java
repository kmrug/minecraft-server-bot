package com.kmrug.discordbot;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class BotTest {

  private Process mockProcess;
  private SlashCommandInteractionEvent mockEvent;
  private Bot bot;
  private MessageCreateAction mockAction;
  private MessageChannelUnion mockChannelUnion;
  private TextChannel mockTextChannel;

  @BeforeEach
  public void setUp() throws Exception {

    mockEvent = mock(SlashCommandInteractionEvent.class);
    mockProcess = mock(Process.class);
    mockAction = mock(MessageCreateAction.class);
    mockChannelUnion = mock(MessageChannelUnion.class);
    mockTextChannel = mock(TextChannel.class);

    // Real bot with a dummy JDA
    JDA mockJDA = mock(JDA.class);
    bot = new Bot(mockJDA);

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.sendMessage(anyString())).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    Field processField = Bot.class.getDeclaredField("serverProcess");
    processField.setAccessible(true);
    processField.set(bot, mockProcess);
  }

  // Note: This section tests the StartMinecraftServer function

  @Test
  public void testServerProcess() {

    when(mockProcess.isAlive()).thenReturn(true);
    bot.startMinecraftServer(mockEvent);

    verify(mockChannelUnion).sendMessage("⚠️ Minecraft server is already running!");
    verify(mockAction).queue();

  }

  @Test
  public void testIsTextChannel() {

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.getType()).thenReturn(ChannelType.TEXT);
    when(mockChannelUnion.asTextChannel()).thenReturn(mockTextChannel);
    when(mockTextChannel.getName()).thenReturn("test-channel");

    boolean result = bot.isValidTextChannel(mockChannelUnion);

    assertTrue(result);
    assertEquals("test-channel", bot.channelName);
  }

  @Test
  public void testIsNotTextChannel() {

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.getType()).thenReturn(ChannelType.PRIVATE);

    when(mockChannelUnion.sendMessage(anyString())).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    boolean result = bot.isValidTextChannel(mockChannelUnion);

    assertFalse(result);
    verify(mockChannelUnion).sendMessage("❌ This command can only be used in a text channel!");
    verify(mockAction).queue();
  }

  @Test
  public void getCorrectDirectory_whenFilesExist_shouldReturnCorrectPaths() {
    File testBaseDir = tempDir.toPath().resolve("Server").toFile();
    File logsDir = new File(testBaseDir, "logs");
    logsDir.mkdirs();

    File fakeJar = new File(testBaseDir, "server.jar");
    File fakeLog = new File(logsDir, "latest.log");

    try {
      fakeJar.createNewFile();
      fakeLog.createNewFile();
    } catch (IOException e) {
      fail("Test setup failed");
    }

    // SSpy and override base path
    Bot spyBot = spy(new Bot(null));
    doReturn(testBaseDir.getAbsolutePath()).when(spyBot).getServerBasePath();

    File[] files = spyBot.getCorrectDirectory();
    File logFile = files[0];
    File serverJar = files[1];

    assertTrue(logFile.exists(), "Expected mock latest.log to exist");
    assertTrue(serverJar.exists(), "Expected mock server.jar to exist");
  }

  @Test
  public void getCorrectDirectory_whenFilesMissing_shouldLogAndStillReturn() {
    File testBaseDir = new File(tempDir, "MissingServer");
    File logsDir = new File(testBaseDir, "logs");
    logsDir.mkdirs();

    File logFile = new File(logsDir, "latest.log");
    File serverJar = new File(testBaseDir, "server.jar");

    assertFalse(logFile.exists(), "Expected latest.log to not exist");
    assertFalse(serverJar.exists(), "Expected server.jar to not exist");

    String basePath = testBaseDir.getAbsolutePath();

    File expectedLogFile = new File(basePath + "/logs/latest.log");
    File expectedServerJar = new File(basePath + "/server.jar");

    assertEquals(expectedLogFile.getAbsolutePath(), logFile.getAbsolutePath());
    assertEquals(expectedServerJar.getAbsolutePath(), serverJar.getAbsolutePath());
  }

  @Test
  public void testLogFileExists() {

    // File mockFile = mock(File.class);

    // when(mockFile.getPath()).thenReturn("./Server/logs/latest.log");
    // when(mockFile.exists()).thenReturn(true);

    // // TODO: Refactor getLogFile() to allow mocking and verifying log deletion

    // bot.startMinecraftServer(mockEvent);
  }

  @Test
  public void testNoLogFileExists() {

    // TODO: Test log file deletion and fallback behavior after refactoring
    // getLogFile() helper

  }

  @Test
  public void testServerEnvironment() {
    // TODO: Extract log readiness monitoring into testable class for better unit
    // testing

  }

  @Test
  public void testGetLogTimestamp() {

    Bot bot = new Bot(null); // we don't need a read JDA instance

    String timestamp = bot.getLogTimeStamp();

    assertNotNull(timestamp, "Timestamp should not be null");

    // Match pattern: log_YYYY-MM-DD_HH-MM-SS.log
    String expectedPattern = "log_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.log";
    assertTrue(timestamp.matches(expectedPattern), "Timestamp format was invlid. Got: " + timestamp);
  }

  @Test
  public void testIdleStartTimer() {

    // TODO: Cannot be done without refactoring ProcessBuilder entirely

    // IdleShutdownManager mockIdleManager = mock(IdleShutdownManager.class);

    // Bot.idleShutdownManager = mockIdleManager;

    // bot.startMinecraftServer(mockEvent);

    // verify(Bot.idleShutdownManager).startTimer();

    // when(bot.getPlayerCount(mockEvent, true)).thenReturn(2);

    // verify(Bot.idleShutdownManager).resetTimer();
  }

  // Note: This section tests the CheckServerStatus function

  @Test
  public void testIsProcessNotRunning() {

    Bot spyBot = spy(new Bot(null));

    doReturn(false).when(spyBot).isPortOpen("localhost", 25565);

    when(mockProcess.isAlive()).thenReturn(true);
    spyBot.serverProcess = mockProcess;

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.sendMessage("⚠️ Minecraft server is running, but the port is closed."))
        .thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    spyBot.checkServerStatus(mockEvent);

    verify(mockChannelUnion).sendMessage("⚠️ Minecraft server is running, but the port is closed.");
    verify(mockAction).queue();
  }

  @Test
  public void testIsPrintingOSInfo() {

    Bot spyBot = spy(new Bot(null));

    doReturn(true).when(spyBot).isPortOpen("localhost", 25565);

    when(mockProcess.isAlive()).thenReturn(true);
    spyBot.serverProcess = mockProcess;

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.sendMessageEmbeds(any(MessageEmbed.class)))
        .thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    spyBot.checkServerStatus(mockEvent);

    verify(mockChannelUnion).sendMessageEmbeds(any(MessageEmbed.class));
    verify(mockAction).queue();
  }

  // Note: This section tests the GetPlayerCount function

  @Test
  public void testIsProcessAlive() {

    when(mockProcess.isAlive()).thenReturn(false);

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.sendMessage("❌ No Minecraft server is currently running!")).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    int result = bot.getPlayerCount(mockEvent, true); // MIGHT HAVE TO CHANGE TRUE TO FALSE LATER
    assertEquals(-1, result);

    verify(mockChannelUnion).sendMessage("❌ No Minecraft server is currently running!");
    verify(mockAction).queue();

  }

  @Test
  public void testLogFileExistsInPlayerCount() {

    File mockFile = mock(File.class);

    when(mockFile.getPath()).thenReturn("./Server/logs/latest.log");
    when(mockFile.exists()).thenReturn(true);

    // TODO: Refactor getLogFile() to allow mocking and verifying log deletion

    int result = bot.getPlayerCount(mockEvent, true);
    assertEquals(-1, result);
  }

  @TempDir
  File tempDir;

  @Test
  public void testCorrectPlayerCount() {
    Bot spyBot = spy(new Bot(null));
    OutputStream mockStream = mock(OutputStream.class);
    doReturn(true).when(spyBot).isPortOpen("localhost", 25565);

    // Create a mock latest.log file in temp dir
    Path testLogPath = tempDir.toPath().resolve("logs/latest.log");
    try {
      Files.createDirectories(testLogPath.getParent());
      Files.write(testLogPath, List.of("There are 5 of a max of 10 players online"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      fail("Test setup failed to write mock log file");
    }

    // Create a dummy server.jar to complete getCorrectDirectory()
    File dummyServerJar = tempDir.toPath().resolve("server.jar").toFile();
    try {
      dummyServerJar.createNewFile();
    } catch (IOException e) {
      fail("Test setup failed to create dummy server.jar");
    }

    // Override getCorrectDirectory to return our temp paths
    doReturn(new File[] {
        testLogPath.toFile(),
        dummyServerJar
    }).when(spyBot).getCorrectDirectory();

    spyBot.serverProcess = mockProcess;

    when(mockProcess.isAlive()).thenReturn(true);
    when(mockProcess.getOutputStream()).thenReturn(mockStream);
    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.sendMessage("📊 Players Online: 5/10")).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    int result = spyBot.getPlayerCount(mockEvent, false);

    assertEquals(5, result);
    verify(mockChannelUnion).sendMessage("📊 Players Online: 5/10");
    verify(mockAction).queue();
  }

  // Note: This section tests the RestartMinecraftServer function

  @Test
  public void testIsProcessAliveInRestart() {

    Bot spyBot = spy(new Bot(null));

    when(mockProcess.isAlive()).thenReturn(false);
    when(mockChannelUnion.sendMessage("❌ Minecraft server is offline")).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    spyBot.restartMinecraftServer(mockEvent);

    verify(spyBot, never()).stopMinecraftServer("ManualStop");
    verify(spyBot, never()).startMinecraftServer(mockEvent);
    verify(mockChannelUnion).sendMessage("❌ Minecraft server is offline");
    verify(mockAction).queue();
  }

  @Test
  public void testCallToRestartServer() {

    Bot spyBot = spy(new Bot(null));

    spyBot.serverProcess = mockProcess;

    when(mockProcess.isAlive()).thenReturn(true);
    doReturn(true).when(spyBot).isPortOpen("localhost", 25565);
    when(mockChannelUnion.sendMessage("🛠️ Restarting Minecraft server")).thenReturn(mockAction);
    doNothing().when(mockAction).queue();
    doNothing().when(spyBot).stopMinecraftServer("ManualStop");
    doNothing().when(spyBot).startMinecraftServer(mockEvent);

    spyBot.restartMinecraftServer(mockEvent);

    verify(mockChannelUnion).sendMessage("🛠️ Restarting Minecraft server");
    verify(mockAction).queue();

    verify(spyBot).stopMinecraftServer("ManualStop");
    verify(spyBot).startMinecraftServer(mockEvent);
  }

  // Note: This section tests the StopMinecraftServer function

  @Test
  public void testIsProcessAliveInStop() {

    JDA mockJDA = mock(JDA.class);
    Bot spyBot = spy(new Bot(mockJDA));
    spyBot.serverProcess = mockProcess;
    spyBot.channelName = "test-channel";
    TextChannel mockChannel = mock(TextChannel.class);

    when(mockJDA.getTextChannelsByName("test-channel", true)).thenReturn(List.of(mockChannel));
    when(mockProcess.isAlive()).thenReturn(false);
    when(mockChannel.sendMessage("⚠️ No Minecraft server is currently running!")).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    spyBot.stopMinecraftServer("ManualStop");

    verify(mockChannel).sendMessage("⚠️ No Minecraft server is currently running!");
    verify(mockAction).queue();
  }

  @Test
  public void testLatestLogExists() {

    JDA mockJDA = mock(JDA.class);
    Bot spyBot = spy(new Bot(mockJDA));
    OutputStream mockStream = mock(OutputStream.class);
    spyBot.serverProcess = mockProcess;
    spyBot.channelName = "test-channel";
    TextChannel mockChannel = mock(TextChannel.class);

    try {
      when(mockProcess.isAlive()).thenReturn(true);
      when(mockJDA.getTextChannelsByName("test-channel", true)).thenReturn(List.of(mockChannel));
      File mockLogFile = mock(File.class);
      // when(spyBot.resolvePathWithFallback(anyString(),
      // anyString())).thenReturn(mockLogFile);
      when(mockLogFile.exists()).thenReturn(false);
      when(mockProcess.getOutputStream()).thenReturn(mockStream);
      when(mockProcess.waitFor()).thenReturn(0);
      when(mockChannel.sendMessage("Invalid stop message was sent")).thenReturn(mockAction);
      doNothing().when(mockAction).queue();
    } catch (InterruptedException e) {
      System.err.println("InterruptedException has occured when trying to TestLatestLogExists()");
    }

    spyBot.stopMinecraftServer("TestSafeShutdownServer");

    assertNull(spyBot.serverProcess);
    verify(mockChannel).sendMessage("Invalid stop message was sent");
    verify(mockAction).queue();
  }

  @Test
  public void testManualStopServer() {

    JDA mockJDA = mock(JDA.class);
    Bot spyBot = spy(new Bot(mockJDA));
    IdleShutdownManager mockIdle = mock(IdleShutdownManager.class);
    OutputStream mockStream = mock(OutputStream.class);
    spyBot.serverProcess = mockProcess;
    spyBot.channelName = "test-channel";
    Bot.idleShutdownManager = mockIdle;
    TextChannel mockChannel = mock(TextChannel.class);

    try {
      when(mockProcess.isAlive()).thenReturn(true);
      when(mockJDA.getTextChannelsByName("test-channel", true)).thenReturn(List.of(mockChannel));
      File mockLogFile = mock(File.class);
      // when(spyBot.resolvePathWithFallback(anyString(),
      // anyString())).thenReturn(mockLogFile);
      when(mockLogFile.exists()).thenReturn(false);
      when(mockProcess.getOutputStream()).thenReturn(mockStream);
      when(mockProcess.waitFor()).thenReturn(0);
      when(mockChannel.sendMessage("✅ Minecraft server has safely shut down.")).thenReturn(mockAction);
      doNothing().when(mockAction).queue();
    } catch (InterruptedException e) {
      System.err.println("InterruptedException has occured when trying to TestLatestLogExists()");
    }
    spyBot.stopMinecraftServer("ManualStop");

    verify(mockChannel).sendMessage("✅ Minecraft server has safely shut down.");
    verify(mockAction).queue();
    verify(mockIdle).stopTimer();
    assertNull(spyBot.serverProcess);

  }

  @Test
  public void testIdleStopServer() {

    JDA mockJDA = mock(JDA.class);
    Bot spyBot = spy(new Bot(mockJDA));
    OutputStream mockStream = mock(OutputStream.class);
    spyBot.serverProcess = mockProcess;
    spyBot.channelName = "test-channel";
    TextChannel mockChannel = mock(TextChannel.class);

    try {
      when(mockProcess.isAlive()).thenReturn(true);
      when(mockJDA.getTextChannelsByName("test-channel", true)).thenReturn(List.of(mockChannel));
      File mockLogFile = mock(File.class);
      // when(spyBot.resolvePathWithFallback(anyString(),
      // anyString())).thenReturn(mockLogFile);
      when(mockLogFile.exists()).thenReturn(false);
      when(mockProcess.getOutputStream()).thenReturn(mockStream);
      when(mockProcess.waitFor()).thenReturn(0);
      when(mockChannel.sendMessage("❌ Minecraft server was stopped due to inactivity.")).thenReturn(mockAction);
      doNothing().when(mockAction).queue();
    } catch (InterruptedException e) {
      System.err.println("InterruptedException has occured when trying to TestLatestLogExists()");

      spyBot.stopMinecraftServer("IdleStop");

      assertNull(spyBot.serverProcess);
      verify(mockChannel).sendMessage("❌ Minecraft server was stopped due to inactivity.");
      verify(mockAction).queue();
    }
  }
}
