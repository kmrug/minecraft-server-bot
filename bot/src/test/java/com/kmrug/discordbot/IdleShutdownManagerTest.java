package com.kmrug.discordbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IdleShutdownManagerTest {

  private IdleShutdownManager idleShutdownManager;
  private Bot mockBot;

  @BeforeEach
  public void setUp() {
    mockBot = mock(Bot.class); // this is Mockito
    idleShutdownManager = new IdleShutdownManager(mockBot, 1); // 1 min timeout
  }

  @Test
  public void testStartTimer() {
    idleShutdownManager.startTimer();
  }

  @Test
  public void testResetTimer() {
    idleShutdownManager.resetTimer();
  }

  @Test
  public void testStopTimerDoesNotCrash() {
    idleShutdownManager.stopTimer();
  }

  @Test
  public void testStopIsCalledAfterTimeout() throws InterruptedException {
    idleShutdownManager = new IdleShutdownManager(mockBot, 0);
    idleShutdownManager.startTimer();

    // Give time for task to run
    Thread.sleep(500);

    // verify id stopMinecraftServer was called
    verify(mockBot, times(1)).stopMinecraftServer("IdleStop");
  }
  
}
