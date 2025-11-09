package com.kurly.feature.mvi

import com.kerly.core.mvi.State
import com.kurly.domain.model.LocationInfo

internal data class MapUiState(
    val isLoading: Boolean = false,
    val locations: List<LocationInfo> = emptyList()
) : State