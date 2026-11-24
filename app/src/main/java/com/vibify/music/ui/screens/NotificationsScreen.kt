package com.vibify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibify.music.ui.AppNotification
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.vibifyListBottomPadding

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SpotifyWhite
                    )
                }
                Text(
                    text = "Notifications",
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text(text = "Clear all", color = SpotifyGreen, fontSize = 13.sp)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = vibifyListBottomPadding()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = SpotifySubtext,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No notifications yet",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Refresh, likes, and scans will show up here",
                        color = SpotifySubtext,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = vibifyListBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpotifyActiveCard)
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpotifyBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = SpotifyWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = item.message,
                                color = SpotifySubtext,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.timeLabel,
                                color = SpotifySubtext,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
