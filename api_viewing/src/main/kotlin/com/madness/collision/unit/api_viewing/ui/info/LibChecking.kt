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

import android.content.Context
import android.os.Parcelable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.info.*
import com.madness.collision.util.mainApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

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
    val notifierCollection = remember(itemCollectionNotifier) { itemCollection.clone() }
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
    val libListState = remember(itemCollection, listState) { LibListState(itemCollection, listState, context) }
    var lastListTypeIndex = remember { 0 }
    val listIndexNotifier by remember(listTypeIndexMap) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val i = TwoDimenPositions.getCurrentIndex(index, listTypeIndexMap, lastListTypeIndex)
            lastListTypeIndex = i
            if (index < nonCompItemCount) return@derivedStateOf -1
            i
        }
    }
//    LaunchedEffect(Unit) {
//        snapshotFlow { listState.firstVisibleItemIndex }
//            .onEach { Log.d("LIB-TYPE", "listState/firstVisibleItemIndex:$it") }
//            .launchIn(this)
//    }
    val loadingTypes = remember { mutableStateMapOf<PackCompType, Boolean>() }
    val isDark = mainApplication.isDarkTheme
    val backGreenColor = if (isDark) Color(0xFF11293C) else Color(0xFFC3EED6)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .background(backGreenColor.copy(alpha = 0.45f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            for (tabIndex in 0 until compTypeList.size + nonCompItemCount) {
                if (tabIndex < nonCompItemCount) {
                    val label = stringResource(R.string.av_settings_tags)
                    CompTab(label, selected = tabIndex == selTabIndex, loading = false, onClick = click@{
                        if (selTabIndex == tabIndex) return@click
                        selTabIndex = tabIndex
                        scope.launch { libListState.scrollToItem(0) }
                    })
                } else {
                    val compType = compTypeList[tabIndex - 1]
                    val lastCompType = compTypeList.getOrNull(tabIndex - 2)
                    val typeLabel = getTypeLabel(compType)
                    CompTab(typeLabel, selected = tabIndex == selTabIndex, loading = loadingTypes[compType] == true, onClick = click@{
                        if (selTabIndex == tabIndex) return@click
                        selTabIndex = tabIndex
                        scope.launch {
                            if (itemCollection.getTypeState(compType) == PackCompCollection.State.None) {
                                loadingTypes[compType] = true
                                withContext(Dispatchers.IO) {
                                    // load the last type as well since it is preloaded by lazy column
                                    val types = listOfNotNull(lastCompType, compType)
                                    libListState.loadType(app, *types.toTypedArray())
                                }
                                loadingTypes[compType] = false
                                val nextValue = itemCollectionNotifier + 1
                                scrollNotifier = nextValue to compType
                                itemCollectionNotifier = nextValue
                            } else {
                                libListState.scrollToItem(itemCollection.getTypeIndex(compType))
                            }
                        }
                    })
                }
            }
        }
        ComponentList(modifier, notifierCollection, listState, contentPadding, loadCompType = { type ->
            scope.launch(Dispatchers.IO) {
                libListState.loadType(app, type)
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
                    libListState.scrollToItem(itemIndex)
                    Log.d("LIB-TYPE", "$scrollN/index:$itemIndex")
                }
            }
        }
    }
}

class LibListState(
    private val compCollection: PackCompCollection,
    private val state: LazyListState,
    private val context: Context
) {
    private val loadMutex = Mutex()
    private val scrollMutex = Mutex()

    suspend fun loadType(app: ApiViewingApp, vararg type: PackCompType) {
        // use loadMutex to forbid parallel loading
        loadMutex.withLock {
            when (type.size) {
                0 -> Unit
                1 -> compCollection.loadType(context, app, type[0])
                else -> supervisorScope {
                    type.forEach { t ->
                        launch { compCollection.loadType(context, app, t) }
                    }
                }
            }
        }
    }

    suspend fun scrollToItem(index: Int) {
        // use loadMutex to forbid scrolling when loading
        loadMutex.withLock {
            scrollMutex.withLock {
                state.scrollToItem(index)
            }
        }
    }
}

