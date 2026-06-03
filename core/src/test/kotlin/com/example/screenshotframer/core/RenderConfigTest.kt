package com.example.screenshotframer.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RenderConfigTest {
    @Test
    fun pngTransparentExportPreservesAlpha() {
        val config = ExportConfig(format = ExportFormat.Png, transparentBackground = true)

        assertThat(config.supportsAlpha).isTrue()
        assertThat(config.mimeType).isEqualTo("image/png")
    }

    @Test
    fun jpegExportDisablesTransparentBackground() {
        val config = ExportConfig(format = ExportFormat.Jpeg, transparentBackground = true)

        assertThat(config.supportsAlpha).isFalse()
        assertThat(config.effectiveTransparentBackground).isFalse()
        assertThat(config.mimeType).isEqualTo("image/jpeg")
    }
}
