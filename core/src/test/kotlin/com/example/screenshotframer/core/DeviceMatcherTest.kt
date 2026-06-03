package com.example.screenshotframer.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceMatcherTest {
    @Test
    fun exactResolutionRanksFirst() {
        val manifest = FrameManifestParser.parse(SAMPLE_MANIFEST)

        val matches = DeviceMatcher(manifest).match(ScreenshotMetadata(width = 1344, height = 2992))

        assertThat(matches.first().device.id).isEqualTo("pixel-9-pro")
        assertThat(matches.first().confidence).isEqualTo(MatchConfidence.Exact)
    }

    @Test
    fun closeAspectRatioReturnsCandidateList() {
        val manifest = FrameManifestParser.parse(SAMPLE_MANIFEST)

        val matches = DeviceMatcher(manifest).match(ScreenshotMetadata(width = 1440, height = 3088))

        assertThat(matches).isNotEmpty()
        assertThat(matches.take(2).map { it.device.id }).contains("galaxy-s24-ultra")
        assertThat(matches.first().confidence).isAnyOf(MatchConfidence.High, MatchConfidence.Medium)
    }
}
