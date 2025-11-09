package com.kurly.feature.viewModel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import com.kerly.core.viewModel.BaseViewModel
import com.kurly.domain.repository.LocationRepository
import com.kurly.domain.usecase.EnqueueLocationUpdateUseCase
import com.kurly.domain.usecase.ObserveLocationWorkUseCase
import com.kurly.feature.mvi.MapAction
import com.kurly.feature.mvi.MapEffect
import com.kurly.feature.mvi.MapReducer
import com.kurly.feature.mvi.MapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
internal class MapViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val enqueueLocationUpdateUseCase: EnqueueLocationUpdateUseCase,
    private val observeLocationWorkUseCase: ObserveLocationWorkUseCase,
    private val locationRepository: LocationRepository,
) : BaseViewModel<MapAction, MapUiState, MapEffect, MapReducer>(
    initialState = MapUiState()
) {

    private var workId: UUID? = null
    private var observeWorkJob: Job? = null

    init {
        // ViewModel 생성 시 DB의 모든 위치 정보를 관찰 시작
        observeAllLocations()
    }

    /**
     * UI로부터 Action을 받아 처리합니다.
     */
    override fun action(action: MapAction) {
        when (action) {
            // '현 위치' 버튼 클릭 시
            is MapAction.OnFetchLocationClicked -> {
                checkLocationPermission()
            }

            // UI로부터 권한 요청 결과를 받았을 때
            is MapAction.OnPermissionResult -> {
                if (action.isGranted) {
                    // 권한 있으면 Work 시작
                    startLocationWork()
                } else {
                    // 권한 없으면 에러 팝업
                    launchInViewModelScope {
                        emitEffect(MapEffect.ShowErrorPopup("위치 권한이 필요합니다."))
                    }
                }
            }
        }
    }

    /**
     * 위치 권한을 확인하고, 결과에 따라 로직을 분기합니다.
     */
    private fun checkLocationPermission() = launchInViewModelScope {
        val finePermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (finePermissionGranted || coarsePermissionGranted) {
            action(MapAction.OnPermissionResult(isGranted = true))
        } else {
            emitEffect(MapEffect.RequestLocationPermission)
        }
    }

    /**
     * Room DB의 위치 정보 변경을 실시간으로 관찰합니다.
     */
    private fun observeAllLocations() {
        launchInViewModelScope {
            locationRepository.getAllLocations().collectLatest { locations ->
                // DB가 변경될 때마다 State를 업데이트
                emitReducer(MapReducer.UpdateLocations(locations))
            }
        }
    }

    /**
     * WorkManager를 통해 위치 정보 업데이트 작업을 시작합니다.
     */
    private fun startLocationWork() = launchWithJob {
        emitReducer(MapReducer.UpdateLoading(true))

        observeWorkJob?.cancel()
        workId = enqueueLocationUpdateUseCase(Unit)
        observeWorkJob = launchWithJob {
            workId?.let { id ->
                observeLocationWorkUseCase(id)
                    .filter { it?.state == WorkInfo.State.RUNNING || it?.state?.isFinished == true }
                    .collectLatest { workInfo ->
                        when (workInfo?.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                emitReducer(MapReducer.UpdateLoading(false))
                            }

                            WorkInfo.State.FAILED -> {
                                emitReducer(MapReducer.UpdateLoading(false))
                                emitEffect(MapEffect.ShowErrorPopup("위치 정보 획득에 실패했습니다."))
                            }

                            WorkInfo.State.RUNNING -> {
                                emitReducer(MapReducer.UpdateLoading(true))
                            }

                            else -> {
                                // Not work
                            }
                        }
                    }
            }
        }
    }

    /**
     * ViewModel 스코프 내에서 발생하는 예외를 처리합니다.
     */
    override suspend fun onHandleException(coroutineContext: CoroutineContext, throwable: Throwable) {
        emitReducer(MapReducer.UpdateLoading(false))
        emitEffect(MapEffect.ShowErrorPopup(throwable.message ?: "알 수 없는 오류가 발생했습니다."))
    }
}