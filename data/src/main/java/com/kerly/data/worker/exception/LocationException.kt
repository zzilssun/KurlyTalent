package com.kerly.data.worker.exception

import java.io.IOException

sealed class LocationException(message: String) : IOException(message) {
    class PermissionDenied : LocationException("위치 권한이 없습니다.")
    class FailToFetch : LocationException("위치 정보를 가져오는 데 실패했습니다.")
}