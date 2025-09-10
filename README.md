# PhysioSim
인체생리학에 의거한 자바 기반 프로젝트

## 소개
이 프로젝트는 세포 > 조직 > 기관 > 기관계 > 개체의 계층 구조를 기반으로,

인체생리학의 기본 원리(혈압 조절, 호흡, 혈당 항상성 등)를 자바(JAVA)로 시뮬레이션합니다.

*참고: eclipse + jdk17 + sqlite 활용

<img width="736" height="772" alt="image" src="https://github.com/user-attachments/assets/5f3d2d78-ab1e-485a-a09d-54da3ca725e5" />


## 주요 기능
1. 사용자 입력(성별, 키, 체중)
   > DB에 저장해 캐릭터 파라미터 초기화
2. 생리학적 상호작용
   > 신경계, 심혈관계, 근육계, 내분비계, 호흡계, 비뇨계, 소화계, 감각계, 생식계
3. 시각화
   > Vital Signs (Map, HR, SpO₂, RR, Glucose 등): 정상 및 이탈 표기

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
- 구조 재정립: 계정 설정의 이유 (사용자의 계정 - 여러 캐릭터 - 캐릭터의 바이탈)

## 메모
1. DB 스키마
   
users(id, username UNIQUE, email UNIQUE, password_hash, created_at)

characters(id, user_id FK→users ON DELETE CASCADE, name, sex, height_cm, weight_kg, created_at)

vitals(id, character_id FK→characters ON DELETE CASCADE, hr/map/rr/spo2/glucose/temp, recorded_at)


2. - DB 부트스트랩 통일: `Database.open()` → `Database.setup()`로 PRAGMA + 스키마 생성
