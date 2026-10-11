"""지원금 JSON 형식 검사기.

사용법:  python scripts/validate_data.py data/subsidies.json
문제가 있으면 내용을 출력하고 종료 코드 1로 끝납니다 (GitHub Actions에서 갱신을 막음).
"""
import json
import sys
from datetime import datetime

CARRIERS = {"SKT", "KT", "LGU"}


def validate(data: dict) -> list[str]:
    errors: list[str] = []

    for key in ("updatedAt", "source", "plans", "devices"):
        if key not in data:
            errors.append(f"최상위 '{key}' 항목이 없습니다")
    if errors:
        return errors

    try:
        datetime.fromisoformat(data["updatedAt"])
    except ValueError:
        errors.append(f"updatedAt 형식 오류: {data['updatedAt']}")

    plan_ids = set()
    for i, p in enumerate(data["plans"]):
        where = f"plans[{i}]"
        for k in ("id", "carrier", "name", "monthlyFee"):
            if k not in p:
                errors.append(f"{where}: '{k}' 없음")
        if p.get("carrier") not in CARRIERS:
            errors.append(f"{where}: carrier는 {sorted(CARRIERS)} 중 하나여야 합니다 ({p.get('carrier')})")
        if not isinstance(p.get("monthlyFee"), int) or p["monthlyFee"] <= 0:
            errors.append(f"{where}: monthlyFee는 양의 정수여야 합니다")
        if p.get("id") in plan_ids:
            errors.append(f"{where}: 중복된 요금제 id {p.get('id')}")
        plan_ids.add(p.get("id"))

    device_ids = set()
    for i, d in enumerate(data["devices"]):
        where = f"devices[{i}] ({d.get('name', '?')})"
        for k in ("id", "name", "maker", "releasePrice", "subsidies"):
            if k not in d:
                errors.append(f"{where}: '{k}' 없음")
        if d.get("id") in device_ids:
            errors.append(f"{where}: 중복된 기종 id")
        device_ids.add(d.get("id"))
        price = d.get("releasePrice")
        if not isinstance(price, int) or price <= 0:
            errors.append(f"{where}: releasePrice는 양의 정수여야 합니다")
            continue
        for j, s in enumerate(d.get("subsidies", [])):
            if s.get("planId") not in plan_ids:
                errors.append(f"{where}.subsidies[{j}]: 없는 요금제 id {s.get('planId')}")
            amt = s.get("amount")
            if not isinstance(amt, int) or amt < 0:
                errors.append(f"{where}.subsidies[{j}]: amount는 0 이상 정수여야 합니다")
            elif amt > price:
                errors.append(f"{where}.subsidies[{j}]: 지원금({amt})이 출고가({price})보다 큽니다")

    if not data["devices"] or not data["plans"]:
        errors.append("기종 또는 요금제 목록이 비어 있습니다")
    return errors


def main(path: str) -> int:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    errors = validate(data)
    if errors:
        print(f"❌ {path}: 오류 {len(errors)}건")
        for e in errors:
            print("  -", e)
        return 1
    print(f"✅ {path}: 기종 {len(data['devices'])}개, 요금제 {len(data['plans'])}개, 기준 {data['updatedAt']}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "data/subsidies.json"))
