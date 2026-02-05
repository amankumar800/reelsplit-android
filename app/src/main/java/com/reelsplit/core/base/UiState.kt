package com.reelsplit.core.base

import androidx.compose.runtime.Immutable

/**
 * Generic sealed class representing UI state for any screen.
 * Provides a standardized way to handle loading, success, and error states.
 *
 * @param T The type of data contained in the Success state.
 */
@Immutable
sealed class UiState<out T> {
    
    /**
     * Initial/idle state before any action is taken.
     */
    data object Idle : UiState<Nothing>()
    
    /**
     * Loading state indicating an operation is in progress.
     */
    data object Loading : UiState<Nothing>()
    
    /**
     * Success state containing the result data.
     *
     * @param data The successful result data.
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state containing error information.
     *
     * @param message Human-readable error message.
     * @param throwable Optional throwable for logging/debugging.
     *
     * Note: Both message and throwable are included in equals/hashCode.
     * Different exceptions are considered different error states.
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * Returns true if the state is [Loading].
     */
    val isLoading: Boolean get() = this is Loading
    
    /**
     * Returns true if the state is [Success].
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Returns true if the state is [Error].
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Returns true if the state is [Idle].
     */
    val isIdle: Boolean get() = this is Idle
    
    /**
     * Returns the data if this is a [Success] state, null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data
    
    /**
     * Returns the data if this is a [Success] state, or the default value otherwise.
     *
     * @param default The default value to return if not in Success state.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = (this as? Success)?.data ?: default
    
    /**
     * Returns the data if this is a [Success] state, or computes a default value otherwise.
     *
     * @param defaultProvider Function to compute the default value.
     */
    inline fun getOrElse(defaultProvider: () -> @UnsafeVariance T): T = 
        (this as? Success)?.data ?: defaultProvider()
    
    /**
     * Returns the error message if this is an [Error] state, null otherwise.
     */
    fun errorMessageOrNull(): String? = (this as? Error)?.message
    
    /**
     * Returns the throwable if this is an [Error] state, null otherwise.
     */
    fun throwableOrNull(): Throwable? = (this as? Error)?.throwable
    
    /**
     * Maps the success data to a new type.
     *
     * @param transform Function to transform the data.
     * @return New UiState with transformed data, or same state if not Success.
     */
    inline fun <R> map(transform: (T) -> R): UiState<R> = when (this) {
        is Idle -> Idle
        is Loading -> Loading
        is Success -> Success(transform(data))
        is Error -> Error(message, throwable)
    }
    
    /**
     * Flat maps the success data to a new UiState.
     *
     * @param transform Function to transform the data into a new UiState.
     * @return The transformed UiState, or same state if not Success.
     */
    inline fun <R> flatMap(transform: (T) -> UiState<R>): UiState<R> = when (this) {
        is Idle -> Idle
        is Loading -> Loading
        is Success -> transform(data)
        is Error -> Error(message, throwable)
    }
    
    /**
     * Executes the given block if this is an [Idle] state.
     */
    inline fun onIdle(action: () -> Unit): UiState<T> {
        if (this is Idle) action()
        return this
    }
    
    /**
     * Executes the given block if this is a [Success] state.
     */
    inline fun onSuccess(action: (T) -> Unit): UiState<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Executes the given block if this is an [Error] state.
     */
    inline fun onError(action: (String, Throwable?) -> Unit): UiState<T> {
        if (this is Error) action(message, throwable)
        return this
    }
    
    /**
     * Executes the given block if this is a [Loading] state.
     */
    inline fun onLoading(action: () -> Unit): UiState<T> {
        if (this is Loading) action()
        return this
    }
    
    companion object {
        /**
         * Creates a Success state from data.
         */
        fun <T> success(data: T): UiState<T> = Success(data)
        
        /**
         * Creates an Error state from a message.
         */
        fun error(message: String, throwable: Throwable? = null): UiState<Nothing> = 
            Error(message, throwable)
        
        /**
         * Creates an Error state from a Throwable.
         */
        fun fromThrowable(throwable: Throwable): UiState<Nothing> = 
            Error(throwable.message ?: "An unexpected error occurred", throwable)
        
        /**
         * Returns the idle state.
         */
        fun <T> idle(): UiState<T> = Idle
        
        /**
         * Returns the loading state.
         */
        fun <T> loading(): UiState<T> = Loading
    }
}
