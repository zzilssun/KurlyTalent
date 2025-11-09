package com.kurly.feature.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurly.feature.mvi.MapAction
import com.kurly.feature.mvi.MapEffect
import com.kurly.feature.viewModel.MapViewModel
import kotlinx.coroutines.launch

@Composable
internal fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            // 4. 권한 요청 *결과*를 ViewModel에 Action으로 전달
            val isGranted = permissionsMap.values.all { it }
            viewModel.action(MapAction.OnPermissionResult(isGranted))
        }
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MapEffect.RequestLocationPermission -> {
                    scope.launch {
                        locationPermissionLauncher.launch(permissionsToRequest)
                    }
                }

                is MapEffect.ShowErrorPopup -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    }

    MapContent(
        modifier = modifier,
        state = state,
        snackbarHostState = snackbarHostState,
        onFetchLocationClick = {
            viewModel.action(MapAction.OnFetchLocationClicked)
        },
    )
}