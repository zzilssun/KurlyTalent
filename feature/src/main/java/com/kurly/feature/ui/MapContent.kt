package com.kurly.feature.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.kurly.feature.mvi.MapUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapContent(
    state: MapUiState,
    snackbarHostState: SnackbarHostState,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    onMapLoaded: () -> Unit,
    onFetchLocationClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "KurlyTalent",
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GoogleMap(
                    modifier = Modifier
                        .weight(1f),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = onMapLoaded,
                ) {
                    state.locations.forEach { location ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(location.latitude, location.longitude)
                            ),
                            title = "위치 ID: ${location.id}",
                            snippet = "시간: ${location.timestamp.toFormattedString()}"
                        )
                    }
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = onFetchLocationClick,
                ) {
                    Text(text = "현 위치")
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun Long.toFormattedString(): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

@Preview
@Composable
private fun PreviewMapContent() {
    Surface {
        MapContent(
            state = MapUiState(),
            snackbarHostState = SnackbarHostState(),
            cameraPositionState = CameraPositionState(),
            modifier = Modifier.fillMaxSize(),
            onMapLoaded = {},
            onFetchLocationClick = {},
        )
    }
}