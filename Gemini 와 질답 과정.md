## 질문 1
요구사항
- '현 위치' 버튼을 클릭하면 현재 위치를 WorkManager로 조회합니다.
- 조회된 위치 정보는 Room 로컬 DB에 저장합니다.
- Room 로컬 DB에 저장된 정보를 가지고 와서 지도뷰에 마커 표시를 합니다.

위와 같은 Android 프로젝트를 만들거야 기본 골자를 구상해보자 일단은 프로젝트 초기화를 하고 싶어
Android Architecture Components 의 구조로 구현을 하고 싶은데 아이디어를 내봐

## 답변 1
좋습니다. 요구하신 기능(WorkManager로 위치 조회, Room에 저장, 지도에 표시)을 Android Architecture Components (AAC) 기반으로 구현하기 위한 초기 프로젝트 골격을 잡아보겠습니다.
핵심 구조는 다음과 같습니다.
1. UI Layer (Activity/Fragment + ViewModel): 사용자의 '현 위치' 버튼 클릭을 받고, ViewModel을 통해 WorkManager 작업을 요청합니다. 또한 ViewModel로부터 Room DB의 위치 데이터 변경을 Flow (또는 LiveData)로 구독하여 지도에 마커를 그립니다.
2. Domain Layer (WorkManager): ViewModel의 요청을 받아 백그라운드에서 FusedLocationProviderClient를 사용해 실제 위치를 조회합니다.
3. Data Layer (Repository + Room): WorkManager는 조회된 위치를 Room DB에 저장합니다. Repository는 Room의 DAO를 통해 이 데이터를 Flow로 노출시켜 ViewModel이 구독할 수 있게 합니다.
4. Dependency Injection (Hilt): 이 모든 구성 요소(DB, DAO, Repository, LocationClient 등)를 Hilt를 사용해 주입하여 의존성을 관리합니다.