# 질문 1
요구사항
- '현 위치' 버튼을 클릭하면 현재 위치를 WorkManager로 조회합니다.
- 조회된 위치 정보는 Room 로컬 DB에 저장합니다.
- Room 로컬 DB에 저장된 정보를 가지고 와서 지도뷰에 마커 표시를 합니다.

위와 같은 Android 프로젝트를 만들거야 기본 골자를 구상해보자 일단은 프로젝트 초기화를 하고 싶어
Android Architecture Components 의 구조로 구현을 하고 싶은데 아이디어를 내봐

# 답변 1
좋습니다. 요구하신 기능(WorkManager로 위치 조회, Room에 저장, 지도에 표시)을 Android Architecture Components (AAC) 기반으로 구현하기 위한 초기 프로젝트 골격을 잡아보겠습니다.
핵심 구조는 다음과 같습니다.
1. UI Layer (Activity/Fragment + ViewModel): 사용자의 '현 위치' 버튼 클릭을 받고, ViewModel을 통해 WorkManager 작업을 요청합니다. 또한 ViewModel로부터 Room DB의 위치 데이터 변경을 Flow (또는 LiveData)로 구독하여 지도에 마커를 그립니다.
2. Domain Layer (WorkManager): ViewModel의 요청을 받아 백그라운드에서 FusedLocationProviderClient를 사용해 실제 위치를 조회합니다.
3. Data Layer (Repository + Room): WorkManager는 조회된 위치를 Room DB에 저장합니다. Repository는 Room의 DAO를 통해 이 데이터를 Flow로 노출시켜 ViewModel이 구독할 수 있게 합니다.
4. Dependency Injection (Hilt): 이 모든 구성 요소(DB, DAO, Repository, LocationClient 등)를 Hilt를 사용해 주입하여 의존성을 관리합니다.

---

# 질문 2
요구사항의 "조회된 위치 정보는 Room 로컬 DB에 저장합니다." 부분을 구현해보자.
안드로이드 앱 아키텍처에 맞도록 구현을 할거고 hilt 를 통해서 의존성 주입을 사용할거야.
데이터베이스와 Repository 부분을 생성해서 구현해봐

# 대답 2
좋습니다\! 요구사항의 "조회된 위치 정보는 Room 로컬 DB에 저장합니다" 부분을 함께 구현해 보시죠. 😄

우리가 사용할 **클린 아키텍처**와 **Hilt**를 기반으로 데이터베이스(Room)와 Repository 레이어를 구축해 보겠습니다.

전체적인 흐름은 이렇습니다:

1.  **Room Entity:** 데이터베이스 테이블을 정의하는 데이터 클래스를 만듭니다.
2.  **Room DAO (Data Access Object):** 데이터베이스에 접근 (CRUD)하는 메서드를 정의하는 인터페이스를 만듭니다.
3.  **Room Database:** 데이터베이스 홀더 클래스를 만들고, Entity와 DAO를 등록합니다.
4.  **Repository:** DAO를 사용하여 실제 데이터 로직을 처리하는 Repository 인터페이스와 구현체를 만듭니다.
5.  **Hilt Modules:** Room Database, DAO, Repository를 앱 전역에 주입할 수 있도록 Hilt 모듈을 설정합니다.

-----

## 1\. Room Entity (위치 정보)

먼저, Room에 저장할 위치 정보(`Location`)를 나타내는 Entity 클래스를 정의합니다. `data` 레이어의 `local.model` 같은 패키지에 위치시키면 좋습니다.

```kotlin
// data/local/model/LocationEntity.kt

package com.example.kurlytalent.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 데이터베이스에 저장될 위치 정보 엔티티
 *
 * @param id 자동 생성되는 고유 ID
 * @param latitude 위도
 * @param longitude 경도
 * @param timestamp 정보가 저장된 시간 (millis)
 */
@Entity(tableName = "location_history")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
```

* `@Entity` 어노테이션을 사용해 Room이 이 클래스를 테이블로 인식하도록 합니다.
* `@PrimaryKey(autoGenerate = true)`로 `id`를 자동 증가하는 기본 키로 설정했습니다.
* 요구사항에 따라 위도, 경도, 그리고 타임스탬프를 저장합니다.

-----

## 2\. Room DAO (Data Access Object)

다음으로, `LocationEntity`에 접근할 수 있는 메서드를 정의하는 DAO 인터페이스를 만듭니다.

```kotlin
// data/local/dao/LocationDao.kt

package com.example.kurlytalent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kurlytalent.data.local.model.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    /**
     * 새로운 위치 정보를 데이터베이스에 삽입합니다.
     * 충돌이 발생하면 기존 데이터를 덮어씁니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    /**
     * 저장된 모든 위치 정보를 시간순(최신순)으로 조회합니다.
     * Flow를 반환하여 데이터 변경 시 자동으로 UI에 반영되도록 합니다.
     */
    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>
}
```

* `@Dao` 어노테이션을 붙입니다.
* `insertLocation`: 위치 정보를 저장하는 `suspend` 함수입니다.
* `getAllLocations`: 저장된 모든 위치 정보를 `Flow`로 반환합니다. 이렇게 하면 Room DB가 변경될 때마다 새로운 리스트를 방출(emit)하여, ViewModel과 UI가 항상 최신 상태를 유지할 수 있습니다 (Stateful 한 UI 작성에 유리합니다).

-----

## 3\. Room Database

이제 `LocationEntity`와 `LocationDao`를 포함하는 `RoomDatabase` 추상 클래스를 정의합니다.

```kotlin
// data/local/AppDatabase.kt

package com.example.kurlytalent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kurlytalent.data.local.dao.LocationDao
import com.example.kurlytalent.data.local.model.LocationEntity

@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false // 스키마 내보내기 비활성화 (프로덕션에서는 true 권장)
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
```

* `@Database` 어노테이션에 `entities`와 `version`을 명시합니다.
* 추상 메서드로 `locationDao()`를 선언하여 DAO 인스턴스를 제공합니다.

-----

## 4\. Repository (Interface & Implementation)

클린 아키텍처에 따라 `domain` 레이어에 Repository 인터페이스를, `data` 레이어에 구현체를 만듭니다.

### 4-1. Repository Interface (Domain)

```kotlin
// domain/repository/LocationRepository.kt

package com.example.kurlytalent.domain.repository

import com.example.kurlytalent.data.local.model.LocationEntity
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    /**
     * 새로운 위치 정보를 로컬 DB에 저장합니다.
     */
    suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long)

    /**
     * 로컬 DB에 저장된 모든 위치 정보 목록을 Flow로 가져옵니다.
     */
    fun getAllLocations(): Flow<List<LocationEntity>>
}
```

* ViewModel(혹은 UseCase)에서는 이 인터페이스에만 의존하게 됩니다.
* 파라미터로 `LocationEntity` 대신 원시값(primitive)을 받아, `domain` 레이어가 `data` 레이어의 `model`에 직접 의존하지 않도록 설계할 수도 있습니다. (여기서는 UseCase에서 Entity로 변환한다고 가정하고 `LocationEntity`를 사용하거나, 위처럼 원시값을 받도록 설계할 수 있습니다. 여기서는 UseCase의 편의를 위해 원시값을 받도록 수정했습니다.)

### 4-2. Repository Implementation (Data)

```kotlin
// data/repository/LocationRepositoryImpl.kt

package com.example.kurlytalent.data.repository

import com.example.kurlytalent.data.local.dao.LocationDao
import com.example.kurlytalent.data.local.model.LocationEntity
import com.example.kurlytalent.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    override suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long) {
        val locationEntity = LocationEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocation(locationEntity)
    }

    override fun getAllLocations(): Flow<List<LocationEntity>> {
        return locationDao.getAllLocations()
    }
}
```

* `@Inject constructor`를 통해 `LocationDao`를 Hilt로부터 주입받습니다.
* `LocationRepository` 인터페이스를 구현하며, 실제 로직은 `locationDao`의 메서드를 호출합니다.

-----

## 5\. Hilt Modules (DI)

마지막으로, 위에서 만든 `AppDatabase`, `LocationDao`, `LocationRepository`를 Hilt가 주입할 수 있도록 모듈을 설정합니다. `di` 패키지를 만들어 관리합니다.

