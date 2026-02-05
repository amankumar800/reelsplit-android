package com.reelsplit.core.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn as kotlinStateIn
import kotlinx.coroutines.launch

/**
 * Extension functions for Kotlin Flow.
 */

/**
 * Collects the flow within the given coroutine scope.
 * This is a convenience wrapper around launching a coroutine to collect.
 * 
 * @param scope The [CoroutineScope] to collect in.
 * @param action The action to perform on each emitted value.
 * @return The [Job] representing the collection coroutine, allowing cancellation.
 */
inline fun <T> Flow<T>.collectIn(
    scope: CoroutineScope,
    crossinline action: suspend (T) -> Unit
): Job {
    return scope.launch {
        collect { value ->
            action(value)
        }
    }
}

/**
 * Converts this Flow into a StateFlow with the given initial value.
 * Uses [SharingStarted.WhileSubscribed] with configurable timeouts
 * to handle configuration changes gracefully.
 * 
 * @param scope The [CoroutineScope] for sharing.
 * @param initialValue The initial value of the StateFlow.
 * @param stopTimeoutMillis Time to wait before stopping collection when there are no subscribers.
 *                          Default is 5 seconds to survive configuration changes.
 * @param replayExpirationMillis Time before the cached value expires. Default is [Long.MAX_VALUE] (never expires).
 * @return A [StateFlow] with the given initial value.
 */
fun <T> Flow<T>.stateIn(
    scope: CoroutineScope,
    initialValue: T,
    stopTimeoutMillis: Long = 5000L,
    replayExpirationMillis: Long = Long.MAX_VALUE
): StateFlow<T> {
    return this.kotlinStateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = stopTimeoutMillis,
            replayExpirationMillis = replayExpirationMillis
        ),
        initialValue = initialValue
    )
}
