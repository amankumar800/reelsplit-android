package com.reelsplit.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Base ViewModel providing common functionality for all feature ViewModels.
 * Handles UI state management, error handling, and loading states.
 *
 * @param S The type of UI state this ViewModel manages.
 * @param initialState The initial state of the ViewModel.
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {
    
    /**
     * Protected mutable state flow for internal state updates.
     */
    protected val _uiState = MutableStateFlow(initialState)
    
    /**
     * Public immutable state flow for UI observation.
     */
    val uiState: StateFlow<S> = _uiState.asStateFlow()
    
    /**
     * Current state value for quick access.
     * Note: For thread-safe updates, always use [updateState] instead of reading this
     * and writing to [_uiState] directly.
     */
    protected val currentState: S get() = _uiState.value
    
    /**
     * Mutable flow for one-time events (navigation, snackbars, etc.)
     * Uses extraBufferCapacity of 64 to handle rapid event emissions.
     * Older events are dropped if buffer overflows.
     */
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Public flow for UI to collect one-time events.
     */
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    
    /**
     * Loading state for simple loading indicator management.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Error message state for simple error handling.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Map of named jobs for cancellation support.
     */
    private val namedJobs = ConcurrentHashMap<String, Job>()
    
    /**
     * Coroutine exception handler that logs errors and updates error state.
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine exception in ${this::class.simpleName}")
        handleException(throwable)
    }
    
    /**
     * Updates the UI state atomically using [MutableStateFlow.update].
     * This is thread-safe and prevents race conditions.
     *
     * @param reduce Function that transforms the current state to a new state.
     */
    protected fun updateState(reduce: S.() -> S) {
        _uiState.update { it.reduce() }
    }
    
    /**
     * Sets the UI state directly.
     * Use [updateState] for transformations based on current state.
     *
     * @param state The new state to set.
     */
    protected fun setState(state: S) {
        _uiState.value = state
    }
    
    /**
     * Sets the loading state.
     *
     * @param loading True if loading, false otherwise.
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Sets an error message.
     *
     * @param message The error message, or null to clear.
     */
    protected fun setError(message: String?) {
        _errorMessage.value = message
    }
    
    /**
     * Clears the current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Emits a one-time UI event.
     * Uses tryEmit to avoid suspension.
     *
     * @param event The event to emit.
     * @return True if the event was emitted, false if buffer was full.
     */
    protected fun emitEvent(event: UiEvent): Boolean {
        return _events.tryEmit(event)
    }
    
    /**
     * Emits a one-time UI event, suspending if necessary.
     * Prefer [emitEvent] for non-critical events.
     *
     * @param event The event to emit.
     */
    protected suspend fun emitEventSuspend(event: UiEvent) {
        _events.emit(event)
    }
    
    /**
     * Launches a coroutine with automatic error handling.
     *
     * @param dispatcher The dispatcher to use (defaults to Main).
     * @param block The suspend block to execute.
     * @return The Job for the launched coroutine.
     */
    protected fun launchSafe(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job = viewModelScope.launch(dispatcher + exceptionHandler) {
        block()
    }
    
    /**
     * Launches a named coroutine with automatic error handling.
     * If a job with the same name is already running, it will be cancelled first.
     * Useful for operations that shouldn't run in parallel (e.g., search).
     *
     * @param name The unique name for this job.
     * @param dispatcher The dispatcher to use (defaults to Main).
     * @param block The suspend block to execute.
     * @return The Job for the launched coroutine.
     */
    protected fun launchNamed(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job {
        // Cancel existing job with same name
        namedJobs[name]?.cancel()
        
        val job = viewModelScope.launch(dispatcher + exceptionHandler) {
            block()
        }
        
        namedJobs[name] = job
        
        // Clean up when job completes
        job.invokeOnCompletion {
            namedJobs.remove(name, job)
        }
        
        return job
    }
    
    /**
     * Cancels a named job if it exists.
     *
     * @param name The name of the job to cancel.
     */
    protected fun cancelJob(name: String) {
        namedJobs[name]?.cancel()
        namedJobs.remove(name)
    }
    
    /**
     * Launches a coroutine with loading state management and error handling.
     * Loading state is automatically set to true before execution and false after.
     *
     * @param dispatcher The dispatcher to use (defaults to Main).
     * @param block The suspend block to execute.
     * @return The Job for the launched coroutine.
     */
    protected fun launchWithLoading(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job = viewModelScope.launch(dispatcher + exceptionHandler) {
        try {
            setLoading(true)
            block()
        } finally {
            setLoading(false)
        }
    }
    
    /**
     * Launches a coroutine on IO dispatcher with automatic error handling.
     * Convenience method for IO-bound operations.
     *
     * @param block The suspend block to execute.
     * @return The Job for the launched coroutine.
     */
    protected fun launchIO(block: suspend () -> Unit): Job =
        launchSafe(Dispatchers.IO, block)
    
    /**
     * Launches a coroutine on Default dispatcher with automatic error handling.
     * Convenience method for CPU-bound operations.
     *
     * @param block The suspend block to execute.
     * @return The Job for the launched coroutine.
     */
    protected fun launchDefault(block: suspend () -> Unit): Job =
        launchSafe(Dispatchers.Default, block)
    
    /**
     * Handles exceptions caught by the exception handler.
     * Override in subclasses for custom error handling.
     *
     * @param throwable The exception that was caught.
     */
    protected open fun handleException(throwable: Throwable) {
        setError(throwable.message ?: "An unexpected error occurred")
    }
    
    /**
     * Called when the ViewModel is cleared.
     * Cancels all named jobs.
     */
    override fun onCleared() {
        super.onCleared()
        namedJobs.values.forEach { it.cancel() }
        namedJobs.clear()
    }
}
