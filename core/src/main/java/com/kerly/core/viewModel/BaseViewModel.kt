package com.kerly.core.viewModel

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kerly.core.mvi.Action
import com.kerly.core.mvi.Effect
import com.kerly.core.mvi.Reducer
import com.kerly.core.mvi.State
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel<A : Action, S : State, E : Effect, R : Reducer<S>>(
    initialState: S
) : ViewModel() {

    @CallSuper
    abstract fun action(action: A)

    abstract suspend fun onHandleException(coroutineContext: CoroutineContext, throwable: Throwable)

    protected val internalExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        viewModelScope.launch {
            onHandleException(coroutineContext, throwable)
        }
    }

    protected val _state = MutableStateFlow(initialState)

    val state: StateFlow<S>
        get() = _state

    protected val _effect = MutableSharedFlow<E>(replay = 0)

    val effect: SharedFlow<E>
        get() = _effect

    suspend inline fun withCurrentState(action: suspend (S) -> Unit) {
        action(state.value)
    }

    suspend fun emitReducer(reducer: R) {
        withCurrentState { state ->
            _state.update { reducer.reduce(state) }
        }
    }

    suspend fun emitEffect(effect: E) {
        _effect.emit(effect)
    }

    protected inline fun launchWithJob(crossinline block: suspend (scope: CoroutineScope) -> Unit): Job {
        return viewModelScope.launch(internalExceptionHandler) {
            block(this)
        }
    }

    protected inline fun launchInViewModelScope(crossinline block: suspend (scope: CoroutineScope) -> Unit) {
        viewModelScope.launch(internalExceptionHandler) {
            block(this)
        }
    }
}