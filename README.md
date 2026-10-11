# 폰값비교 — 3사 지원금 비교 앱 (안드로이드)

SKT · KT · LG U+ 의 단말기 지원금을 매일 갱신해서, 기종·요금제별로
**"지원금 받기" vs "선택약정 25%"** 중 24개월 기준 어느 쪽이 싼지 비교해 주는 앱입니다.

> 현재는 **샘플 데이터** 상태입니다. KTOA(스마트초이스) 데이터 이용 승인을 받은 뒤
> 수집기를 연결하면 실제 데이터로 매일 자동 갱신됩니다. (7단계)

---

## 구성

| 폴더 | 내용 |
|---|---|
| `app/` | 안드로이드 앱 (Kotlin + Jetpack Compose) |
| `data/subsidies.json` | 앱이 매일 받아가는 지원금 데이터 |
| `scripts/` | 데이터 갱신·형식 검사 스크립트 (Python) |
| `.github/workflows/` | 매일 오전 6시경 자동 갱신 설정 (GitHub Actions, 무료) |
| `docs/privacy-policy.md` | 개인정보처리방침 (플레이스토어 필수) |

**동작 방식:** GitHub가 매일 `data/subsidies.json` 을 갱신 → 앱이 실행될 때 그 파일을 받아 표시.
서버를 따로 빌릴 필요가 없어 **운영 비용 0원**입니다.

---

## ⚡ 빠른 길: Android Studio 없이 APK 받기 (15분)

1. 아래 **4단계의 1~2번**대로 GitHub 저장소를 만들고 파일을 전부 올립니다
2. 저장소 상단 **Actions** 탭 → 처음이면 초록 버튼 "I understand my workflows, go ahead and enable them" 클릭
3. 왼쪽 **APK 자동 빌드** → 오른쪽 **Run workflow** → Run workflow (파일을 올릴 때 자동으로 시작되기도 해요)
4. 5~10분 뒤 실행 항목에 ✅ 가 뜨면 클릭 → 맨 아래 **Artifacts** 의 `폰값비교-apk` 다운로드 → 압축 풀면 `app-debug.apk`
5. 폰으로 APK 파일을 옮겨 실행 → "출처를 알 수 없는 앱 설치 허용" → 설치

- ❌ 가 뜨면 실행 항목을 눌러 빨간 단계의 로그를 복사해 Claude에게 보여주세요
- 이 APK는 **내 폰 테스트용**입니다. 플레이스토어에는 5단계의 서명된 AAB를 올려야 해요

## 1단계. Android Studio 설치 (30분)

1. https://developer.android.com/studio 에서 **Android Studio** 다운로드·설치
2. 처음 실행 시 나오는 설정 마법사는 모두 **Standard(기본값)** → Next → Finish
   (안드로이드 SDK가 자동으로 설치됩니다. 시간이 좀 걸려요)

## 2단계. 프로젝트 열고 실행해 보기 (20분)

1. 받은 zip 파일 압축 풀기 (한글이 없는 경로 권장, 예: `C:\dev\phone-subsidy-app`)
2. Android Studio → **Open** → 압축 푼 `phone-subsidy-app` 폴더 선택
3. 아래쪽에 "Gradle sync" 진행 표시가 끝날 때까지 기다리기 (처음엔 5~15분)
   - 버전 업그레이드 안내(Upgrade Assistant)가 뜨면 **권장대로 업그레이드**해도 됩니다
   - `gradlew` 관련 안내가 나오면 Android Studio가 제안하는 대로 진행하세요
4. 상단 메뉴 **Tools → Device Manager → 가상 기기 추가(+)** → Pixel 아무 기종 → 최신 시스템 이미지 선택
5. 상단의 ▶ (Run) 버튼 → 에뮬레이터에 앱이 뜨면 성공 🎉
   - 내 폰으로 보려면: 폰 설정 → 휴대전화 정보 → 빌드번호 7번 탭(개발자 모드) → 개발자 옵션 → USB 디버깅 켜기 → USB 연결 후 ▶

