package com.kmrug.discordbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

  private final Bot bot;

  public CommandListener(Bot bot) {
    this.bot = bot;
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

    if (event == null || event.getName() == null) {
      Bot.logger.warn("Received null slash command event or null name.");
      return;
    }

    switch (event.getName()) {
      case "startserver":
        event.reply("⏳ Waiting for Minecraft server to start...").queue();
        bot.startMinecraftServer(event);
        break;

      case "stopserver":
        event.reply("🔴 Stopping Minecraft server...").queue();
        bot.stopMinecraftServer("ManualStop");
        break;

      case "restartserver":
        event.reply("🔄 Rebooting...").queue();
        bot.restartMinecraftServer(event);
        break;

      case "serverstatus":
        event.reply("🔍 Checking server status...").queue();
        bot.checkServerStatus(event);
        break;

      case "playercount":
        event.reply("👥 Fetching player count...").queue();
        bot.getPlayerCount(event, false);
        break;

      default:
        event.reply("❌ Unknown command!").queue();
    }
  }
}
