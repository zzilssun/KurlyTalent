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
