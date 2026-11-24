package com.vibify.music.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibify.music.data.model.Track
import kotlin.math.min

/** Whether the floating mini player is covering list content. */
val LocalHasMiniPlayer = compositionLocalOf { false }

/** Hide the soft keyboard and clear text focus (search field). */
val LocalHideKeyboard = compositionLocalOf { {} }

/** Open the song-details sheet for this track. */
val LocalOpenTrackDetails = compositionLocalOf<(Track) -> Unit> { {} }

object VibifyDimens {
    val screenPadding = 16.dp
    val sectionSpacing = 20.dp
    val itemSpacing = 12.dp
    val cardRadius = 10.dp
    val tileRadius = 8.dp
    val miniPlayerHeight = 64.dp
}

@Composable
fun statusBarPaddingValues(): PaddingValues =
    WindowInsets.statusBars.asPaddingValues()

@Composable
fun navigationBarPaddingValues(): PaddingValues =
    WindowInsets.navigationBars.asPaddingValues()

/** Bottom padding for scrollable lists (mini player sits above the nav bar). */
@Composable
fun vibifyListBottomPadding(): Dp {
    val hasMini = LocalHasMiniPlayer.current
    return if (hasMini) VibifyDimens.miniPlayerHeight + 16.dp else 16.dp
}

@Composable
fun vibifyListContentPadding(): PaddingValues =
    PaddingValues(bottom = vibifyListBottomPadding())

@Composable
fun screenWidthDp(): Dp = LocalConfiguration.current.screenWidthDp.dp

@Composable
fun screenHeightDp(): Dp = LocalConfiguration.current.screenHeightDp.dp

@Composable
fun isCompactHeight(): Boolean = LocalConfiguration.current.screenHeightDp < 700

/** Phone = 2, large phone/fold = 3, tablet = 4. */
@Composable
fun gridColumnCount(): Int {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width >= 840 -> 4
        width >= 600 -> 3
        else -> 2
    }
}
@Composable
fun carouselCardSize(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return min(168, maxOf(120, (w * 0.38f).toInt())).dp
}

/** Made-for-you playlist card size. */
@Composable
fun playlistCardSize(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return min(176, maxOf(132, (w * 0.42f).toInt())).dp
}

/** Search browse category card height. */
@Composable
fun categoryCardHeight(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return min(110, maxOf(88, (w * 0.26f).toInt())).dp
}

/** Playlist detail hero art size. */
@Composable
fun playlistHeroSize(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    val h = LocalConfiguration.current.screenHeightDp
    val base = min(w * 0.38f, h * 0.22f)
    return min(180, maxOf(110, base.toInt())).dp
}

fun songCountLabel(count: Int): String =
    if (count == 1) "1 song" else "$count songs"

/** Strip noisy download-site / bitrate prefixes from scraped titles. */
fun cleanTrackTitle(raw: String): String {
    var t = raw.trim()
    t = t.replace(Regex("""(?i)^vidssave\.com\s*"""), "")
    t = t.replace(Regex("""(?i)\b\d{2,3}\s*kbps\b"""), "")
    t = t.replace(Regex("""\s{2,}"""), " ").trim()
    return t.ifBlank { raw.trim() }
}
