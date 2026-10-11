"""매일 실행되는 데이터 갱신 스크립트 (GitHub Actions가 자동 실행).

흐름: 수집기 실행 → 형식 검사 → 이전 데이터와 비교 → 바뀌었으면 data/subsidies.json 저장

⚠️ 현재는 수집기가 연결되어 있지 않습니다 (KTOA 데이터 이용 승인 대기).
   승인 후 scripts/collectors/ktoa_collector.py 를 승인받은 방식대로 구현하고
   아래 COLLECTOR_ENABLED 를 True 로 바꾸면 됩니다.
"""
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from validate_data import validate  # noqa: E402

COLLECTOR_ENABLED = False
DATA_PATH = Path(__file__).resolve().parent.parent / "data" / "subsidies.json"
KST = timezone(timedelta(hours=9))


def main() -> int:
    if not COLLECTOR_ENABLED:
        print("ℹ️ 수집기가 아직 연결되지 않았습니다. 기존 데이터를 그대로 둡니다.")
        return 0

    from collectors.ktoa_collector import collect

    plans, devices, source, source_url = collect()
    new = {
        "updatedAt": datetime.now(KST).replace(microsecond=0).isoformat(),
        "source": source,
        "sourceUrl": source_url,
        "isSample": False,
        "plans": plans,
        "devices": devices,
    }

    errors = validate(new)
    if errors:
        print("❌ 수집 데이터 형식 오류 — 기존 데이터를 유지합니다")
        for e in errors:
            print("  -", e)
        return 1

    # 이상치 방어: 기종 수가 갑자기 절반 이하로 줄면 수집 오류로 보고 중단
    if DATA_PATH.exists():
        old = json.loads(DATA_PATH.read_text(encoding="utf-8"))
        if not old.get("isSample") and len(devices) < len(old.get("devices", [])) * 0.5:
            print(f"❌ 기종 수가 급감했습니다 ({len(old['devices'])} → {len(devices)}). 확인이 필요합니다.")
            return 1
        if old.get("plans") == plans and old.get("devices") == devices:
            print("ℹ️ 변경 사항 없음")
            return 0

    DATA_PATH.write_text(json.dumps(new, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"✅ 갱신 완료: 기종 {len(devices)}개, 요금제 {len(plans)}개")
    return 0


if __name__ == "__main__":
    sys.exit(main())
