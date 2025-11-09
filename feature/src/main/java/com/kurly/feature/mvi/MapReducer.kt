package com.kurly.feature.mvi

import com.kerly.core.mvi.Reducer
import com.kurly.domain.model.LocationInfo

internal sealed interface MapReducer : Reducer<MapUiState> {
    data class UpdateLoading(val isLoading: Boolean) : MapReducer {
        override fun reduce(state: MapUiState): MapUiState = state.copy(isLoading = isLoading)
    }

    data class UpdateLocations(val locations: List<LocationInfo>) : MapReducer {
        override fun reduce(state: MapUiState): MapUiState = state.copy(locations = locations)
    }
}