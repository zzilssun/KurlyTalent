package com.kurly.feature.mvi

import com.kerly.core.mvi.Effect

internal sealed interface MapEffect : Effect {
    data object RequestLocationPermission : MapEffect
    data class ShowErrorPopup(val message: String) : MapEffect
    data object GoToAppSettings : MapEffect
}