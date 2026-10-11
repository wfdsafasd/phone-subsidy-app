"""KTOA(스마트초이스) 지원금 데이터 수집기 — 승인 후 구현할 자리.

KTOA에서 서면 승인을 받은 뒤, 승인된 제공 방식에 맞춰 collect() 를 구현하세요.
  - API 를 제공받은 경우: 발급받은 키로 API 호출
  - 파일(CSV/엑셀)을 제공받은 경우: 해당 파일을 읽어 변환
  - 웹 수집을 허락받은 경우: 허락된 범위·빈도만 지키고, User-Agent에 앱 이름과 연락처를 밝힐 것

반환 형식 (data/subsidies.json 과 동일):
  plans   = [{"id": "skt_xxx", "carrier": "SKT", "name": "...", "monthlyFee": 89000, "dataDesc": "무제한"}, ...]
  devices = [{"id": "...", "name": "...", "maker": "...", "storage": "256GB", "releasePrice": 1155000,
              "subsidies": [{"planId": "skt_xxx", "amount": 500000}, ...]}, ...]
"""


def collect():
    """(plans, devices, source, source_url) 를 반환"""
    raise NotImplementedError("KTOA 승인 후 승인된 방식에 맞춰 구현하세요")
