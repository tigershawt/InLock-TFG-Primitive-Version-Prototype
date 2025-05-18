package com.jetbrains.kmpapp.mvi

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MviModel<S : MviState, I : MviIntent, E : SideEffect> : ScreenModel {
    
    private val _state: MutableStateFlow<S> by lazy { MutableStateFlow(initialState()) }
    val state: StateFlow<S> by lazy { _state.asStateFlow() }
    
    protected val sideEffectHandler = SideEffectHandler<E>()
    val sideEffects: SharedFlow<E> get() = sideEffectHandler.sideEffects
    
    abstract fun initialState(): S
    
    fun processIntent(intent: I) {
        launchInScope {
            handleIntent(intent)
        }
    }
    
    protected abstract suspend fun handleIntent(intent: I)
    
    protected fun updateState(reducer: (S) -> S) {
        _state.update(reducer)
    }
    
    protected fun launchInScope(block: suspend () -> Unit) {
        screenModelScope.launch {
            block()
        }
    }
    
    protected suspend fun emitSideEffect(effect: E) {
        sideEffectHandler.emitSideEffect(effect)
    }
}