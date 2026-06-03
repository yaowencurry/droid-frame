package com.example.screenshotframer.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class FramerUiStateTest {
    @Test
    fun resetForNewImportClearsBackgroundImageAndLatestExport() {
        val dirty = FramerUiState(
            backgroundMode = BackgroundMode.Image,
            latestExport = File("old.png"),
            isExporting = true
        )

        val reset = dirty.resetForNewImport()

        assertThat(reset.backgroundMode).isEqualTo(BackgroundMode.Solid)
        assertThat(reset.backgroundImageUri).isNull()
        assertThat(reset.latestExport).isNull()
        assertThat(reset.isExporting).isFalse()
    }
}
