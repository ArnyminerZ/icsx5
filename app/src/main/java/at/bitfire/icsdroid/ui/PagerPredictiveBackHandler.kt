package at.bitfire.icsdroid.ui

import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Provides a predictive back handler for a pager state.
 * The behaviour after the user scrolls back is:
 * - If the pager is in the first page:
 *   [onBack] is called.
 * - Otherwise, the pager is moved to the previous page, and [onPageChanged] is called.
 * This includes the predictive functionality for Android 14+.
 * It means that when the user starts to scroll back, the pager will show this action.
 */
@Composable
@ExperimentalFoundationApi
fun PagerPredictiveBackHandler(
    pagerState: PagerState,
    enabled: Boolean = true,
    onPageChanged: ((page: Int) -> Unit)? = null,
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    var lastProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(
        enabled = enabled,
        onBack = { progress ->
            // code for gesture back started
            Log.i("PagerPredictiveBack", "Started back gesture")
            try {
                progress.collect { event ->
                    // -- code for progress --
                    // ignore gesture for first page
                    if (pagerState.settledPage < 1) return@collect
                    Log.v("PagerPredictiveBack", "progress=${event.progress}, lastProgress=$lastProgress")
                    // obtain the difference between the last updated progress and the current one
                    val delta = lastProgress - event.progress
                    // convert the progress into pixels
                    val deltaPx = delta * screenWidth
                    Log.v("PagerPredictiveBack", "delta=$delta, deltaPx=$deltaPx")
                    // scroll the calculated amount of pixels
                    pagerState.scrollBy(deltaPx)
                    // store the current progress for the next update
                    lastProgress = event.progress
                }

                // -- code for completion --
                Log.i("PagerPredictiveBack", "Finished back gesture")
                lastProgress = 0f
                val page = pagerState.settledPage
                // If not on the first page, animate pager to the previous one
                if (page > 0) {
                    onPageChanged?.invoke(page - 1)
                    scope.launch { pagerState.animateScrollToPage(page - 1) }
                }
                // Otherwise, finish the activity
                else onBack()
            } catch (e: CancellationException) {
                // -- code for cancellation --
                Log.i("PagerPredictiveBack", "Cancelled back gesture")
                lastProgress = 0f
                scope.launch { pagerState.animateScrollToPage(pagerState.settledPage) }
            }
        }
    )
}
