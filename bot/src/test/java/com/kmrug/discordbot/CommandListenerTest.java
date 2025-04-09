package com.kmrug.discordbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class CommandListenerTest {

  private CommandListener commandListener;
  private Bot mockBot;
  private SlashCommandInteractionEvent mockEvent;
  private ReplyCallbackAction mockReplyAction;

  @BeforeEach
  public void setUp() {
    mockBot = mock(Bot.class);
    mockEvent = mock(SlashCommandInteractionEvent.class);
    mockReplyAction = mock(ReplyCallbackAction.class);

    // Simulate event.reply() returning mock reply action
    when(mockEvent.reply(anyString())).thenReturn(mockReplyAction);

    // When .queue() is called on reply, do nothing (it's void)
    doNothing().when(mockReplyAction).queue();

    commandListener = new CommandListener(mockBot);
  }

  @DisplayName("Should handle /startserver command and call bot.startMinecraftServer()")
  @Test
  public void testStartServerCommand() {

    when(mockEvent.getName()).thenReturn("startserver");

    commandListener.onSlashCommandInteraction(mockEvent);

    // verify that the command for startserver was called
    verify(mockBot).startMinecraftServer(mockEvent);
    verify(mockEvent).reply("⏳ Waiting for Minecraft server to start...");
    verify(mockReplyAction).queue();
  }

  @DisplayName("Should handle /stopserver command and call bot.stopMinecraftServer()")
  @Test
  public void testStopServerCommand() {

    when(mockEvent.getName()).thenReturn("stopserver");

    commandListener.onSlashCommandInteraction(mockEvent);

    // verify that the command for stopserver was called
    verify(mockBot).stopMinecraftServer("ManualStop");
    verify(mockEvent).reply("🔴 Stopping Minecraft server...");
    verify(mockReplyAction).queue();
  }

  @DisplayName("Should handle /restartserver command and call bot.restartMinecraftServer()")
  @Test
  public void testRestartServerCommand() {

    when(mockEvent.getName()).thenReturn("restartserver");

    commandListener.onSlashCommandInteraction(mockEvent);

    // verify that the command for restartserver was called
    verify(mockBot).restartMinecraftServer(mockEvent);
    verify(mockEvent).reply("🔄 Rebooting...");
    verify(mockReplyAction).queue();
  }

  @DisplayName("Should handle /serverstatus command and call bot.checkServerStatus()")
  @Test
  public void testServerStatusCommand() {

    when(mockEvent.getName()).thenReturn("serverstatus");

    commandListener.onSlashCommandInteraction(mockEvent);

    // verify that the command for serverstatus was called
    verify(mockBot).checkServerStatus(mockEvent);
    verify(mockEvent).reply("🔍 Checking server status...");
    verify(mockReplyAction).queue();
  }

  @DisplayName("Should handle /playercount command and call bot.getPlayerCount()")
  @Test
  public void testGetPlayerCount() {

    when(mockEvent.getName()).thenReturn("playercount");

    commandListener.onSlashCommandInteraction(mockEvent);

    // verify that the command for playercount was called
    verify(mockBot).getPlayerCount(mockEvent, false);
    verify(mockEvent).reply("👥 Fetching player count...");
    verify(mockReplyAction).queue();
  }

  @DisplayName("Should handle default command")
  @Test
  public void testDefaultCommand() {

    when(mockEvent.getName()).thenReturn("default");

    commandListener.onSlashCommandInteraction(mockEvent);

    // verify that the command for null was called
    verify(mockEvent).reply("❌ Unknown command!");
    verify(mockReplyAction).queue();
  }

  @Test
  @DisplayName("Should handle NULL command safely")
  public void testNullCommand() {
    when(mockEvent.getName()).thenReturn(null);

    commandListener.onSlashCommandInteraction(mockEvent);
  }
}
