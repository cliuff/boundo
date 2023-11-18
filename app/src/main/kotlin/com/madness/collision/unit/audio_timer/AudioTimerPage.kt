/*
 * Copyright 2023 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.audio_timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madness.collision.R
import com.madness.collision.util.ui.layoutDirected
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun AudioTimerPage(paddingValues: PaddingValues, onStartTimer: () -> Unit, onNavControls: () -> Unit) {
    val context = LocalContext.current
    val timerController = remember { AudioTimerController(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val statusFlow = remember { timerController.getTimerStatusFlow(lifecycleOwner, scope) }
    val status by statusFlow.collectAsState()
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(paddingValues)
            .padding(top = 13.dp, bottom = 50.dp),
    ) {
        AnimatedVisibility(visible = status) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp)
                    .clip(AbsoluteSmoothCornerShape(20.dp, 60))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                TimerStatus(status = status, timerController = timerController)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AbsoluteSmoothCornerShape(20.dp, 60))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SetTimer(timerController = timerController, onStart = onStartTimer)
        }
        Spacer(modifier = Modifier.height(15.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AbsoluteSmoothCornerShape(20.dp, 60))
                .clickable(onClick = onNavControls)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.at_hint_dev_ctrl),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 12.sp,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = layoutDirected(
                    { Icons.Outlined.ChevronRight }, { Icons.Outlined.ChevronLeft }),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun TimerStatus(status: Boolean, timerController: AudioTimerController) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val runningStatusFlow = remember { timerController.getTimerRunningStatus(lifecycleOwner) }
    val displayStatus by runningStatusFlow.collectAsState("")
    Column() {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column() {
                Text(
                    text = stringResource(R.string.at_status_running),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clickable(
                        onClick = { timerController.stopTimer() },
                        interactionSource = MutableInteractionSource(),
                        indication = rememberRipple(bounded = false),
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.26f))
                    .padding(8.dp),
            ) {
                Icon(
                    modifier = Modifier.height(32.dp).widthIn(max = 32.dp).aspectRatio(1f),
                    imageVector = if (status) Icons.Rounded.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                )
            }
        }
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = displayStatus,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetTimer(timerController: AudioTimerController, onStart: () -> Unit) {
    val viewModel: AtUnitViewModel = viewModel()
    val initUiState = remember { viewModel.uiState.value }
    var hourInput by rememberSaveable {
        val value = initUiState.hours?.toString()
        mutableStateOf(value ?: "")
    }
    var minuteInput by rememberSaveable {
        val value = initUiState.minutes?.toString()
        mutableStateOf(value ?: "")
    }
    Column() {
        Text(
            text = stringResource(R.string.at_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.height(25.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = hourInput,
                onValueChange = { v ->
                    hourInput = v
                    viewModel.setHours(v.toIntOrNull())
                },
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 200.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                shape = AbsoluteSmoothCornerShape(10.dp, 60),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, textAlign = TextAlign.End),
                placeholder = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.at_hour_hint),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = "h",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            BoxWithConstraints {
                val space = remember { if (maxWidth > 360.dp) 20.dp else 8.dp }
                Spacer(modifier = Modifier.width(space))
            }
            TextField(
                value = minuteInput,
                onValueChange = { v ->
                    minuteInput = v
                    viewModel.setMinutes(v.toIntOrNull())
                },
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 200.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                shape = AbsoluteSmoothCornerShape(10.dp, 60),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, textAlign = TextAlign.End),
                placeholder = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.at_minute_hint),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = "min",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onStart)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                .padding(horizontal = 15.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.at_action_start),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}