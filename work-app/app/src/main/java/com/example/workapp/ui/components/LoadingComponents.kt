package com.example.workapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A wrapper composable that handles the smooth transition between a loading state (skeleton)
 * and the actual content using a crossfade effect.
 *
 * @param isLoading Whether the content is currently loading.
 * @param modifier Modifier to be applied to the container.
 * @param skeletonContent The content to display while loading (e.g., SkeletonLoader).
 * @param content The actual content to display when not loading.
 */
@Composable
fun FadeInLoadingContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    skeletonContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = isLoading,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        },
        label = "LoadingContentTransition"
    ) { loading ->
        if (loading) {
            skeletonContent()
        } else {
            content()
        }
    }
}
