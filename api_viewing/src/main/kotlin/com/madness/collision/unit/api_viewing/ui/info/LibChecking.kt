/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.unit.api_viewing.ui.info

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.info.*
import com.madness.collision.util.mainApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibPage(
    modifier: Modifier = Modifier,
    app: ApiViewingApp,
    contentPadding: PaddingValues,
    listContent: LazyListScope.() -> Unit,
) {
    val pkgName = app.packageName
    val context = LocalContext.current
    val itemCollection = remember(pkgName) { PackCompCollection() }
    var itemCollectionNotifier by remember(pkgName) { mutableStateOf(0) }
    val notifierCollection = remember(itemCollectionNotifier) { itemCollection }
    val listTypeIndexMap = remember(itemCollectionNotifier) {
        compTypeList.withIndex().associate { (i, type) ->
            i to itemCollection.getTypeIndex(type)
        }
    }
    LaunchedEffect(itemCollectionNotifier) {
        Log.d("LIB-TYPE", listTypeIndexMap.entries.joinToString(prefix = "IndexMap/") { (a, b) -> "$a-$b" })
    }
    var selTabIndex by remember { mutableStateOf(0) }
    var scrollNotifier by remember(pkgName) { mutableStateOf(-1 to PackCompType.Activity) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var lastListTypeIndex = remember { 0 }
    val listIndexNotifier by remember(listTypeIndexMap) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val i = TwoDimenPositions.getCurrentIndex(index, listTypeIndexMap, lastListTypeIndex)
            lastListTypeIndex = i
            Log.d("LIB-TYPE", "lastListTypeIndex/index=$index/type=$i")
            if (index < nonCompItemCount) return@derivedStateOf -1
            i
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .onEach { Log.d("LIB-TYPE", "listState/firstVisibleItemIndex:$it") }
            .launchIn(this)
    }
    val loadingTypes = remember { mutableStateMapOf<PackCompType, Boolean>() }
    val isDark = mainApplication.isDarkTheme
    val backGreenColor = if (isDark) Color(0xFF11293C) else Color(0xFFC3EED6)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .horizontalScroll(rememberScrollState())
                .background(backGreenColor.copy(alpha = 0.45f))
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            for (tabIndex in 0 until compTypeList.size + nonCompItemCount) {
                if (tabIndex < nonCompItemCount) {
                    CompTab("Tags", selected = tabIndex == selTabIndex, loading = false, onClick = click@{
                        if (selTabIndex == tabIndex) return@click
                        selTabIndex = tabIndex
                        scope.launch { listState.scrollToItem(0) }
                    })
                } else {
                    val compType = compTypeList[tabIndex - 1]
                    val lastCompType = compTypeList.getOrNull(tabIndex - 2)
                    val typeLabel = when (compType) {
                        PackCompType.Activity -> "Activity"
                        PackCompType.Service -> "Service"
                        PackCompType.Receiver -> "Receiver"
                        PackCompType.Provider -> "Provider"
                        PackCompType.DexPackage -> "DexPackage"
                        PackCompType.NativeLibrary -> "NativeLibrary"
                    }
                    CompTab(typeLabel, selected = tabIndex == selTabIndex, loading = loadingTypes[compType] == true, onClick = click@{
                        if (selTabIndex == tabIndex) return@click
                        selTabIndex = tabIndex
                        scope.launch {
                            if (itemCollection.getTypeState(compType) == PackCompCollection.State.None) {
                                loadingTypes[compType] = true
                                withContext(Dispatchers.IO) {
                                    // load the last type as well since it is preloaded by lazy column
                                    lastCompType?.let { type ->
                                        launch { itemCollection.loadType(context, app, type) }
                                    }
                                    itemCollection.loadType(context, app, compType)
                                }
                                loadingTypes[compType] = false
                                val nextValue = itemCollectionNotifier + 1
                                scrollNotifier = nextValue to compType
                                itemCollectionNotifier = nextValue
                            } else {
                                listState.scrollToItem(itemCollection.getTypeIndex(compType))
                            }
                        }
                    })
                }
            }
        }
        ComponentList(modifier, notifierCollection, listState, contentPadding, loadCompType = { type ->
            scope.launch(Dispatchers.IO) {
                itemCollection.loadType(context, app, type)
                withContext(Dispatchers.Main) { itemCollectionNotifier++ }
            }
        }, listContent)
        LaunchedEffect(listIndexNotifier) {
            val index = listState.firstVisibleItemIndex
            selTabIndex = listIndexNotifier + nonCompItemCount
            Log.d("LIB-TYPE", "ListIndex/index=$index/type=${listIndexNotifier}/selTab=$selTabIndex")
        }
        LaunchedEffect(itemCollectionNotifier) {
            val (scrollN, compType) = scrollNotifier
            Log.d("LIB-TYPE", "$scrollN/${itemCollectionNotifier}")
            if (scrollN == itemCollectionNotifier) {
                scrollNotifier = scrollN - 1 to compType
                scope.launch {
                    val itemIndex = itemCollection.getTypeIndex(compType)
                    listState.scrollToItem(itemIndex)
                    Log.d("LIB-TYPE", "$scrollN/index:$itemIndex")
                }
            }
        }
    }
}

