package com.kurly.feature.mvi

import com.kerly.core.mvi.Action

internal sealed interface MapAction : Action {
    data object OnFetchLocationClicked : MapAction
    data class OnPermissionResult(val isGranted: Boolean, val shouldShowRationale: Boolean) : MapAction
}