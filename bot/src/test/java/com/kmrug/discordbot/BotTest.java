package com.kmrug.discordbot;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

    bot.startMinecraftServer(mockEvent);
  }

  @Test
  public void testIsNotTextChannel() {

    when(mockEvent.getChannel()).thenReturn(mockChannelUnion);
    when(mockChannelUnion.getType()).thenReturn(ChannelType.PRIVATE);

    when(mockChannelUnion.sendMessage(anyString())).thenReturn(mockAction);
    doNothing().when(mockAction).queue();

    bot.startMinecraftServer(mockEvent);

    verify(mockChannelUnion).sendMessage("❌ This command can only be used in a text channel!");
    // verify(mockAction).queue();
  }

  @Test
  public void testLogFileExists() {

    File mockFile = mock(File.class);

    when(mockFile.getPath()).thenReturn("./Server/logs/latest.log");
    when(mockFile.exists()).thenReturn(true);

    // TODO: Refactor getLogFile() to allow mocking and verifying log deletion

    bot.startMinecraftServer(mockEvent);
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

  // Note: This section tests the CheckServerStatus functino

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

}