@Composable
private fun CompTab(label: String, selected: Boolean, loading: Boolean, onClick: () -> Unit) {
    var width: Int? by remember { mutableStateOf(null) }
    val widthDp = with(LocalDensity.current) { width?.toDp() }
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected && !loading) 0.95f else 0.75f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .animateContentSize()
            .sizeIn(minWidth = (widthDp ?: 20.dp) + 30.dp, minHeight = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onGloballyPositioned { if (width == null) width = it.size.width },
            text = label,
            color = animateColorAsState(color).value,
            fontSize = if (selected && !loading) 11.sp else 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            lineHeight = 11.sp,
            overflow = TextOverflow.Ellipsis,
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 1.4.dp,
            )
        }
    }
}

// show "tags" tab before comp types
private const val nonCompItemCount = 1
private val compTypeList = listOf(
    PackCompType.NativeLibrary,
    PackCompType.Activity,
    PackCompType.Service,
    PackCompType.Receiver,
    PackCompType.Provider,
    PackCompType.DexPackage,
)

private fun PackCompCollection.getItemCount(type: PackCompType): Int {
    val collectionTitleCount = 1
    val sectionCollection = this[type].firstOrNull() ?: return collectionTitleCount
    val sectionTitleCount = sectionCollection.sectionItems.filterNot { it.isEmpty() }.size
    val sectionDividerCount = (sectionTitleCount - 1).coerceAtLeast(0)
    val sectionNoDataCount = if (sectionTitleCount == 0) 1 else 0
    val poweredByCount = if (sectionCollection[CompSection.Marked].isNotEmpty()) 1 else 0
    return collectionTitleCount + sectionCollection.size +
            sectionTitleCount + sectionDividerCount + sectionNoDataCount + poweredByCount
}

private fun PackCompCollection.getTypeIndex(type: PackCompType): Int {
    val typeList = compTypeList
    val typeMap = typeList.withIndex().associate { it.value to it.index }
    val itemIndex = typeMap[type] ?: -1
    if (itemIndex <= 0) return nonCompItemCount
    val offsetList = typeList.subList(0, itemIndex)
    val typeOffsetCount = offsetList.sumOf { getItemCount(it) }
    val dividerOffsetCount = offsetList.size - 1
    // index of t3: offsetList[t0, t1, t2] - divider - t3[title, items]
    // after offset types, we have one divider before items of the specified type
    val typeTopCount = 1
    return typeOffsetCount + dividerOffsetCount + typeTopCount + nonCompItemCount
}

@Composable
private fun ComponentList(
    modifier: Modifier = Modifier,
    itemCollection: PackCompCollection,
    listState: LazyListState,
    contentPadding: PaddingValues,
    loadCompType: (PackCompType) -> Unit,
    listContent: LazyListScope.() -> Unit,
) {
    BoxWithConstraints {
        ComponentList(
            modifier = modifier,
            itemCollection = itemCollection,
            listState = listState,
            contentPadding = contentPadding,
            loadCompType = loadCompType,
            listContent = listContent,
            maxWidth = maxWidth,
        )
    }
}

