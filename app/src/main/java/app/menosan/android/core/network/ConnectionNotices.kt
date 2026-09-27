package app.menosan.android.core.network

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

const val CONNECTION_SETTLE_MILLIS = 1_500L

@OptIn(FlowPreview::class)
fun Flow<Boolean>.connectionNotices(settleMillis: Long = CONNECTION_SETTLE_MILLIS): Flow<Boolean> =
    debounce(settleMillis)
        .distinctUntilChanged()
        .withIndex()
        .filter { (index, online) -> index > 0 || !online }
        .map { it.value }
