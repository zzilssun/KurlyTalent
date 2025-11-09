package com.kurly.feature.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.kurly.feature.mvi.MapAction
import com.kurly.feature.mvi.MapEffect
import com.kurly.feature.viewModel.MapViewModel
import kotlinx.coroutines.launch

@Composable
internal fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
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
            val isGranted = permissionsMap.values.all { it }
            if (isGranted) {
                viewModel.action(
                    MapAction.OnPermissionResult(
                        isGranted = true,
                        shouldShowRationale = false
                    )
                )
            } else {
                val activity = context.findActivity()
                val shouldShowRationale = if (activity != null) {
                    permissionsToRequest.any {
                        activity.shouldShowRequestPermissionRationale(it)
                    }
                } else {
                    true
                }

                viewModel.action(
                    MapAction.OnPermissionResult(
                        isGranted = false,
                        shouldShowRationale = shouldShowRationale
                    )
                )
            }
        }
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 10f)
    }

    var isMapLoaded by remember { mutableStateOf(false) }

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

                MapEffect.GoToAppSettings -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "위치 권한이 영구적으로 거부되었습니다.",
                            actionLabel = "설정으로 이동",
                            withDismissAction = true
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.locations, isMapLoaded) {
        if (isMapLoaded && state.locations.isNotEmpty()) {
            state.locations.firstOrNull()?.let { latestLocation ->
                // 가장 최신 위치로 카메라를 부드럽게 이동
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(latestLocation.latitude, latestLocation.longitude), 15f
                    )
                )
            }
        }
    }

    MapContent(
        state = state,
        snackbarHostState = snackbarHostState,
        cameraPositionState = cameraPositionState,
        modifier = modifier,
        onMapLoaded = {
            isMapLoaded = true
        },
        onFetchLocationClick = {
            viewModel.action(MapAction.OnFetchLocationClicked)
        },
    )
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}