### 5-1. Database Module

```kotlin
// di/DatabaseModule.kt

package com.example.kurlytalent.di

import android.content.Context
import androidx.room.Room
import com.example.kurlytalent.data.local.AppDatabase
import com.example.kurlytalent.data.local.dao.LocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kurly_talent_db" // 데이터베이스 이름
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: AppDatabase): LocationDao {
        return database.locationDao()
    }
}
```

* `@InstallIn(SingletonComponent::class)`: 앱 전역에서 싱글톤으로 인스턴스를 관리합니다.
* `provideAppDatabase`: `Room.databaseBuilder`를 사용해 `AppDatabase` 인스턴스를 생성합니다.
* `provideLocationDao`: `AppDatabase` 인스턴스를 받아 `LocationDao`를 제공합니다.

### 5-2. Repository Module

```kotlin
// di/RepositoryModule.kt

package com.example.kurlytalent.di

import com.example.kurlytalent.data.repository.LocationRepositoryImpl
import com.example.kurlytalent.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
}
```

* `@Binds` 어노테이션을 사용하여 `LocationRepository` 인터페이스의 구현체로 `LocationRepositoryImpl`를 Hilt에 알려줍니다.

-----

이제 준비가 끝났습니다\! 🥳

이제 `domain` 레이어에서 `UseCase`를 만들고 `LocationRepository`를 주입받아 사용하거나, `ViewModel`에서 직접 `LocationRepository`를 주입받아 `saveLocation` 및 `getAllLocations`를 호출하여 MVI 상태를 업데이트할 수 있습니다.

다음 단계가 필요하면 언제든지 말씀해 주세요\!

---

# 질문 3
현재 모듈은 app, data, domain, feature(ui) 로 나뉘어져 있는데 이를 고려해서 구조를 다시 고려해봐

# 대답 3
아주 좋은 지적입니다\! 👍

`app`, `data`, `domain`, `feature`로 모듈이 분리된 환경은 클린 아키텍처를 구현하는 이상적인 구조입니다. 이전 코드를 이 모듈 구조에 맞게 재배치하고, 모듈 간의 의존성 원칙(Domain -\> Data (X), Data -\> Domain (O))을 철저히 지키도록 수정해 보겠습니다.

가장 큰 변경점은 `domain` 모듈은 순수 Kotlin 모듈이어야 하므로, Room Entity(`LocationEntity`)를 직접 알면 안 된다는 것입니다.

이를 위해 **Domain Model**(`LocationInfo`)을 새로 정의하고, `data` 모듈에서 **Mapper**를 사용해 `LocationEntity`와 `LocationInfo`를 변환하는 작업을 추가하겠습니다.

-----

## 1\. `domain` 모듈 (Pure Kotlin Module)

`domain` 모듈은 안드로이드 프레임워크나 Room 같은 외부 라이브러리에 의존하지 않습니다.

### 1-1. Domain Model

`feature` 모듈(UI)과 `domain` 모듈(UseCase)에서 사용할 순수한 데이터 클래스입니다.

```kotlin
// domain/src/main/java/com/example/kurlytalent/domain/model/LocationInfo.kt

package com.example.kurlytalent.domain.model

/**
 * Domain 레이어에서 사용하는 순수한 위치 정보 데이터
 * (UI 및 UseCase에서 사용)
 */
data class LocationInfo(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
```

### 1-2. Repository Interface

ViewModel(UseCase)이 의존할 레포지토리 *인터페이스*입니다. Domain Model(`LocationInfo`)을 사용합니다.

```kotlin
// domain/src/main/java/com/example/kurlytalent/domain/repository/LocationRepository.kt

package com.example.kurlytalent.domain.repository

import com.example.kurlytalent.domain.model.LocationInfo
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    /**
     * 새로운 위치 정보를 로컬 DB에 저장합니다.
     */
    suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long)

    /**
     * 로컬 DB에 저장된 모든 위치 정보 목록을 Flow로 가져옵니다.
     */
    fun getAllLocations(): Flow<List<LocationInfo>>
}
```

-----

## 2\. `data` 모듈 (Android Library Module)

`data` 모듈은 `domain` 모듈에 **의존합니다 (`implementation project(":domain")`)**. Room, Retrofit 등 실제 데이터 소스를 다룹니다.

### 2-1. Room Entity & DAO (변경 없음)

이전과 동일하게 `data` 모듈 내에 위치합니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/data/local/model/LocationEntity.kt

package com.example.kurlytalent.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
```

```kotlin
// data/src/main/java/com/example/kurlytalent/data/local/dao/LocationDao.kt

package com.example.kurlytalent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kurlytalent.data.local.model.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>
}
```

### 2-2. AppDatabase (변경 없음)

```kotlin
// data/src/main/java/com/example/kurlytalent/data/local/AppDatabase.kt

package com.example.kurlytalent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kurlytalent.data.local.dao.LocationDao
import com.example.kurlytalent.data.local.model.LocationEntity

