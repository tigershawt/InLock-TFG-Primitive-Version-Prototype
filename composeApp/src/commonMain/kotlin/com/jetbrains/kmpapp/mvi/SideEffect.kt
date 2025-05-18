package com.jetbrains.kmpapp.mvi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SideEffect

class SideEffectHandler<E : SideEffect> {
    private val _sideEffects = MutableSharedFlow<E>()
    val sideEffects: SharedFlow<E> = _sideEffects.asSharedFlow()
    
    suspend fun emitSideEffect(effect: E) {
        _sideEffects.emit(effect)
    }
}