@Composable
private fun getTypeLabel(compType: PackCompType) = when (compType) {
    PackCompType.Activity -> "Activity"
    PackCompType.Service -> "Service"
    PackCompType.Receiver -> "Receiver"
    PackCompType.Provider -> "Provider"
    PackCompType.DexPackage -> "Dex"
    PackCompType.NativeLibrary -> stringResource(R.string.av_info_lib_native_lib)
    PackCompType.SharedLibrary -> stringResource(R.string.av_info_lib_shared_lib)
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
    PackCompType.SharedLibrary,
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

@Stable
class LibPrefs(compressedSize: Boolean, systemLibs: Boolean) {
    var preferCompressedSize: Boolean by mutableStateOf(compressedSize)
    var preferSystemLibs: Boolean by mutableStateOf(systemLibs)
}

val LocalLibPrefs = compositionLocalOf<LibPrefs> { error("LibPrefs not provided") }

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
    val layoutGroupsList = remember(strategyValuesList) {
        strategyValuesList.map list@{ strategyValues ->
            strategyValues ?: return@list null
            strategyValues.map layout@{ value ->
                if (value !is LibItemLayoutStrategy.GroupedValue) return@layout null
                value.entries.map { (sizeLimit, rowList) ->
                    val rowSize = layoutStrategy.calculateRowSize(sizeLimit)
                    when {
                        rowSize <= 1 -> GroupedLayoutList.Normal(rowList)
                        else -> GroupedLayoutList.Chunked(rowSize, rowList.chunked(rowSize))
                    }
                }
            }
        }
    }
    val libPrefs = remember {
        LibPrefs(compressedSize = true, systemLibs = false)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        listContent()
        for ((typeIndex, compType) in compTypeList.withIndex()) {
            val typeCollection = itemCollection[compType].firstOrNull()
            if (typeIndex > 0) {
                item { CompTypeDivider() }
            }
            item {
                if (itemCollection.getTypeState(compType) == PackCompCollection.State.None) {
                    SideEffect {
                        Log.d("LIB-TYPE", "$typeIndex/${compType}")
                        loadCompType(compType)
                    }
                }
                CompTypeTitle(compType, libPrefs)
            }
            if (typeCollection != null) {
                val sections = typeCollection.entryIterator().asSequence().toList()
                if (sections.all { it.value.isEmpty() }) {
                    item { EmptyContent() }
                }
                for (sectionIndex in sections.indices) {
                    val (compSection, itemList) = sections[sectionIndex]
                    if (sectionIndex > 0 && itemList.isNotEmpty() &&
                        (0 until sectionIndex).any { sections[it].value.isNotEmpty() }) {
                        item { SectionDivider() }
                    }
                    // hide section title for shared libs changing size
                    if (itemList.isNotEmpty() && compType != PackCompType.SharedLibrary) {
                        item { SectionTitle(compSection, itemList.size) }
                    }
                    val layoutGroups = layoutGroupsList[typeIndex]?.getOrNull(sectionIndex)
                    layoutGroupItems(itemList, layoutGroups, typeIndex, sectionIndex, libPrefs,
                        altRowColorEnabled = compSection != CompSection.Marked)
                    if (compSection == CompSection.Marked && itemList.isNotEmpty()) {
                        item { LibCheckerMark() }
                    }
                }
            }
        }
    }
}

sealed interface GroupedLayoutList {
    data class Chunked(
        /** maximum number of columns in a row */
        val rowSize: Int,
        val chunks: List<List<PackComponent>>,
    ) : GroupedLayoutList
    data class Normal(val rowList: List<PackComponent>) : GroupedLayoutList
}

/** Dedicated class to provide [equals] implementation for an [IntArray] field */
@Parcelize
class CompositeItemKey(private val indexes: IntArray) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CompositeItemKey
        if (!indexes.contentEquals(other.indexes)) return false
        return true
    }

    override fun hashCode(): Int {
        return indexes.contentHashCode()
    }
}

fun compItemKeyOf(vararg indexes: Int) = CompositeItemKey(indexes)

