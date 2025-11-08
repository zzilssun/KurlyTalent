package com.kerly.core.mvi

interface Reducer<S : State> {
    fun reduce(state: S): S
}