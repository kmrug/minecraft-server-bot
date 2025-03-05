package com.kmrug.discordbot;

import java.util.Timer;
import java.util.TimerTask;

public class IdleShutdownManager {
  
  private final long timeoutMinutes;
  private Timer timer;

  private final Bot botInstance;

  public IdleShutdownManager(Bot botInstance, long timeoutMinutes) {
    this.timeoutMinutes = timeoutMinutes;
    this.timer = new Timer(true);
    this.botInstance = botInstance;
  }

  public void startTimer() {
    System.out.println("[BOT] Idle timer started...");
    timer.cancel();
    timer = new Timer(true);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        System.out.println("[BOT] No players detected for " + timeoutMinutes + " minute(s). Stopping server...");
        botInstance.stopMinecraftServer("IdleStop");
      }
    }, timeoutMinutes * 60 * 1000);
  }

  public void resetTimer() {
    System.out.println("[BOT] Player activity detected! Resetting idle timer...");
    startTimer();
  }

  public void stopTimer() {
    timer.cancel();
  }
}
