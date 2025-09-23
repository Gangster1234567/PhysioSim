# PhysioSim
인체생리학에 의거한 자바 기반 프로젝트

## 소개
- 계층 구조: 세포 → 조직 → 기관 → 기관계 → 개체(sim)
- 목표: 하나의 공통 시뮬레이션 코어 위에서  
  - 의료인 모드: 정확한 수치/그래프/개입 제어  
  - 환자 모드: 캐릭터 모션으로 직관적 상태 확인
- 개발 환경: Eclipse + JDK 17 + SQLite (JDBC)

<img width="736" height="772" alt="image" src="https://github.com/user-attachments/assets/5f3d2d78-ab1e-485a-a09d-54da3ca725e5" />


## 주요 기능
1. 사용자 입력(성별, 키, 체중)
   > DB에 저장해 캐릭터 파라미터 초기화
2. 생리학적 상호작용
   > 신경계, 심혈관계, 근육계, 내분비계, 호흡계, 비뇨계, 소화계, 감각계, 생식계
3. 시각화
   - 의료인 모드: 실시간 그래프/수치/알람, 개입 패널(O₂, 출혈/수액 등)
   - 환자 모드: 캐릭터 모션(호흡/피부색/아이콘), 핵심 바이탈 카드(HR, RR, SpO₂, MAP)

## 설계 개요
- 패키지
  - `physiosim.core`: `Updatable`, `EventBus`, `SimulationClock`, `Units/Quantity`
  - `physiosim.cell / tissue / organ / system`: 생리 도메인 계층
  - `physiosim.sim`: `Character`, `VitalSigns`, `HomeostasisController`
  - `physiosim.control`: `Scenario`, `Intervention`, `AlarmManager`, Recorder/Playback
  - `physiosim.db`: `Database`, `UserRepository`, `CharacterRepository`, `VitalRepository` …
  - `physiosim.ui`: ModeSelect / Login / PatientView / ClinicianView
  - `physiosim.event`: 알람/개입/상태 변화 이벤트 타입

- 실행 모드
  - 시작 화면에서 환자 모드 또는 의료인 모드(로그인 필요) 선택
  - 같은 코어 엔진, 다른 뷰(권한 가드)

## 진행 기록
### DAY 1
- GitHub Repo 생성
- README.md 초안 작성
- 프로젝트 기본 구조 설계 시작 및 추가 정보 학습
- 패키지 추가

### DAY 2
- SQLite 연동 및 DB 초기화
- 사용자 기본 정보(성별, 키, 체중) 저장 기능 구현

### DAY 3
- 로그인/회원가입 기능 추가 (백엔드 로직)
- 비밀번호 로직 추가
- 구조 재정립: 계정 설정의 이유 (사용자의 계정 - 다 캐릭터 - 캐릭터의 바이탈)

## 메모
단위 고정: bpm / % / mmHg / ℃ / mg·dL
MAP 계산식: MAP = DBP + (SBP - DBP)/3

DB 부트스트랩(통일 규칙)
Database.open(path): 연결만 연다.
Database.setup(connection): PRAGMA + 스키마 생성(멱등) 을 수행한다.
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA temp_store = MEMORY;
PRAGMA busy_timeout = 5000;
// PRAGMA wal_autocheckpoint = 1000; / PRAGMA cache_size = -20000;