## 3단계. 출시 전 반드시 바꿀 것

| 파일 | 바꿀 내용 |
|---|---|
| `app/build.gradle.kts` | `applicationId` 를 고유한 값으로 (예: `com.본인영문이름.phonesubsidy`). **출시 후 변경 불가** |
| `app/src/main/res/values/strings.xml` | 앱 이름 (통신사 이름·"공식" 같은 단어 금지) |
| `app/build.gradle.kts` | `DATA_URL`, `PRIVACY_URL` (4단계에서 확인한 주소) |
| `docs/privacy-policy.md` | [ ] 표시된 부분 채우기 |

> `applicationId` 를 바꿀 때 `namespace` 는 그대로 둬도 됩니다.

## 4단계. GitHub에 올리기 (자동 갱신 + 개인정보처리방침 주소)

1. https://github.com 가입 → 오른쪽 위 **+ → New repository**
   - 이름 예: `phone-subsidy-app`, **Public** 선택 (앱이 데이터를 받아가야 하므로)
2. **uploading an existing file** 링크 → 압축 푼 폴더 안의 파일·폴더 전체를 끌어다 놓기 → Commit
   - `.github` 폴더처럼 점(.)으로 시작하는 폴더가 빠지지 않았는지 확인
   - 더 편하게 하려면 GitHub Desktop 앱 사용 권장
3. 저장소 **Settings → Actions → General → Workflow permissions → Read and write** 체크 → Save
4. 저장소 **Settings → Pages → Branch: main / 폴더: /docs** → Save
   → 몇 분 뒤 `https://본인아이디.github.io/저장소이름/privacy-policy.html` 에서 방침이 보입니다
5. `app/build.gradle.kts` 의 주소 두 개를 교체:
   - `DATA_URL` = `https://raw.githubusercontent.com/본인아이디/저장소이름/main/data/subsidies.json`
   - `PRIVACY_URL` = 위 4번 주소
6. 앱을 다시 실행 → 화면을 아래로 당겨 새로고침 → "최신 데이터로 갱신했어요" 가 뜨면 연결 성공

**데이터 직접 고치기:** GitHub에서 `data/subsidies.json` 을 열고 연필(✏️) 버튼으로 수정 → Commit.
형식이 틀리면 Actions 탭에 ❌ 가 표시되니 확인하세요. 앱은 다음 실행 때 반영합니다.

## 5단계. 출시용 파일(AAB) 만들기

1. Android Studio → **Build → Generate Signed App Bundle or APK → Android App Bundle**
2. **Create new...** 로 서명 키(.jks) 생성
   - ⚠️ 키 파일과 비밀번호를 **두 곳 이상에 백업**하세요. 잃어버리면 앱 업데이트가 매우 번거로워집니다
   - ⚠️ 키 파일은 GitHub에 올리지 마세요 (`.gitignore` 에 이미 제외 설정됨)
3. **release** 선택 → Create → `app/release/app-release.aab` 생성

## 6단계. 플레이스토어 등록

1. https://play.google.com/console 에서 개발자 계정 등록 (일회성 등록비, 신원 확인 필요)
2. **앱 만들기** → 앱 이름, 기본 언어 한국어, 앱/무료 선택
3. 대시보드의 할 일 목록을 순서대로 채우기:
   - **개인정보처리방침**: 4단계 주소 입력
   - **데이터 보안**: "사용자 데이터를 수집하거나 공유하지 않음" (광고 SDK를 넣었다면 그에 맞게 변경)
   - **앱 액세스 권한**: 로그인 없이 모든 기능 사용 가능
   - **콘텐츠 등급**: 설문 작성 (일반적으로 전체이용가)
   - **타겟층**: 18세 이상 권장
   - **스토어 등록정보**: 설명, 스크린샷(최소 2장), 아이콘 512×512, 그래픽 이미지 1024×500
