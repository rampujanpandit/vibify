package com.vibify.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibify.music.R
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyDivider
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurface
import com.vibify.music.ui.theme.SpotifySurfaceLight
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.vibifyListBottomPadding
import java.util.Calendar

private val Danger = Color(0xFFE85D5D)
private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun SettingsScreen(
    likedCount: Int,
    localCount: Int,
    onBack: () -> Unit,
    onRefreshHome: () -> Unit,
    onScanLocal: () -> Unit,
    onClearLikes: () -> Unit,
    sleepLabel: String,
    sleepMode: String,
    spokenSpeed: Float,
    skipSeconds: Int,
    resumeOnOpen: Boolean,
    onSelectSleep: (String) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSkip: (Int) -> Unit,
    onResumeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = vibifyListBottomPadding())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF143026), SpotifyBlack)))
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Settings",
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = 18.dp)
                )
                Text(
                    text = "Playback, library, and about",
                    color = SpotifySubtext,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(SpotifySurface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF143026)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_vibify_mark),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Vibify", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Music, podcasts, and stories",
                            color = SpotifySubtext,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "1.0",
                        color = SpotifyBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SpotifyGreen)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        SectionLabel("Playback")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sleep timer", color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "Pauses audio after the time you choose",
                    color = SpotifySubtext,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                OptionPills(
                    options = listOf("off" to "Off", "15" to "15m", "30" to "30m", "45" to "45m", "end" to "End"),
                    selected = sleepMode,
                    onSelect = onSelectSleep
                )
                if (sleepMode != "off") {
                    Text(
                        text = sleepLabel,
                        color = SpotifyGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                ControlDivider()
                Text("Story speed", color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "Saved for podcasts and stories",
                    color = SpotifySubtext,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                OptionPills(
                    options = listOf(1f to "1x", 1.25f to "1.25x", 1.5f to "1.5x", 2f to "2x").map {
                        it.first.toString() to it.second
                    },
                    selected = spokenSpeed.toString(),
                    onSelect = { onSelectSpeed(it.toFloat()) }
                )

                ControlDivider()
                Text("Skip length", color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "Jump back or forward while listening",
                    color = SpotifySubtext,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                OptionPills(
                    options = listOf(10 to "10s", 15 to "15s", 30 to "30s").map { it.first.toString() to it.second },
                    selected = skipSeconds.toString(),
                    onSelect = { onSelectSkip(it.toInt()) }
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = SpotifyDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Resume on open", color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "Continue the last song or story",
                        color = SpotifySubtext,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = resumeOnOpen,
                    onCheckedChange = onResumeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpotifyBlack,
                        checkedTrackColor = SpotifyGreen,
                        uncheckedThumbColor = SpotifySubtext,
                        uncheckedTrackColor = SpotifySurfaceLight,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }

        SectionLabel("Library")
        SettingsCard {
            ActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Refresh home",
                subtitle = "Update charts and recommendations",
                onClick = onRefreshHome
            )
            HorizontalDivider(modifier = Modifier.padding(start = 62.dp), thickness = 0.5.dp, color = SpotifyDivider)
            ActionRow(
                icon = Icons.Outlined.FolderOpen,
                title = "Device music",
                subtitle = if (localCount == 0) "No songs found on this phone" else "$localCount songs on this phone",
                onClick = onScanLocal
            )
            HorizontalDivider(modifier = Modifier.padding(start = 62.dp), thickness = 0.5.dp, color = SpotifyDivider)
            ActionRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "Clear liked songs",
                subtitle = if (likedCount == 0) "No liked songs yet" else "$likedCount liked songs on this device",
                tint = Danger,
                onClick = onClearLikes
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_vibify_mark),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Vibify",
                color = SpotifyWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "Designed and developed by Ram Pujan",
                color = SpotifySubtext,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
            )
            Text(
                "© $year Ram Pujan. All rights reserved.",
                color = Color(0xFF6E6E6E),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun OptionPills(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF242424))
            .padding(3.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = id == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) SpotifyGreen else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) SpotifyBlack else SpotifySubtext,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ControlDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        thickness = 0.5.dp,
        color = SpotifyDivider
    )
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF8A8A8A),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(SpotifySurface)
    ) {
        content()
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = SpotifyGreen,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = SpotifySubtext, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF6E6E6E),
            modifier = Modifier.size(20.dp)
        )
    }
}
