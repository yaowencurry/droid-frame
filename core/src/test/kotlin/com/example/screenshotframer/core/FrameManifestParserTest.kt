package com.example.screenshotframer.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrameManifestParserTest {
    @Test
    fun parsesDevicesAndColorsFromManifest() {
        val manifest = FrameManifestParser.parse(SAMPLE_MANIFEST)

        assertThat(manifest.devices).hasSize(2)
        assertThat(manifest.devices.first().id).isEqualTo("pixel-9-pro")
        assertThat(manifest.devices.first().colors.map { it.name }).containsExactly("Obsidian", "Porcelain")
        assertThat(manifest.devices.first().screen.width).isEqualTo(1344)
        assertThat(manifest.devices.first().safeArea.left).isEqualTo(92)
    }
}
