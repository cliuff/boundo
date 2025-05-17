package com.madness.collision.settings

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.madness.collision.R
import com.madness.collision.util.dev.LayoutDirectionPreviews
import com.madness.collision.util.mainApplication
import com.madness.collision.util.ui.initDelayed
import com.madness.collision.versatile.ctrl.ControlInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun DeviceControlsPage(paddingValues: PaddingValues) {
    val viewModel = viewModel<DeviceControlsViewModel>()
    // delay state to show switch animation more clearly
    val isEnabled by viewModel.enabledState.initDelayed(200)
    val controlList by viewModel.controlListState.initDelayed(210)
    // use LaunchedEffect instead of SideEffect to avoid repetitive invocations
    // that are triggered by consecutive recompositions due to list changes
    LaunchedEffect(Unit) { viewModel.init() }
    DeviceControls(
        featureEnabled = isEnabled,
        onToggleFeature = { viewModel.setEnabled(!isEnabled) },
        controlList = controlList,
        paddingValues = paddingValues,
    )
}

@Composable
private fun DeviceControls(
    featureEnabled: Boolean,
    onToggleFeature: (Boolean) -> Unit,
    controlList: List<ControlInfo>,
    paddingValues: PaddingValues,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(when (mainApplication.isDarkTheme) {
                false -> MaterialTheme.colorScheme.background
                true -> Color.Black
            })
            .padding(paddingValues)
            .padding(horizontal = 15.dp)
            .padding(top = 13.dp, bottom = 50.dp),
    ) {
        if (maxWidth < 600.dp) {
            Column {
                FeatureSwitchCard(enabled = featureEnabled, onToggle = onToggleFeature)
                Spacer(modifier = Modifier.height(15.dp))
                ControlListCard(controlList = controlList)
            }
        } else {
            Row {
                Box(modifier = Modifier.weight(1f)) {
                    ControlListCard(controlList = controlList)
                }
                Spacer(modifier = Modifier.width(15.dp))
                Box(modifier = Modifier.weight(1f)) {
                    FeatureSwitchCard(enabled = featureEnabled, onToggle = onToggleFeature)
                }
            }
        }
    }
}

@Composable
private fun FeatureSwitchCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Contained(onClick = { onToggle(!enabled) }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.settings_dev_ctrl_switch),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Switch(
                    modifier = Modifier.scale(0.7f),
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Material Switch height is hardcoded
                Spacer(modifier = Modifier.height(39.dp))
                Divider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.settings_dev_ctrl_switch_desc),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun ControlListCard(controlList: List<ControlInfo>) {
    Contained {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(stiffness = 150f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_dev_ctrl_list_title),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box {
                androidx.compose.animation.AnimatedVisibility(
                    visible = controlList.isEmpty(),
                    enter = fadeIn(animationSpec = spring(stiffness = 50f)),
                    exit = fadeOut(animationSpec = spring(stiffness = 1500f)),
                ) {
                    Text(
                        text = stringResource(R.string.text_no_content),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        lineHeight = 15.sp,
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = controlList.isNotEmpty(),
                    enter = fadeIn(animationSpec = spring(stiffness = 50f)),
                    exit = fadeOut(animationSpec = spring(stiffness = 150f)),
                ) {
                    Column {
                        for (ctrl in controlList) {
                            key(ctrl) {
                                ControlItem(control = ctrl)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlItem(control: ControlInfo) {
    val context = LocalContext.current
    val iconDrawable by produceState(null as Drawable?, control.icon) {
        val ic = control.icon ?: return@produceState
        value = withContext(Dispatchers.IO) { ic.loadDrawable(context) }
    }
    Row(
        modifier = Modifier.padding(vertical = 4.dp).heightIn(min = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier.width(20.dp).heightIn(max = 20.dp),
            model = iconDrawable,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(text = control.title.toString(), fontSize = 14.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun Contained(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        shape = AbsoluteSmoothCornerShape(14.dp, 80),
        colors = when (mainApplication.isDarkTheme) {
            false -> CardDefaults.outlinedCardColors(containerColor = Color.White)
            true -> CardDefaults.outlinedCardColors()
        },
        border = BorderStroke(
            width = 0.4.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contained(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        onClick = onClick,
        shape = AbsoluteSmoothCornerShape(14.dp, 80),
        colors = when (mainApplication.isDarkTheme) {
            false -> CardDefaults.outlinedCardColors(containerColor = Color.White)
            true -> CardDefaults.outlinedCardColors()
        },
        border = BorderStroke(
            width = 0.4.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        ),
        content = content,
    )
}

@LayoutDirectionPreviews
@Composable
private fun DeviceControlsPreview() {
    val controls = remember {
        listOf<ControlInfo>(
//            ControlDetails("Audio Timer", "", )
        )
    }
    MaterialTheme {
        DeviceControls(
            featureEnabled = true,
            onToggleFeature = { },
            controlList = controls,
            paddingValues = PaddingValues(),
        )
    }
}