@Composable
private fun ComponentList(
    modifier: Modifier = Modifier,
    itemCollection: PackCompCollection,
    listState: LazyListState,
    contentPadding: PaddingValues,
    loadCompType: (PackCompType) -> Unit,
    listContent: LazyListScope.() -> Unit,
    maxWidth: Dp,
) {
    val itemCount = itemCollection.sectionItems.flatten().sumOf { it.size }
    val typeCount = itemCollection.sectionSize
    val loadedTypeSet = itemCollection.entryIterator().asSequence().mapNotNullTo(HashSet()) {
        if (itemCollection.getTypeState(it.key) == PackCompCollection.State.Loaded) it.key else null
    }
    val layoutStrategy = remember(maxWidth) { LibItemLayoutStrategy(maxWidth.value) }
    var lastStrategyValues: Map<PackCompType, List<LibItemLayoutStrategy.Value?>> by remember {
        mutableStateOf(emptyMap())
    }
    val strategyValuesList = remember(loadedTypeSet) {
        val typeList = compTypeList
        val list = typeList.map { compType ->
            if (lastStrategyValues.containsKey(compType)) return@map lastStrategyValues[compType]
            val typeCollection = itemCollection[compType].firstOrNull() ?: return@map null
            val sections = typeCollection.entryIterator().asSequence().toList()
            sections.map { (sec, items) -> layoutStrategy.calculate(items, compType, sec) }
        }
        lastStrategyValues = typeList.zip(list).mapNotNull { (k, v) -> v?.let { k to v } }.toMap()
        list
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        listContent()
        for ((typeIndex, compType) in compTypeList.withIndex()) {
            val typeCollection = itemCollection[compType].firstOrNull()
            val typeLabel = when (compType) {
                PackCompType.Activity -> "Activity"
                PackCompType.Service -> "Service"
                PackCompType.Receiver -> "Receiver"
                PackCompType.Provider -> "Provider"
                PackCompType.DexPackage -> "DexPackage"
                PackCompType.NativeLibrary -> "NativeLibrary"
            }
            if (typeIndex > 0) {
                item {
                    Divider(
                        modifier = Modifier.padding(top = 8.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    )
                }
            }
            item {
                if (itemCollection.getTypeState(compType) == PackCompCollection.State.None) {
                    SideEffect {
                        Log.d("LIB-TYPE", "$typeIndex/${compType}")
                        loadCompType(compType)
                    }
                }
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    text = typeLabel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                )
            }
            if (typeCollection != null) {
                val sections = typeCollection.entryIterator().asSequence().toList()
                val strategyValues = strategyValuesList[typeIndex].orEmpty()
                if (sections.all { it.value.isEmpty() }) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 5.dp),
                            text = "No data available",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                        )
                    }
                }
                for (sectionIndex in sections.indices) {
                    val compSection = sections[sectionIndex].key
                    val itemList = sections[sectionIndex].value
            //                Log.d("LIB", "$typeIndex, $sectionIndex")
                    if (sectionIndex > 0 && itemList.isNotEmpty() && sections[sectionIndex - 1].value.isNotEmpty()) {
                        item {
                            Divider(
                                modifier = Modifier.padding(vertical = 5.dp).padding(start = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            )
                        }
                    }
                    if (itemList.isNotEmpty()) {
                        val sectionType = when (compSection) {
                            CompSection.Marked -> "Marked"
                            CompSection.Normal -> "Normal"
                            CompSection.MinimizedSelf -> "Self"
                            CompSection.Minimized -> "Minimized"
                        }
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 5.dp),
                                text = "$sectionType ${itemList.size}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                            )
                        }
                    }
                    val sortedGroups = when (val value = strategyValues.getOrNull(sectionIndex)) {
                        null -> emptyList()
                        is LibItemLayoutStrategy.GroupedValue -> value.entries
                        is LibItemLayoutStrategy.SimpleValue -> emptyList()
                    }
                    val keyCoefficient = typeCount * (typeIndex + 1) + sections.size * (sectionIndex + 1)
                    if (sortedGroups.isNotEmpty()) {
                        for ((groupIndex, groupEntry) in sortedGroups.withIndex()) {
                            val (sizeLimit, rowList) = groupEntry
                            val rowSize = layoutStrategy.calculateRowSize(sizeLimit)
                            if (rowSize > 1) {
                                val minimizedList = rowList.chunked(rowSize)
                                itemsIndexed(minimizedList, key = { i, _ ->
                                    itemCount * (keyCoefficient + groupIndex + 2) + i
                                }) { _, items ->
                                    ChunkedLibItem(items, rowSize)
                                }
                            } else {
                                itemsIndexed(rowList, key = { i, _ ->
                                    itemCount * (keyCoefficient + groupIndex + 2) + i
                                }) { _, item ->
                                    if (item is MarkedComponent) {
                                        MarkedSimpleLibItem(item)
                                    } else {
                                        PlainLibItem(item = item)
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(itemList, key = { i, _ ->
                            val k = itemCount * (keyCoefficient + 1) + i
//                            Log.d("LIB-K", "$itemCount * ($typeCount * $typeIndex + $sectionIndex) + $i = $k")
                            k
                        }) { _, item ->
                            if (item is MarkedComponent) {
                                if (sectionIndex < 1) {
                                    MarkedLibItem(item = item)
                                } else {
                                    MarkedSimpleLibItem(item)
                                }
                            } else {
                                PlainLibItem(item = item)
                            }
                        }
                    }
                    if (compSection == CompSection.Marked && itemList.isNotEmpty()) {
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 3.dp, bottom = 5.dp),
                                text = "Powered by LibChecker",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 6.sp,
                                lineHeight = 7.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChunkedLibItem(items: List<PackComponent>, rowSize: Int) {
    // make items bottom aligned
    Row(modifier = Modifier.padding(horizontal = 17.dp), verticalAlignment = Alignment.Bottom) {
        for (item in items) {
            Box(modifier = Modifier.weight(1f)) {
                if (item is MarkedComponent) {
                    MarkedSimpleLibItem(item, horizontalPadding = 3.dp)
                } else {
                    PlainLibItem(item = item, horizontalPadding = 3.dp)
                }
            }
        }
        for (i in 0 until (rowSize - items.size)) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}