private fun LazyListScope.layoutGroupItems(
    itemList: List<PackComponent>,
    layoutGroups: List<GroupedLayoutList>?,
    typeIndex: Int,
    sectionIndex: Int,
    libPrefs: LibPrefs,
    altRowColorEnabled: Boolean,
) {
    if (layoutGroups != null) {
        val rowsList = layoutGroups.map { layoutList ->
            when (layoutList) {
                is GroupedLayoutList.Chunked -> layoutList.chunks.size
                is GroupedLayoutList.Normal -> layoutList.rowList.size
            }
        }
        val applyAltRowColor = altRowColorEnabled && rowsList.sum() >= 3
        val consecutiveOffset = when {
            applyAltRowColor -> rowsList.runningFold(0) { acc, size -> acc + size }
            else -> emptyList()
        }
        for ((groupIndex, layoutList) in layoutGroups.withIndex()) {
            val key = { i: Int, _: Any -> compItemKeyOf(typeIndex, sectionIndex, groupIndex, i) }
            if (applyAltRowColor) {
                val indexOffset = consecutiveOffset[groupIndex]
                if (layoutList is GroupedLayoutList.Chunked) {
                    val (rowSize, chunks) = layoutList
                    itemsIndexed(chunks, key) { i, items ->
                        val altColor = (indexOffset + i) % 2 == 0
                        AlternateListItem(altColor) { ChunkedLibItem(items, rowSize) }
                    }
                } else if (layoutList is GroupedLayoutList.Normal) {
                    itemsIndexed(layoutList.rowList, key) { i, item ->
                        AlternateListItem(applyAlternateColor = (indexOffset + i) % 2 == 0) {
                            if (item is MarkedComponent) MarkedSimpleLibItem(item) else PlainLibItem(item)
                        }
                    }
                }
            } else {
                if (layoutList is GroupedLayoutList.Chunked) {
                    val (rowSize, chunks) = layoutList
                    itemsIndexed(chunks, key) { _, items -> ChunkedLibItem(items, rowSize) }
                } else if (layoutList is GroupedLayoutList.Normal) {
                    itemsIndexed(layoutList.rowList, key) { _, item ->
                        if (item is MarkedComponent) MarkedSimpleLibItem(item) else PlainLibItem(item)
                    }
                }
            }
        }
    } else {
        val key = { i: Int, _: Any -> compItemKeyOf(typeIndex, sectionIndex, i) }
        val applyAltRowColor = altRowColorEnabled && itemList.size >= 3
        if (applyAltRowColor) {
            itemsIndexed(itemList, key) { i, item ->
                val altColor = i % 2 == 0
                AlternateListItem(altColor) { ItemList(item, sectionIndex, libPrefs) }
            }
        } else {
            itemsIndexed(itemList, key) { _, item -> ItemList(item, sectionIndex, libPrefs) }
        }
    }
}

@Composable
private fun AlternateListItem(applyAlternateColor: Boolean, content: @Composable () -> Unit) {
    if (applyAlternateColor) {
        val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        Box(modifier = Modifier.fillMaxWidth().background(color)) { content() }
    } else {
        content()
    }
}

@Composable
private fun CompTypeDivider() {
    Divider(
        modifier = Modifier.padding(top = 8.dp),
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    )
}

@Composable
private fun CompTypeTitle(compType: PackCompType, libPrefs: LibPrefs) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            text = getTypeLabel(compType),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 14.sp,
        )
        if (compType == PackCompType.NativeLibrary) {
            val textRes = when {
                libPrefs.preferCompressedSize -> R.string.av_info_lib_pref_compressed_size
                else -> R.string.av_info_lib_pref_uncompressed_size
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { libPrefs.preferCompressedSize = !libPrefs.preferCompressedSize }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                text = stringResource(textRes),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                lineHeight = 9.sp,
            )
        } else if (compType == PackCompType.SharedLibrary) {
            val textRes = when {
                libPrefs.preferSystemLibs -> "SYSTEM LIBS"
                else -> "APP LIBS"
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { libPrefs.preferSystemLibs = !libPrefs.preferSystemLibs }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                text = textRes,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                lineHeight = 9.sp,
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Text(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 5.dp),
        text = stringResource(R.string.av_info_lib_no_data),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        fontSize = 10.sp,
        lineHeight = 11.sp,
    )
}

@Composable
private fun SectionDivider() {
    Divider(
        modifier = Modifier.padding(vertical = 5.dp).padding(start = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    )
}

@Composable
private fun SectionTitle(compSection: CompSection, listSize: Int) {
    val sectionType = when (compSection) {
        CompSection.Marked -> R.string.av_info_lib_sec_marked
        CompSection.Normal -> R.string.av_info_lib_sec_normal
        CompSection.MinimizedSelf -> R.string.av_info_lib_sec_minimized_self
        CompSection.Minimized -> R.string.av_info_lib_sec_minimized
    }
    Text(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 5.dp),
        text = "${stringResource(sectionType)} $listSize",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 12.sp,
    )
}

@Composable
private fun ItemList(item: PackComponent, sectionIndex: Int, libPrefs: LibPrefs) {
    CompositionLocalProvider(LocalLibPrefs provides libPrefs) {
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

@Composable
private fun LibCheckerMark() {
    Text(
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 3.dp, bottom = 5.dp),
        text = "Powered by LibChecker",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        fontWeight = FontWeight.Medium,
        fontSize = 6.sp,
        lineHeight = 7.sp,
    )
}

@Composable
private fun ChunkedLibItem(items: List<PackComponent>, rowSize: Int) {
    // make items bottom aligned
    Row(modifier = Modifier.padding(horizontal = 19.5.dp), verticalAlignment = Alignment.Bottom) {
        for (item in items) {
            Box(modifier = Modifier.weight(1f)) {
                if (item is MarkedComponent) {
                    MarkedSimpleChunkPartLibItem(item, horizontalPadding = 0.5.dp)
                } else {
                    PlainChunkPartLibItem(item = item, horizontalPadding = 0.5.dp)
                }
            }
        }
        for (i in 0 until (rowSize - items.size)) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}