4. **개인 계정이라면 비공개 테스트 필수**: 테스트 → 비공개 테스트 → AAB 업로드 → 테스터 **12명 이상이 14일 연속** 참여
   - 테스트 기간 중 업데이트를 1~2번 올리면 승인에 유리하다고 알려져 있어요
5. 14일 후 **프로덕션 액세스 신청** → 심사 통과 후 출시

**스토어 등록 시 주의**
- 설명에 "참고용 정보, 통신사와 무관한 개인 운영" 문구 넣기
- 통신사 로고·공식 앱처럼 보이는 아이콘/스크린샷 사용 금지
- "최저가 보장", "공식" 같은 표현 금지

## 6-1단계. 통신사 지원금 자동 수집 준비 (구조 탐색)

통신사 지원금 페이지가 데이터를 어디서 불러오는지 한 번 조사합니다. GitHub에서 자동으로 돌아가요.

1. (선택) 저장소 **Settings → Secrets and variables → Actions → Variables 탭 → New repository variable**
   - Name: `CONTACT_EMAIL` / Value: 연락받을 이메일 (수집기가 자기 정체와 연락처를 밝히는 데 사용)
2. **Actions** 탭 → 왼쪽 **지원금 페이지 구조 탐색** → **Run workflow**
3. 3~5분 뒤 ✅ 실행 항목 클릭 → 맨 아래 **Artifacts** 의 `discovery` 다운로드
4. 받은 `discovery.zip` 을 Claude 대화창에 첨부 → 실제 수집기를 만들어 드립니다

- 탐색기는 각 사이트의 robots.txt 를 확인해서, 허용하지 않는 곳은 자동으로 건너뜁니다
- 페이지당 한 번만 열고 천천히 스크롤하므로 서버 부담은 일반 방문자 수준입니다

## 7단계. 실제 데이터 연결

1. `scripts/collectors/ktoa_collector.py` 의 `collect()` 를 **승인받은 방식**(API / 파일 / 허용된 수집)에 맞게 구현
2. `scripts/update_data.py` 의 `COLLECTOR_ENABLED = False` → `True`
3. GitHub Actions 탭에서 **Run workflow** 로 한 번 수동 실행해 확인
4. 이후 매일 오전 6시경 자동 갱신되고, 앱 상단의 "샘플 데이터" 경고는 자동으로 사라집니다

> 이 단계는 승인 조건이 정해진 뒤 Claude에게 승인 내용과 함께 요청하면 구현해 드릴 수 있어요.

---

## 계산 방식 (앱 '안내' 화면과 동일)

- 24개월 기준 (단말 할부금 + 월 요금) 총액 비교
- 단말 할부수수료 연 5.9%, 원리금 균등 가정 → `domain/CostCalculator.kt` 의 `Assumptions` 에서 수정 가능
- 선택약정: 월 요금 25% 할인
- 매장 추가지원금 입력 시 두 방식 모두 단말 가격에서 차감 (단통법 폐지 후 선택약정과 중복 가능)
- 요금제 의무 유지기간, 부가서비스, 결합·카드 할인, 위약금은 미반영

## 데이터 형식 (`data/subsidies.json`)

```json
{
  "updatedAt": "2026-10-07T06:00:00+09:00",
  "source": "스마트초이스",
  "sourceUrl": "https://www.smartchoice.or.kr",
  "isSample": false,
  "plans":   [{ "id": "skt_a", "carrier": "SKT", "name": "요금제명", "monthlyFee": 89000, "dataDesc": "무제한" }],
  "devices": [{ "id": "dev1", "name": "기종명", "maker": "제조사", "storage": "256GB", "releasePrice": 1155000,
                "subsidies": [{ "planId": "skt_a", "amount": 500000 }] }]
}
```
- `carrier` 는 `SKT`, `KT`, `LGU` 중 하나
- 금액은 모두 원 단위 정수
- 형식 검사: `python scripts/validate_data.py data/subsidies.json`