@Database(entities = [LocationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
```

### 2-3. Mapper (신규)

`data` 모듈의 `LocationEntity`를 `domain` 모듈의 `LocationInfo`로 변환할 Mapper입니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/data/mapper/LocationMapper.kt

package com.example.kurlytalent.data.mapper

import com.example.kurlytalent.data.local.model.LocationEntity
import com.example.kurlytalent.domain.model.LocationInfo

/**
 * Data Layer의 Entity를 Domain Layer의 Model로 변환합니다.
 */
fun LocationEntity.toDomain(): LocationInfo {
    return LocationInfo(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.timestamp
    )
}

/**
 * Domain Layer의 Model을 Data Layer의 Entity로 변환합니다.
 * (필요시 사용. 이 예제에서는 saveLocation에서 Entity를 직접 생성합니다.)
 */
fun LocationInfo.toEntity(): LocationEntity {
    return LocationEntity(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.timestamp
    )
}
```

### 2-4. Repository Implementation (수정)

`LocationRepository` 인터페이스를 구현합니다. DAO에서 받은 `LocationEntity`를 Mapper를 사용해 `LocationInfo`로 변환하여 반환합니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/data/repository/LocationRepositoryImpl.kt

package com.example.kurlytalent.data.repository

import com.example.kurlytalent.data.local.dao.LocationDao
import com.example.kurlytalent.data.local.model.LocationEntity
import com.example.kurlytalent.data.mapper.toDomain
import com.example.kurlytalent.domain.model.LocationInfo
import com.example.kurlytalent.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    override suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long) {
        val locationEntity = LocationEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocation(locationEntity)
    }

    override fun getAllLocations(): Flow<List<LocationInfo>> {
        // DAO로부터 Entity Flow를 받고,
        // map 연산자를 사용해 List<LocationEntity>를 List<LocationInfo>로 변환합니다.
        return locationDao.getAllLocations().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
}
```

### 2-5. Hilt Modules (Data 모듈 내 위치)

DI 모듈들도 `data` 모듈 내에 두어, `data` 모듈이 자신의 의존성 주입 방법을 스스로 제공하도록 하는 것이 좋습니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/di/DatabaseModule.kt

package com.example.kurlytalent.di

import android.content.Context
import androidx.room.Room
import com.example.kurlytalent.data.local.AppDatabase
import com.example.kurlytalent.data.local.dao.LocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kurly_talent_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: AppDatabase): LocationDao {
        return database.locationDao()
    }
}
```

```kotlin
// data/src/main/java/com/example/kurlytalent/di/RepositoryModule.kt

package com.example.kurlytalent.di

import com.example.kurlytalent.data.repository.LocationRepositoryImpl
import com.example.kurlytalent.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
}
```

-----

## 3\. `feature` 및 `app` 모듈

* **`feature` 모듈:** `feature` 모듈은 `domain` 모듈에 의존합니다 (`implementation project(":domain")`).
    * `feature` 모듈의 ViewModel은 `domain` 모듈의 `LocationRepository` (인터페이스)를 주입받아 사용합니다.
    * ViewModel은 `LocationInfo` (Domain Model)를 UI(Composable)에 전달합니다.
* **`app` 모듈:** `app` 모듈은 `data`, `domain`, `feature` 모듈 모두에 의존합니다.
    * `@HiltAndroidApp` 어노테이션을 가진 `Application` 클래스가 이곳에 위치합니다.
    * `data` 모듈에 정의된 Hilt 모듈(`DatabaseModule`, `RepositoryModule`)이 `app` 모듈의 빌드 타임에 포함되어 앱 전역에 의존성을 주입하게 됩니다.

이 구조를 통해 각 모듈의 책임이 명확히 분리되었고, 의존성 규칙도 준수하게 되었습니다.

다음으로 UseCase를 구현하거나 `feature` 모듈의 ViewModel을 구현해 볼까요?

---

# 질문 4
위치를 가져와서 DB 에 업데이트 시키는 로직을 구현하자
위치 관련 퍼미션 체크와 퍼미션 체크 이후 위치를 가져와서 디비에 기록을 하는 코드를 구현해야해
위치를 가져올때는 WorkManager 를 통해서 가져와야하고 이후 위치를 가져오게 되면 디비에 기록을 해줘
만약 위치 정보를 가져오다가 오류가 나게 되면 에러 팝업을 띄울건데 throw 로 에러를 전달하면 돼

# 답변 4
알겠습니다. 요구사항에 맞춰 **WorkManager**를 사용해 백그라운드에서 위치를 가져오고, Room DB에 저장하는 로직을 구현해 보겠습니다.

말씀하신 대로 `feature` 모듈의 UI(Composable)에서 권한을 요청하고, ViewModel이 이 결과를 받아 `WorkManager`를 실행시키며, `WorkManager`가 `data` 모듈의 Repository를 통해 DB에 저장하는 흐름으로 구성합니다.

오류 발생 시 `WorkManager`가 예외를 throw 하도록 하여 `WorkInfo`의 상태가 `FAILED`가 되도록 처리하고, ViewModel이 이를 감지하여 `Effect`로 에러 팝업을 띄우도록 하겠습니다.

-----

## 0\. 필요한 라이브러리 추가

먼저 `app` 모듈의 `build.gradle.kts`에 Hilt-WorkManager를, `data` 모듈에 WorkManager와 Play Services Location을 추가해야 합니다.

**`app/build.gradle.kts`**

```kotlin
// ...
dependencies {
    // ...
    // Hilt WorkManager 통합
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
}
```

**`data/build.gradle.kts`**

```kotlin
// ...
dependencies {
    // ...
    // WorkManager (Coroutine 지원)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Google Play Services: Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // Coroutine Play Services (Task를 suspend 함수로 변환)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
```

-----

## 1\. `app` 모듈 (Application 설정)

Hilt-WorkManager를 사용하기 위해 `Application` 클래스가 `Configuration.Provider`를 구현해야 합니다.

**`app/src/main/java/com/example/kurlytalent/KurlyApplication.kt`**

```kotlin
package com.example.kurlytalent

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KurlyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

-----

## 2\. `data` 모듈 (WorkManager 및 Service 구현)

### 2-1. LocationWorker (WorkManager)

Hilt를 통해 `LocationRepository`와 `FusedLocationProviderClient`를 주입받는 `CoroutineWorker`입니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/data/worker/LocationWorker.kt

package com.example.kurlytalent.data.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kurlytalent.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.IOException

// 사용자 정의 예외
class LocationPermissionException : IOException("위치 권한이 없습니다.")
class LocationFetchException : IOException("위치 정보를 가져오는 데 실패했습니다.")

@HiltWorker
class LocationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. 권한이 있는지 다시 한번 확인 (Worker가 실행되는 시점)
            val a = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val b = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (!a && !b) {
                // 이 예외는 FAILED 상태를 유발
                throw LocationPermissionException()
            }

            // 2. 현재 위치 가져오기 (Priority.PRIORITY_HIGH_ACCURACY)
            // Task를 await()으로 suspend 함수처럼 사용
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await() // kotlinx-coroutines-play-services 필요

            if (location != null) {
                // 3. Room DB에 저장
                locationRepository.saveLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis()
                )
                Result.success()
            } else {
                // 위치는 null일 수 있음
                throw LocationFetchException()
            }

        } catch (e: Exception) {
            // throw된 예외는 Result.failure()로 변환됨
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "LocationWorker"
    }
}
```

### 2-2. DI Module (FusedLocationProviderClient 주입)

`data` 모듈의 `di` 패키지에 `FusedLocationProviderClient`와 `WorkManager`를 제공하는 모듈을 추가합니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/di/LocationModule.kt

package com.example.kurlytalent.di

import android.content.Context
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
```

-----

## 3\. `domain` 모듈 (UseCases)

ViewModel이 `data` 모듈의 `WorkManager`에 직접 의존하지 않도록 UseCase를 정의합니다.

### 3-1. UseCase Interfaces

```kotlin
// domain/src/main/java/com/example/kurlytalent/domain/usecase/EnqueueLocationUpdateUseCase.kt

package com.example.kurlytalent.domain.usecase

import java.util.UUID

/**
 * 위치 정보 업데이트 Work를 WorkManager에 등록하는 UseCase
 */
interface EnqueueLocationUpdateUseCase : UseCase<Unit, UUID> {
    override suspend fun invoke(args: Unit): UUID
}

// domain/src/main/java/com/example/kurlytalent/domain/usecase/ObserveLocationWorkUseCase.kt

package com.example.kurlytalent.domain.usecase

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 등록된 Work의 상태를 관찰하는 UseCase
 */
interface ObserveLocationWorkUseCase : UseCase<UUID, Flow<WorkInfo>> {
    override suspend fun invoke(args: UUID): Flow<WorkInfo>
}
```

### 3-2. UseCase Implementations (`data` 모듈)

`data` 모듈에서 `domain`의 UseCase 인터페이스를 구현합니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/data/usecase/EnqueueLocationUpdateUseCaseImpl.kt

package com.example.kurlytalent.data.usecase

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kurlytalent.data.worker.LocationWorker
import com.example.kurlytalent.domain.usecase.EnqueueLocationUpdateUseCase
import java.util.UUID
import javax.inject.Inject

class EnqueueLocationUpdateUseCaseImpl @Inject constructor(
    private val workManager: WorkManager
) : EnqueueLocationUpdateUseCase {

    override suspend fun invoke(args: Unit): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // 네트워크 필요 없음
            .build()

        val locationWorkRequest = OneTimeWorkRequestBuilder<LocationWorker>()
            .setConstraints(constraints)
            .build()

        // 동일한 작업이 이미 실행 중이면 추가하지 않음 (KEEP)
        workManager.enqueueUniqueWork(
            LocationWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            locationWorkRequest
        )
        
        return locationWorkRequest.id
    }
}

// data/src/main/java/com/example/kurlytalent/data/usecase/ObserveLocationWorkUseCaseImpl.kt

package com.example.kurlytalent.data.usecase

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.kurlytalent.domain.usecase.ObserveLocationWorkUseCase
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ObserveLocationWorkUseCaseImpl @Inject constructor(
    private val workManager: WorkManager
) : ObserveLocationWorkUseCase {
    override suspend fun invoke(args: UUID): Flow<WorkInfo> {
        return workManager.getWorkInfoByIdFlow(args)
    }
}
```

### 3-3. DI Binds (`data` 모듈)

`RepositoryModule`에 UseCase 바인딩을 추가합니다.

```kotlin
// data/src/main/java/com/example/kurlytalent/di/RepositoryModule.kt

package com.example.kurlytalent.di

import com.example.kurlytalent.data.repository.LocationRepositoryImpl
import com.example.kurlytalent.data.usecase.EnqueueLocationUpdateUseCaseImpl
import com.example.kurlytalent.data.usecase.ObserveLocationWorkUseCaseImpl
import com.example.kurlytalent.domain.repository.LocationRepository
import com.example.kurlytalent.domain.usecase.EnqueueLocationUpdateUseCase
import com.example.kurlytalent.domain.usecase.ObserveLocationWorkUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
    
    // --- 여기부터 추가 ---
    @Binds
    @Singleton
    abstract fun bindEnqueueLocationUpdateUseCase(
        useCase: EnqueueLocationUpdateUseCaseImpl
    ): EnqueueLocationUpdateUseCase

    @Binds
    @Singleton
    abstract fun bindObserveLocationWorkUseCase(
        useCase: ObserveLocationWorkUseCaseImpl
    ): ObserveLocationWorkUseCase
}
```

---
# 질문 5
ViewModel 과 뷰를 구성해보려고해 단 하나의 진실 원칙에 입각해서 단일 State 를 구성을 하고 그동안 생성된 코드를 활용해서 구현을 해보자

# 답변 5
좋습니다. \*\*단일 진실 원칙(Single Source of Truth)\*\*에 입각하여 `MainState`라는 단일 상태 객체를 두고, `feature` 모듈의 ViewModel과 View(Composable)를 구성해 보겠습니다.

지금까지 우리가 만든 MVI 아키텍처, UseCase, Repository를 모두 활용합니다.

-----

## 1\. MVI 정의 (`MainMVI.kt`)

먼저 `feature` 모듈의 `main` 패키지에 MVI 계약(Contract)을 정의합니다. (이전 단계에서 정의한 내용을 재사용합니다.)

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainMVI.kt

package com.example.kurlytalent.feature.main

import com.example.kurlytalent.domain.model.LocationInfo
import com.example.kurlytalent.mvi.Action
import com.example.kurlytalent.mvi.Effect
import com.example.kurlytalent.mvi.Reducer
import com.example.kurlytalent.mvi.State

// Actions (UI -> ViewModel)
sealed interface MainAction : Action {
    // '현 위치' 버튼 클릭
    data object OnFetchLocationClicked : MainAction
    // UI에서 권한 요청 결과 전달
    data class OnPermissionResult(val isGranted: Boolean) : MainAction
}

// State (ViewModel -> UI)
// 단 하나의 진실 원칙(SSOT)
data class MainState(
    val isLoading: Boolean = false,
    val locations: List<LocationInfo> = emptyList() // DB에서 가져온 위치 목록
) : State

// Effects (ViewModel -> UI, 일회성 이벤트)
sealed interface MainEffect : Effect {
    // UI에게 권한을 요청하라고 명령
    data object RequestLocationPermission : MainEffect
    // 에러 팝업 표시
    data class ShowErrorPopup(val message: String) : MainEffect
}

// Reducer (State 변경 로직)
sealed interface MainReducer : Reducer<MainState> {
    data class UpdateLoading(val isLoading: Boolean) : MainReducer {
        override fun reduce(state: MainState): MainState =
            state.copy(isLoading = isLoading)
    }

    data class UpdateLocations(val locations: List<LocationInfo>) : MainReducer {
        override fun reduce(state: MainState): MainState =
            state.copy(locations = locations)
    }
}
```

* `MainState`는 UI가 알아야 할 모든 정보를 담고 있습니다. (로딩 상태, 위치 목록)

-----

## 2\. ViewModel (`MainViewModel.kt`)

`BaseViewModel`을 상속받아 MVI 로직을 구현합니다. Hilt를 통해 UseCase와 Repository를 주입받습니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainViewModel.kt

package com.example.kurlytalent.feature.main

import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.kurlytalent.domain.repository.LocationRepository
import com.example.kurlytalent.domain.usecase.EnqueueLocationUpdateUseCase
import com.example.kurlytalent.domain.usecase.ObserveLocationWorkUseCase
import com.example.kurlytalent.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val enqueueLocationUpdateUseCase: EnqueueLocationUpdateUseCase,
    private val observeLocationWorkUseCase: ObserveLocationWorkUseCase,
    private val locationRepository: LocationRepository
) : BaseViewModel<MainAction, MainState, MainEffect, MainReducer>(
    initialState = MainState()
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
    override fun action(action: MainAction) {
        when (action) {
            // '현 위치' 버튼 클릭 시
            is MainAction.OnFetchLocationClicked -> {
                launchInViewModelScope {
                    // UI에 권한 요청 Effect 전달
                    emitEffect(MainEffect.RequestLocationPermission)
                }
            }
            
            // UI로부터 권한 요청 결과를 받았을 때
            is MainAction.OnPermissionResult -> {
                if (action.isGranted) {
                    // 권한 있으면 Work 시작
                    startLocationWork()
                } else {
                    // 권한 없으면 에러 팝업
                    launchInViewModelScope {
                        emitEffect(MainEffect.ShowErrorPopup("위치 권한이 필요합니다."))
                    }
                }
            }
        }
    }

    /**
     * Room DB의 위치 정보 변경을 실시간으로 관찰합니다.
     */
    private fun observeAllLocations() {
        launchInViewModelScope {
            locationRepository.getAllLocations().collectLatest { locations ->
                // DB가 변경될 때마다 State를 업데이트
                emitReducer(MainReducer.UpdateLocations(locations))
            }
        }
    }

    /**
     * WorkManager를 통해 위치 정보 업데이트 작업을 시작합니다.
     */
    private fun startLocationWork() = launchWithJob {
        // 1. 로딩 상태 시작
        emitReducer(MainReducer.UpdateLoading(true))
        
        // 2. 이전에 관찰하던 Job이 있다면 취소
        observeWorkJob?.cancel()
        
        // 3. WorkManager에 작업 등록 및 ID 받기
        workId = enqueueLocationUpdateUseCase(Unit)
        log("Work enqueued with ID: $workId")

        // 4. Work 상태 관찰 시작
        observeWorkJob = launchInViewModelScope {
            workId?.let { id ->
                observeLocationWorkUseCase(id)
                    .filter { it.state == WorkInfo.State.RUNNING || it.state.isFinished }
                    .collectLatest { workInfo ->
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                log("Work SUCCEEDED")
                                emitReducer(MainReducer.UpdateLoading(false))
                            }
                            WorkInfo.State.FAILED -> {
                                log("Work FAILED")
                                emitReducer(MainReducer.UpdateLoading(false))
                                emitEffect(MainEffect.ShowErrorPopup("위치 정보 획득에 실패했습니다."))
                            }
                            WorkInfo.State.RUNNING -> {
                                log("Work RUNNING")
                                emitReducer(MainReducer.UpdateLoading(true))
                            }
                            else -> { /* No-op (BLOCKED, CANCELLED 등) */ }
                        }
                    }
            }
        }
    }

    /**
     * ViewModel 스코프 내에서 발생하는 예외를 처리합니다.
     */
    override suspend fun onHandleException(coroutineContext: CoroutineContext, throwable: Throwable) {
        emitReducer(MainReducer.UpdateLoading(false))
        emitEffect(MainEffect.ShowErrorPopup(throwable.message ?: "알 수 없는 오류가 발생했습니다."))
    }
}
```

-----

## 3\. View (`MainScreen.kt`)

`Stateful`한 UI를 위해 `state`를 `collectAsState()`로 구독하고, `Action`을 ViewModel로 전달하며, `Effect`를 처리합니다.

**참고:** `accompanist-permissions` 라이브러리가 필요합니다. `feature` 모듈의 `build.gradle.kts`에 추가해주세요.

```kotlin
// feature/build.gradle.kts
dependencies {
    // ...
    implementation("com.google.accompanist:accompanist-permissions:0.34.0") // (최신 버전 확인)
}
```

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainScreen.kt

package com.example.kurlytalent.feature.main

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kurlytalent.domain.model.LocationInfo
import com.example.kurlytalent.mvi.effect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    // 1. ViewModel의 State를 구독 (Stateful)
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 2. 위치 권한 (FINE + COARSE)
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // 3. ViewModel의 Effect 처리 (일회성 이벤트)
    scope.effect(viewModel) { effect: MainEffect ->
        when (effect) {
            // 3-1. 권한 요청 Effect 수신 시
            is MainEffect.RequestLocationPermission -> {
                scope.launch {
                    // 권한 팝업 표시
                    locationPermissionsState.launchPermissionRequest()
                    // 4. 권한 요청 *결과*를 ViewModel에 Action으로 다시 전달
                    viewModel.action(
                        MainAction.OnPermissionResult(locationPermissionsState.allPermissionsGranted)
                    )
                }
            }
            // 3-2. 에러 팝업 Effect 수신 시
            is MainEffect.ShowErrorPopup -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
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
                onClick = {
                    // 7. 현재 권한 상태 확인
                    if (locationPermissionsState.allPermissionsGranted) {
                        // 7-1. 권한이 이미 있으면, 바로 Action 전달
                        viewModel.action(MainAction.OnPermissionResult(isGranted = true))
                    } else {
                        // 7-2. 권한이 없으면, '버튼 클릭' Action 전달 (Effect가 처리)
                        viewModel.action(MainAction.OnFetchLocationClicked)
                    }
                }
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
fun LocationList(locations: List<LocationInfo>) {
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
```

### 흐름 요약

1.  `MainScreen`이 Composable되면서 `MainViewModel`의 `init`이 호출되고, `observeAllLocations()`가 실행되어 Room DB를 구독합니다.
2.  `MainState`의 `locations`가 `LocationList` Composable에 전달됩니다.
3.  사용자가 "현 위치" 버튼을 클릭합니다.
4.  `onClick`에서 권한을 확인합니다.
    * **권한 있음**: `OnPermissionResult(true)` Action이 ViewModel로 갑니다.
    * **권한 없음**: `OnFetchLocationClicked` Action이 ViewModel로 갑니다.
5.  **(권한 없음 시)** ViewModel이 `RequestLocationPermission` Effect를 보냅니다.
6.  `MainScreen`의 `effect` 블록이 Effect를 받아 `launchPermissionRequest()`를 호출합니다.
7.  권한 팝업이 닫히면, `effect` 블록이 `OnPermissionResult(isGranted)` Action을 ViewModel로 보냅니다.
8.  ViewModel이 `OnPermissionResult`를 받고, `isGranted`가 `true`이면 `startLocationWork()`를 호출합니다.
9.  WorkManager가 실행되고, `LocationWorker`가 DB에 위치를 저장합니다.
10. `observeAllLocations()` (1번에서 실행됨)가 DB 변경을 감지하고 새 목록으로 `MainState`를 업데이트합니다.
11. `MainScreen`이 자동으로 Recompose되어 `LocationList`에 새 위치가 표시됩니다.

이제 지도뷰를 제외한 핵심 로직(권한, WorkManager, Room, MVI)이 완성되었습니다.

---

# 질문 6
rememberMultiplePermissionsState 대신 rememberLauncherForActivityResult 사용해 저 라이브러리는 이제 사용되지 않아

# 답변 6
정확한 지적입니다\! 👏 `accompanist-permissions`는 공식적으로 `androidx.activity`의 `rememberLauncherForActivityResult`로 대체되었습니다.

새로운 API를 사용하고, ViewModel과 UI의 역할을 더 명확하게 분리하는 방향으로 코드를 리팩토링해 보겠습니다.

변경된 핵심 로직은 다음과 같습니다.

1.  **ViewModel**: `@ApplicationContext`를 주입받아, '현 위치' 버튼 클릭 시 **ViewModel이 직접 권한을 확인**합니다.
    * 권한이 **있으면**: `OnPermissionResult(true)` Action을 스스로 호출하여 바로 Work를 시작합니다.
    * 권한이 **없으면**: `RequestLocationPermission` Effect를 UI로 보내 권한 팝업을 띄우도록 요청합니다.
2.  **View (Screen)**: `rememberLauncherForActivityResult`를 사용합니다.
    * `onClick`에서는 `OnFetchLocationClicked` Action만 보냅니다. (UI는 권한 상태를 모름)
    * `RequestLocationPermission` Effect를 받으면 런처를 실행(`launch`)합니다.
    * 런처의 `onResult` 콜백에서 `OnPermissionResult(isGranted)` Action을 ViewModel로 전달합니다.

-----

## 1\. `MainViewModel.kt` (수정)

`@ApplicationContext`를 주입받고 `action` 핸들러 내부 로직을 수정합니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainViewModel.kt

package com.example.kurlytalent.feature.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.kurlytalent.domain.repository.LocationRepository
import com.example.kurlytalent.domain.usecase.EnqueueLocationUpdateUseCase
import com.example.kurlytalent.domain.usecase.ObserveLocationWorkUseCase
import com.example.kurlytalent.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // Hilt 어노테이션 임포트
import kotlinx.coroutines.CoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // 1. ApplicationContext 주입
    private val enqueueLocationUpdateUseCase: EnqueueLocationUpdateUseCase,
    private val observeLocationWorkUseCase: ObserveLocationWorkUseCase,
    private val locationRepository: LocationRepository
) : BaseViewModel<MainAction, MainState, MainEffect, MainReducer>(
    initialState = MainState()
) {

    private var workId: UUID? = null
    private var observeWorkJob: Job? = null

    init {
        observeAllLocations()
    }

    /**
     * UI로부터 Action을 받아 처리합니다.
     */
    override fun action(action: MainAction) {
        when (action) {
            // '현 위치' 버튼 클릭 시
            is MainAction.OnFetchLocationClicked -> {
                // 2. ViewModel이 직접 권한 상태 확인
                checkLocationPermission()
            }
            
            // UI(런처) 또는 ViewModel(checkLocationPermission)로부터 권한 결과를 받았을 때
            is MainAction.OnPermissionResult -> {
                if (action.isGranted) {
                    startLocationWork()
                } else {
                    launchInViewModelScope {
                        emitEffect(MainEffect.ShowErrorPopup("위치 권한이 필요합니다."))
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
            // 3. 권한이 이미 있으면, '결과' 액션을 스스로 호출
            action(MainAction.OnPermissionResult(isGranted = true))
        } else {
            // 4. 권한이 없으면, UI에 권한 요청 Effect 전달
            emitEffect(MainEffect.RequestLocationPermission)
        }
    }

    /**
     * Room DB의 위치 정보 변경을 실시간으로 관찰합니다.
     */
    private fun observeAllLocations() {
        launchInViewModelScope {
            locationRepository.getAllLocations().collectLatest { locations ->
                emitReducer(MainReducer.UpdateLocations(locations))
            }
        }
    }

    /**
     * WorkManager를 통해 위치 정보 업데이트 작업을 시작합니다.
     */
    private fun startLocationWork() = launchWithJob {
        emitReducer(MainReducer.UpdateLoading(true))
        observeWorkJob?.cancel()
        
        workId = enqueueLocationUpdateUseCase(Unit)
        log("Work enqueued with ID: $workId")

        observeWorkJob = launchInViewModelScope {
            workId?.let { id ->
                observeLocationWorkUseCase(id)
                    .filter { it.state == WorkInfo.State.RUNNING || it.state.isFinished }
                    .collectLatest { workInfo ->
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                log("Work SUCCEEDED")
                                emitReducer(MainReducer.UpdateLoading(false))
                            }
                            WorkInfo.State.FAILED -> {
                                log("Work FAILED")
                                emitReducer(MainReducer.UpdateLoading(false))
                                emitEffect(MainEffect.ShowErrorPopup("위치 정보 획득에 실패했습니다."))
                            }
                            WorkInfo.State.RUNNING -> {
                                log("Work RUNNING")
                                emitReducer(MainReducer.UpdateLoading(true))
                            }
                            else -> { /* No-op */ }
                        }
                    }
            }
        }
    }

    override suspend fun onHandleException(coroutineContext: CoroutineContext, throwable: Throwable) {
        emitReducer(MainReducer.UpdateLoading(false))
        emitEffect(MainEffect.ShowErrorPopup(throwable.message ?: "알 수 없는 오류가 발생했습니다."))
    }
}
```

-----

## 2\. `MainScreen.kt` (수정)

`rememberMultiplePermissionsState`를 제거하고 `rememberLauncherForActivityResult`로 대체합니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainScreen.kt

package com.example.kurlytalent.feature.main

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult // 1. 임포트 변경
import androidx.activity.result.contract.ActivityResultContracts // 1. 임포트 변경
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kurlytalent.domain.model.LocationInfo
import com.example.kurlytalent.mvi.effect
// com.google.accompanist.permissions 관련 임포트 모두 제거
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 2. 권한 요청에 필요한 권한 목록
    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // 3. rememberLauncherForActivityResult 정의
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            // 4. 권한 요청 *결과*를 ViewModel에 Action으로 전달
            val isGranted = permissionsMap.values.all { it }
            viewModel.action(MainAction.OnPermissionResult(isGranted))
        }
    )

    // 5. ViewModel의 Effect 처리 (일회성 이벤트)
    scope.effect(viewModel) { effect: MainEffect ->
        when (effect) {
            // 5-1. 권한 요청 Effect 수신 시
            is MainEffect.RequestLocationPermission -> {
                // 런처 실행
                locationPermissionLauncher.launch(permissionsToRequest)
            }
            // 5-2. 에러 팝업 Effect 수신 시
            is MainEffect.ShowErrorPopup -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            
            Column(modifier = Modifier.fillMaxSize()) {
                Text(text = "저장된 위치 목록", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                // 6. State의 locations를 사용해 UI 그리기
                LocationList(locations = state.locations)
            }

            // 7. '현 위치' 버튼
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                onClick = {
                    // 8. 버튼 클릭 시 항상 OnFetchLocationClicked Action 전송
                    // 권한 확인은 ViewModel이 담당
                    viewModel.action(MainAction.OnFetchLocationClicked)
                }
            ) {
                Text(text = "현 위치")
            }

            // 9. State의 isLoading에 따른 로딩 인디케이터
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun LocationList(locations: List<LocationInfo>) {
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
```

이 구조가 훨씬 더 깔끔하고 안드로이드에서 권장하는 최신 패턴을 따릅니다. UI는 이제 권한 상태를 알 필요 없이 ViewModel의 '명령'(`Effect`)에만 반응하면 됩니다.

---

# 질문 7
Manifest 설정도 알려줘

# 답변 7
좋습니다\! 지금까지 논의한 기능(위치, WorkManager, Hilt, 지도)을 정상적으로 동작시키기 위해 `AndroidManifest.xml`에 필요한 설정들을 알려드리겠습니다.

`feature` 모듈 (또는 `app` 모듈)의 `src/main/AndroidManifest.xml` 파일에 아래 내용을 추가하거나 확인해야 합니다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>


    <application
        android:name=".KurlyApplication" android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KurlyTalent"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity" android:exported="true"
            android:theme="@style/Theme.KurlyTalent">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
            
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>

    </application>

</manifest>
```

### 주요 설정 설명

1.  **위치 권한 (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)**

    * `FusedLocationProviderClient`가 `PRIORITY_HIGH_ACCURACY`로 현재 위치를 가져오기 위해 필수입니다.

2.  **포그라운드 서비스 권한 (`FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION`)**

    * `WorkManager`가 신속한 작업(expedited job)으로 위치 조회를 실행할 때 내부적으로 포그라운드 서비스를 사용할 수 있습니다. 이 경우 해당 권한이 필요합니다.

3.  **Hilt Application (`android:name`)**

    * `<application>` 태그의 `android:name` 속성에 Hilt 설정을 마친 `KurlyApplication` 클래스를 지정해야 합니다.

4.  **WorkManager 초기화 (`<provider>`)**

    * `HiltWorkerFactory`를 `Application` 클래스에서 `Configuration.Provider`를 통해 주입하고 있습니다. 이 방식은 `WorkManager`의 기본 초기화(`androidx.startup`)와 충돌할 수 있습니다.
    * 따라서 `tools:node="remove"` 속성을 사용해 기본 `WorkManagerInitializer`를 Manifest에서 제거하고, 우리가 정의한 `Configuration.Provider`가 유일한 설정이 되도록 보장하는 것이 안전합니다.

5.  **지도 API 키 (`<meta-data>`)**

    * 요구사항에 따라 Google, Naver, Kakao 등 원하는 지도 플랫폼을 사용할 수 있습니다.
    * 선택한 플랫폼에 맞는 API 키를 `<meta-data>`로 등록해야 지도가 정상적으로 로드됩니다.

이제 지도뷰 구현만 남았습니다\! 지도 관련 코드를 작성해 볼까요?

---

# 질문 8
지도 관련된 코드도 구현을 해보자
secrets-gradle-plugin 를 통해서 키를 숨겨서 사용하려고 하는데 이걸 사용하는 방법을 함께 서술해줘

# 답변 8
알겠습니다. 요구사항의 마지막 단계인 "Room 로컬 DB에 저장된 정보를 가지고 와서 지도뷰에 마커 표시"를 구현해 보겠습니다.

말씀하신 대로 \*\*`secrets-gradle-plugin`\*\*을 사용하여 Google Maps API 키를 안전하게 관리하는 방법부터 지도 구현까지 함께 진행합니다.

-----

## 1\. 🗺️ `secrets-gradle-plugin` 설정하기

이 플러그인은 `local.properties` 파일에 저장된 키를 빌드 시 `AndroidManifest.xml`이나 `BuildConfig`에 안전하게 주입해 줍니다. `local.properties`는 `.gitignore`에 포함되어 있어 Git에 노출되지 않습니다. (이미 `local.properties`가 `.gitignore`에 포함된 것을 확인했습니다.)

### 1-1. 프로젝트 루트 `build.gradle.kts`

`plugins` 블록에 플러그인 ID를 추가합니다.

```kotlin
// build.gradle.kts (Project)

plugins {
    // ...
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}
```

### 1-2. `feature` 모듈 `build.gradle.kts`

`feature` 모듈(또는 지도를 사용하는 모듈)의 `plugins` 블록에 플러그인을 적용합니다.

```kotlin
// feature/build.gradle.kts

plugins {
    // ...
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}
```

### 1-3. `local.properties` 파일

프로젝트 루트의 `local.properties` 파일에 API 키를 추가합니다. (이 파일은 Git에 올라가지 않습니다.)

```properties
# local.properties

MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
```

### 1-4. `AndroidManifest.xml` (feature 모듈)

`application` 태그 내에 API 키를 참조하는 `<meta-data>`를 추가합니다. 플러그인이 빌드 시 `"${MAPS_API_KEY}"` 부분을 실제 키로 교체합니다.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    ...>
    <application ...>
        
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="${MAPS_API_KEY}" />
            
    </application>
</manifest>
```

-----

## 2\. 지도 라이브러리 추가

Jetpack Compose에서 Google 지도를 사용하기 위한 라이브러리를 `feature` 모듈에 추가합니다.

```kotlin
// feature/build.gradle.kts

dependencies {
    // ... (기존 Hilt, ViewModel, Compose 등)
    
    // Google Maps for Compose
    implementation("com.google.maps.android:maps-compose:4.3.3") // (버전은 최신화 확인)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
}
```

-----

## 3\. `MainScreen.kt`에 지도 구현

`MainState`의 `locations` 리스트를 `GoogleMap` Composable에 바인딩하여 마커를 표시합니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainScreen.kt

package com.example.kurlytalent.feature.main

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column // (제거)
//import androidx.compose.foundation.layout.Spacer // (제거)
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height // (제거)
import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn // (제거)
//import androidx.compose.foundation.lazy.items // (제거)
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme // (제거)
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kurlytalent.domain.model.LocationInfo
import com.example.kurlytalent.mvi.effect
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap // 1. Map 임포트
import com.google.maps.android.compose.Marker // 2. Marker 임포트
import com.google.maps.android.compose.MarkerState // 3. MarkerState 임포트
import com.google.maps.android.compose.rememberCameraPositionState // 4. CameraState 임포트
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
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
            val isGranted = permissionsMap.values.all { it }
            viewModel.action(MainAction.OnPermissionResult(isGranted))
        }
    )

    scope.effect(viewModel) { effect: MainEffect ->
        when (effect) {
            is MainEffect.RequestLocationPermission -> {
                locationPermissionLauncher.launch(permissionsToRequest)
            }
            is MainEffect.ShowErrorPopup -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // --- 5. 지도 카메라 상태 추가 ---
    val cameraPositionState = rememberCameraPositionState {
        // 기본 위치 (서울)
        position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 10f)
    }

    // --- 6. DB 위치 목록(State)이 변경되면 카메라 이동 ---
    LaunchedEffect(state.locations) {
        state.locations.firstOrNull()?.let { latestLocation ->
            // 가장 최신 위치로 카메라를 부드럽게 이동
            cameraPositionState.animate(
                com.google.android.gms.maps.model.CameraUpdateFactory.newLatLngZoom(
                    LatLng(latestLocation.latitude, latestLocation.longitude), 15f
                )
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // .padding(16.dp) // 지도는 전체 화면을 사용하도록 패딩 제거
        ) {

            // --- 7. 지도 뷰 구현 ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // 8. State의 locations 리스트를 순회하며 마커 생성
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

            // '현 위치' 버튼 (하단 중앙, 지도 위에 표시되도록 패딩 추가)
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp), // 버튼에만 패딩 적용
                onClick = {
                    viewModel.action(MainAction.OnFetchLocationClicked)
                }
            ) {
                Text(text = "현 위치")
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// Long 타임스탬프를 읽기 쉬운 문자열로 변환하는 확장 함수
fun Long.toFormattedString(): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

// (LocationList Composable은 지도에 의해 대체되었으므로 제거)
```

### 구현 설명

1.  **API 키 설정 (1-4)**: `secrets-gradle-plugin`을 설정하고 `local.properties`에 키를 추가한 뒤, Manifest에서 `${MAPS_API_KEY}`로 참조했습니다.
2.  **라이브러리 추가 (2)**: `maps-compose` 라이브러리를 `feature` 모듈에 추가했습니다.
3.  **`GoogleMap` Composable (7)**: `MainScreen`의 `Box` 내부에 `GoogleMap` Composable을 추가하여 화면 전체를 채우도록 했습니다.
4.  **카메라 상태 (5)**: `rememberCameraPositionState`를 사용해 지도의 카메라 위치를 기억하고 제어할 수 있도록 했습니다.
5.  **마커 표시 (8)**: `GoogleMap`의 컨텐츠 람다 내에서 `state.locations` 리스트를 `forEach`로 순회하며, 각 `LocationInfo` 객체에 대해 `Marker` Composable을 생성했습니다.
6.  **카메라 이동 (6)**: `LaunchedEffect`를 사용하여 `state.locations`가 변경될 때 (즉, Room DB가 업데이트될 때) `locations` 리스트의 첫 번째(가장 최신) 항목을 찾아 카메라를 해당 위치로 부드럽게(`animate`) 이동시켰습니다.

이제 요구사항의 모든 항목이 구현되었습니다.

---

# 질문 9
java.lang.NullPointerException: CameraUpdateFactory is not initialized
   at com.google.android.gms.common.internal.Preconditions.checkNotNull(com.google.android.gms:play-services-basement@@18.3.0:2)
   at com.google.android.gms.maps.CameraUpdateFactory.zzb(com.google.android.gms:play-services-maps@@19.2.0:1)
   at com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(com.google.android.gms:play-services-maps@@19.2.0:2)
   at com.kurly.feature.ui.MapScreenKt$MapScreen$2$1.invokeSuspend(MapScreen.kt:72)
   at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:34)
   at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
   at androidx.compose.ui.platform.AndroidUiDispatcher.performTrampolineDispatch(AndroidUiDispatcher.android.kt:79)
   at androidx.compose.ui.platform.AndroidUiDispatcher.access$performTrampolineDispatch(AndroidUiDispatcher.android.kt:41)
   at androidx.compose.ui.platform.AndroidUiDispatcher$dispatchCallback$1.run(AndroidUiDispatcher.android.kt:57)
   at android.os.Handler.handleCallback(Handler.java:995)
   at android.os.Handler.dispatchMessage(Handler.java:103)
   at android.os.Looper.loopOnce(Looper.java:248)
   at android.os.Looper.loop(Looper.java:338)
   at android.app.ActivityThread.main(ActivityThread.java:9067)
   at java.lang.reflect.Method.invoke(Native Method)
   at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:593)
   at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)
   Suppressed: kotlinx.coroutines.internal.DiagnosticCoroutineContextException: [androidx.compose.ui.platform.MotionDurationScaleImpl@fc924f2, androidx.compose.runtime.BroadcastFrameClock@712a743, StandaloneCoroutine{Cancelling}@2b9d1c0, AndroidUiDispatcher@186fef9]

위와 같이 초기화 오류가 생기고 있어 어떻게 처리하면 좋을까?

# 답변 9
아, 이 에러는 매우 흔하면서도 까다로운 **'레이스 컨디션(race condition)'** 문제입니다. 😥

## 1\. 🕵️‍♂️ 문제 원인

스택 트레이스를 보면 `MapScreen.kt:72`에서 `CameraUpdateFactory.newLatLngZoom`을 호출하다가 `NullPointerException`이 발생했습니다.

```kotlin
// ... (이전 코드)
// 6. DB 위치 목록(State)이 변경되면 카메라 이동 (MapScreen.kt:72 근방)
LaunchedEffect(state.locations) {
    state.locations.firstOrNull()?.let { latestLocation ->
        // 💥 문제의 코드!
        cameraPositionState.animate(
            com.google.android.gms.maps.model.CameraUpdateFactory.newLatLngZoom(
                LatLng(latestLocation.latitude, latestLocation.longitude), 15f
            )
        )
    }
}
// ...
```

이 현상의 원인은 다음과 같습니다.

1.  ViewModel에서 `state.locations` (DB 데이터)가 업데이트되었습니다.
2.  `LaunchedEffect(state.locations)`가 즉시 트리거되었습니다.
3.  `cameraPositionState.animate`를 호출하면서 `CameraUpdateFactory`를 사용했습니다.
4.  **하지만** 이 시점에 `GoogleMap` Composable이 아직 화면에 렌더링되고 내부 Map 객체를 **초기화하는 중**이었습니다.
5.  `CameraUpdateFactory`는 Google Map SDK가 완전히 초기화되어야 사용할 수 있는데, 아직 준비가 안 된 상태에서 호출했기 때문에 `is not initialized`라는 `NullPointerException`이 발생한 것입니다.

-----

## 2\. 💡 해결 방법

`GoogleMap` Composable이 \*\*"맵 로드가 완료되었습니다"\*\*라는 신호를 줄 때까지 기다려야 합니다.

`maps-compose` 라이브러리는 이를 위해 `onMapLoaded`라는 콜백을 제공합니다.

1.  맵이 로드되었는지 추적하는 `Boolean` 상태를 하나 만듭니다.
2.  `GoogleMap`의 `onMapLoaded` 콜백에서 이 상태를 `true`로 변경합니다.
3.  `LaunchedEffect`가 **`state.locations`와 `isMapLoaded`** 두 가지 상태를 모두 관찰하도록 변경하고, 이펙트 내부에서 `isMapLoaded`가 `true`일 때만 카메라를 움직이도록 합니다.

-----

## 3\. 👨‍💻 수정된 코드 (`MainScreen.kt`)

`remember` 상태 변수 추가, `onMapLoaded` 콜백 추가, `LaunchedEffect` 수정, 이렇게 세 부분이 변경되었습니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainScreen.kt

package com.example.kurlytalent.feature.main

import androidServices.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // 1. mutableStateOf 임포트
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue // 1. setValue 임포트
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kurlytalent.domain.model.LocationInfo
import com.example.kurlytalent.mvi.effect
import com.google.android.gms.maps.CameraUpdateFactory // 3. CameraUpdateFactory 임포트
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- 1. 맵 로드 상태를 저장할 변수 추가 ---
    var isMapLoaded by remember { mutableStateOf(false) }

    // (권한 런처 및 Effect 핸들러는 동일)
    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val isGranted = permissionsMap.values.all { it }
            viewModel.action(MainAction.OnPermissionResult(isGranted))
        }
    )

    scope.effect(viewModel) { effect: MainEffect ->
        when (effect) {
            is MainEffect.RequestLocationPermission -> {
                locationPermissionLauncher.launch(permissionsToRequest)
            }
            is MainEffect.ShowErrorPopup -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 10f)
    }

    // --- 3. LaunchedEffect 수정 ---
    // state.locations 뿐만 아니라 isMapLoaded 상태도 함께 관찰
    LaunchedEffect(state.locations, isMapLoaded) {
        // 맵이 로드되었고, 위치 데이터가 있을 때만 실행
        if (isMapLoaded && state.locations.isNotEmpty()) {
            state.locations.firstOrNull()?.let { latestLocation ->
                // 이제 CameraUpdateFactory.newLatLngZoom 호출이 안전합니다.
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(latestLocation.latitude, latestLocation.longitude), 15f
                    )
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // --- 2. 맵 로드가 완료되면 상태 변경 ---
                onMapLoaded = {
                    isMapLoaded = true
                }
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

            // (버튼 및 로딩 인디케이터는 동일)
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = {
                    viewModel.action(MainAction.OnFetchLocationClicked)
                }
            ) {
                Text(text = "현 위치")
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// (확장 함수 동일)
fun Long.toFormattedString(): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}
```

이제 `LaunchedEffect`는 `isMapLoaded`가 `true`가 된 이후에만 카메라 애니메이션을 시도하므로, `CameraUpdateFactory`가 항상 초기화된 상태임을 보장할 수 있습니다.

---

# 질문 10
위치 권한을 허용 안함으로 하는 경우 팝업이 뜨지 않는데 이러한 경우 시스템 설정으로 갈 수 있도록 하고 싶어

# 답변 10
좋은 포인트입니다\! 사용자가 권한을 '허용 안 함' (영구 거부)으로 설정한 경우, 일반 팝업 대신 시스템 설정으로 이동할 수 있도록 안내하는 것은 UX에 매우 중요합니다.

이 로직을 구현하기 위해 MVI 플로우를 수정하겠습니다.

1.  **MVI 수정**:
    * `OnPermissionResult` Action이 `isGranted` 외에 `shouldShowRationale` (다시 물어볼 수 있는지 여부) 상태도 전달하도록 변경합니다.
    * ViewModel은 `isGranted: false` 이고 `shouldShowRationale: false` (영구 거부)일 때, `GoToAppSettings`라는 새로운 Effect를 발행합니다.
2.  **View 수정**:
    * `rememberLauncherForActivityResult`의 `onResult` 콜백에서 `shouldShowRequestPermissionRationale`을 확인하여 ViewModel에 전달합니다.
    * `GoToAppSettings` Effect를 수신하면, "설정으로 이동" 옵션이 있는 스낵바(또는 다이얼로그)를 띄우고, 사용자가 "설정"을 누르면 앱의 시스템 설정 화면으로 `Intent`를 실행합니다.

-----

## 1\. MVI 정의 수정 (`MainMVI.kt`)

`OnPermissionResult` Action을 확장하고 `GoToAppSettings` Effect를 추가합니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainMVI.kt
package com.example.kurlytalent.feature.main

// ... (imports)

// Actions (UI -> ViewModel)
sealed interface MainAction : Action {
    data object OnFetchLocationClicked : MainAction
    /**
     * UI에서 권한 요청 결과를 전달합니다.
     * @param isGranted 권한 허용 여부
     * @param shouldShowRationale 권한을 다시 요청할 수 있는지 여부 (false면 영구 거부)
     */
    data class OnPermissionResult(val isGranted: Boolean, val shouldShowRationale: Boolean) : MainAction
}

// ... (State는 변경 없음)

// Effects (ViewModel -> UI, 일회성 이벤트)
sealed interface MainEffect : Effect {
    data object RequestLocationPermission : MainEffect
    data class ShowErrorPopup(val message: String) : MainEffect
    /**
     * 시스템 앱 설정 화면으로 이동하도록 UI에 요청합니다.
     */
    data object GoToAppSettings : MainEffect
}

// ... (Reducer는 변경 없음)
```

-----

## 2\. ViewModel 수정 (`MainViewModel.kt`)

`OnPermissionResult` Action을 처리하는 로직을 분기합니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainViewModel.kt
package com.example.kurlytalent.feature.main

// ... (imports)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enqueueLocationUpdateUseCase: EnqueueLocationUpdateUseCase,
    private val observeLocationWorkUseCase: ObserveLocationWorkUseCase,
    private val locationRepository: LocationRepository
) : BaseViewModel<MainAction, MainState, MainEffect, MainReducer>(
    initialState = MainState()
) {

    // ... (init)

    override fun action(action: MainAction) {
        when (action) {
            is MainAction.OnFetchLocationClicked -> {
                checkLocationPermission()
            }
            
            // 2. OnPermissionResult 처리 로직 수정
            is MainAction.OnPermissionResult -> {
                if (action.isGranted) {
                    // 2-1. 권한 허용됨
                    startLocationWork()
                } else {
                    // 2-2. 권한 거부됨
                    if (action.shouldShowRationale) {
                        // 2-3. 다시 물어볼 수 있음 (단순 거부)
                        launchInViewModelScope {
                            emitEffect(MainEffect.ShowErrorPopup("위치 권한이 필요합니다."))
                        }
                    } else {
                        // 2-4. 다시 물어볼 수 없음 (영구 거부)
                        launchInViewModelScope {
                            emitEffect(MainEffect.GoToAppSettings)
                        }
                    }
                }
            }
        }
    }
    
    private fun checkLocationPermission() = launchInViewModelScope {
        val finePermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (finePermissionGranted || coarsePermissionGranted) {
            // 1. 권한이 이미 있으면, 바로 작업 시작 (OnPermissionResult를 거치지 않음)
            startLocationWork()
        } else {
            // 권한이 없으면, UI에 권한 요청 Effect 전달
            emitEffect(MainEffect.RequestLocationPermission)
        }
    }

    // ... (startLocationWork, observeAllLocations, onHandleException 등)
}
```

-----

## 3\. View 수정 (`MainScreen.kt`)

`rememberLauncherForActivityResult`의 `onResult` 콜백과 `effect` 핸들러를 수정합니다.

```kotlin
// feature/src/main/java/com/example/kurlytalent/feature/main/MainScreen.kt

