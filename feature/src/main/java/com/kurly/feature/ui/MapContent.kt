package com.kurly.feature.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.kurly.domain.model.LocationInfo
import com.kurly.feature.mvi.MapUiState

@Composable
internal fun MapContent(
    state: MapUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onFetchLocationClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 요구사항: 지도뷰에 마커 표시 (지금은 목록으로 대체)
                Text(text = "저장된 위치 목록", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                // 5. State의 locations를 사용해 UI 그리기
                LocationList(locations = state.locations)
            }

            // 6. '현 위치' 버튼
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                onClick = onFetchLocationClick,
            ) {
                Text(text = "현 위치")
            }

            // 8. State의 isLoading에 따른 로딩 인디케이터
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun LocationList(locations: List<LocationInfo>) {
    if (locations.isEmpty()) {
        Text("저장된 위치 정보가 없습니다.")
    } else {
        LazyColumn {
            items(locations) { location ->
                Text(
                    text = "ID: ${location.id}, " +
                            "Lat: ${location.latitude}, " +
                            "Lon: ${location.longitude}, " +
                            "Time: ${location.timestamp}"
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMapContent() {
    Surface {
        MapContent(
            state = MapUiState(),
            snackbarHostState = SnackbarHostState(),
            onFetchLocationClick = {}
        )
    }
}