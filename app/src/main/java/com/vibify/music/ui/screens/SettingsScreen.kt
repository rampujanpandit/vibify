package com.vibify.music.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.vibifyListBottomPadding
import java.util.Calendar

@Composable
fun SettingsScreen(
    likedCount: Int,
    localCount: Int,
    onBack: () -> Unit,
    onRefreshHome: () -> Unit,
    onScanLocal: () -> Unit,
    onClearLikes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = vibifyListBottomPadding())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SpotifyWhite
                )
            }
            Text(
                text = "Settings",
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }

        Text(
            text = "Library",
            color = SpotifySubtext,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SettingsRow(
            icon = {
                Icon(Icons.Outlined.Refresh, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            },
            title = "Refresh Home Feed",
            subtitle = "Reload live charts and local songs",
            onClick = onRefreshHome
        )
        SettingsRow(
            icon = {
                Icon(Icons.Rounded.Folder, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            },
            title = "Scan Phone Storage",
            subtitle = "$localCount local songs found",
            onClick = onScanLocal
        )
        SettingsRow(
            icon = {
                Icon(Icons.Outlined.Delete, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            },
            title = "Clear Liked Songs",
            subtitle = "$likedCount liked songs will be removed",
            onClick = onClearLikes
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "About",
            color = SpotifySubtext,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SettingsRow(
            icon = {
                Icon(Icons.Outlined.Info, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            },
            title = "Vibify Music",
            subtitle = "Version 1.0 · Live charts + local MP3 player",
            onClick = {}
        )
        SettingsRow(
            icon = {
                Icon(Icons.Outlined.Person, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            },
            title = "About the developer",
            subtitle = "Made with care by Ram Pujan — Android & music app enthusiast.",
            onClick = {}
        )
        SettingsRow(
            icon = {
                Icon(Icons.Outlined.Copyright, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            },
            title = "Copyright",
            subtitle = "© ${Calendar.getInstance().get(Calendar.YEAR)} Ram Pujan. All rights reserved.",
            onClick = {}
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VibifyDimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vibify Music",
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Designed & developed by Ram Pujan",
                color = SpotifySubtext,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "© ${Calendar.getInstance().get(Calendar.YEAR)} Ram Pujan. All rights reserved.",
                color = SpotifySubtext,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyActiveCard),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = subtitle, color = SpotifySubtext, fontSize = 12.sp)
        }
    }
}