package com.example.kurlytalent.feature.main

import androidServices.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
// ... (other layout imports)
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult // 1. 스낵바 결과 임포트
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // 2. LocalContext 임포트
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// ... (domain model, mvi, map imports)
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
// ... (other imports)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // 3. Context 가져오기

    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // 4. 권한 런처 onResult 수정
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val isGranted = permissionsMap.values.all { it }
            if (isGranted) {
                // 4-1. 허용됨
                viewModel.action(MainAction.OnPermissionResult(isGranted = true, shouldShowRationale = false))
            } else {
                // 4-2. 거부됨
                // shouldShowRequestPermissionRationale을 확인하기 위해 Activity가 필요
                val activity = context.findActivity()
                val shouldShowRationale = if (activity != null) {
                    // 요청한 권한 중 하나라도 '다시 물어볼 수 있는지' 확인
                    permissionsToRequest.any {
                        activity.shouldShowRequestPermissionRationale(it)
                    }
                } else {
                    true // Activity를 찾지 못하는 예외 상황 시, 단순 거부로 처리
                }
                
                viewModel.action(MainAction.OnPermissionResult(isGranted = false, shouldShowRationale = shouldShowRationale))
            }
        }
    )

    // 5. Effect 핸들러 수정
    scope.effect(viewModel) { effect: MainEffect ->
        when (effect) {
            is MainEffect.RequestLocationPermission -> {
                locationPermissionLauncher.launch(permissionsToRequest)
            }
            is MainEffect.ShowErrorPopup -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
            // 5-1. GoToAppSettings Effect 처리
            is MainEffect.GoToAppSettings -> {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "위치 권한이 영구적으로 거부되었습니다.",
                        actionLabel = "설정으로 이동",
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // 5-2. 시스템 설정 화면으로 Intent 실행
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

    // ... (맵 로드 상태 변수, LaunchedEffect, Scaffold 등 나머지 UI 코드는 동일)
    
    // (이하 동일)
    val cameraPositionState = rememberCameraPositionState { /* ... */ }
    var isMapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(state.locations, isMapLoaded) { /* ... */ }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { isMapLoaded = true }
            ) { /* ... */ }
            Button(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                onClick = { viewModel.action(MainAction.OnFetchLocationClicked) }
            ) { /* ... */ }
            if (state.isLoading) { /* ... */ }
        }
    }
}

// 6. Context에서 Activity를 찾기 위한 헬퍼 함수
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// (toFormattedString 확장 함수는 동일)
```