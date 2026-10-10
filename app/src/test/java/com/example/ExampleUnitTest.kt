package com.example

import com.example.model.ProjectState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FilmCraft video duration calculation, speed multiplier, and trimming.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun projectState_effectiveDuration_matchesVoiceLengthAndTrimming() {
    val initial = ProjectState(
      audioDurationMs = 12500L,
      trimStartMs = 0L,
      trimEndMs = 12500L,
      playbackSpeed = 1.0f
    )
    assertEquals(12500L, initial.effectiveDurationMs)

    // Trimming test
    val trimmed = initial.copy(trimStartMs = 2000L, trimEndMs = 8000L)
    assertEquals(6000L, trimmed.effectiveDurationMs)

    // 2x speed test (duration halves)
    val doubleSpeed = trimmed.copy(playbackSpeed = 2.0f)
    assertEquals(3000L, doubleSpeed.effectiveDurationMs)

    // 0.5x slow-mo test (duration doubles)
    val slowMo = trimmed.copy(playbackSpeed = 0.5f)
    assertEquals(12000L, slowMo.effectiveDurationMs)
  }
}
