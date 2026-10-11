"""통신사 지원금 페이지 구조 탐색기 (1회성 진단 도구).

각 통신사 온라인샵의 지원금 페이지를 실제 브라우저로 열고,
페이지가 내부적으로 불러오는 데이터 요청(XHR/fetch)과 응답을 기록합니다.
결과(discovery/ 폴더)를 보면 어떤 주소에서 기종·출고가·지원금이 오는지 알 수 있고,
그걸 바탕으로 가볍고 정확한 수집기를 만듭니다.

GitHub Actions의 "지원금 페이지 구조 탐색" 워크플로가 실행합니다.
직접 실행:  pip install playwright && playwright install chromium
           python scripts/collectors/discover.py
"""
from __future__ import annotations

import json
import os
import re
import time
import urllib.request
import urllib.robotparser
from pathlib import Path
from urllib.parse import urlparse

from playwright.sync_api import sync_playwright

TARGETS = {
    "SKT": "https://shop.tworld.co.kr/notice",
    "KT": "https://shop.kt.com/smart/supportAmtList.do",
    "LGU": "https://www.lguplus.com/mobile/financing-model",
}

CONTACT = os.environ.get("CONTACT_EMAIL", "contact-not-set")
# 정체를 숨기지 않도록 브라우저 UA 뒤에 앱 이름과 연락처를 붙입니다.
BOT_TAG = f"PhoneSubsidyCompareBot/0.1 (+mailto:{CONTACT})"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 " + BOT_TAG
)

OUT = Path(os.environ.get("DISCOVERY_OUT", "discovery"))
MAX_BODY = 300_000  # 응답 하나당 저장할 최대 글자 수
KEYWORDS = re.compile(
    r"지원금|공시|출고|support|subsid|sprt|spt|amt|price|prc|disc|model|mdl|plan|ppln",
    re.IGNORECASE,
)
SKIP_EXT = re.compile(r"\.(png|jpe?g|gif|svg|webp|woff2?|ttf|css|ico|mp4)(\?|$)", re.IGNORECASE)


def robots_allows(url: str) -> tuple[bool, str]:
    """robots.txt 를 확인해 허용 여부와 원문을 반환"""
    p = urlparse(url)
    robots_url = f"{p.scheme}://{p.netloc}/robots.txt"
    try:
        req = urllib.request.Request(robots_url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=15) as r:
            text = r.read().decode("utf-8", errors="replace")
    except Exception as e:  # robots.txt 가 없거나 못 읽으면 기록만 하고 진행
        return True, f"(robots.txt 읽기 실패: {e})"
    rp = urllib.robotparser.RobotFileParser()
    rp.parse(text.splitlines())
    return rp.can_fetch(BOT_TAG.split("/")[0], url) and rp.can_fetch("*", url), text


def discover(carrier: str, url: str, browser) -> dict:
    out_dir = OUT / carrier
    out_dir.mkdir(parents=True, exist_ok=True)

    allowed, robots_text = robots_allows(url)
    (out_dir / "robots.txt").write_text(robots_text, encoding="utf-8")
    if not allowed:
        print(f"[{carrier}] robots.txt 가 이 페이지 수집을 허용하지 않아 건너뜁니다")
        return {"carrier": carrier, "url": url, "robotsAllowed": False, "requests": []}

    ctx = browser.new_context(user_agent=USER_AGENT, locale="ko-KR", viewport={"width": 1400, "height": 1000})
    page = ctx.new_page()
    records: list[dict] = []

    def on_response(resp):
        req = resp.request
        if req.resource_type not in ("xhr", "fetch", "document", "script"):
            return
        if SKIP_EXT.search(resp.url):
            return
        rec = {
            "url": resp.url,
            "method": req.method,
            "resourceType": req.resource_type,
            "status": resp.status,
            "contentType": resp.headers.get("content-type", ""),
            "postData": (req.post_data or "")[:5000],
        }
        if req.resource_type in ("xhr", "fetch"):
            try:
                body = resp.text()
            except Exception as e:
                body = f"(본문 읽기 실패: {e})"
            rec["bodyLength"] = len(body)
            rec["keywordHit"] = bool(KEYWORDS.search(resp.url + body[:20000]))
            idx = len(records)
            body_file = out_dir / f"resp_{idx:03d}.txt"
            body_file.write_text(body[:MAX_BODY], encoding="utf-8")
            rec["bodyFile"] = body_file.name
        records.append(rec)

    page.on("response", on_response)

    print(f"[{carrier}] 여는 중: {url}")
    try:
        page.goto(url, wait_until="networkidle", timeout=60_000)
    except Exception as e:
        print(f"[{carrier}] 페이지 로드 경고: {e}")

    # 목록이 스크롤로 더 불려오는 경우를 위해 천천히 스크롤 (서버 부담 최소화)
    for _ in range(6):
        page.mouse.wheel(0, 2500)
        time.sleep(1.5)
    try:
        page.wait_for_load_state("networkidle", timeout=15_000)
    except Exception:
        pass

    (out_dir / "page.html").write_text(page.content(), encoding="utf-8")
    try:
        (out_dir / "page_text.txt").write_text(page.inner_text("body")[:MAX_BODY], encoding="utf-8")
    except Exception:
        pass
    page.screenshot(path=str(out_dir / "screenshot.png"), full_page=True)

    ctx.close()
    hits = [r for r in records if r.get("keywordHit")]
    print(f"[{carrier}] 요청 {len(records)}건 기록, 데이터 후보 {len(hits)}건")
    return {"carrier": carrier, "url": url, "robotsAllowed": True, "requests": records}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    summary = []
    with sync_playwright() as p:
        browser = p.chromium.launch()
        for carrier, url in TARGETS.items():
            try:
                summary.append(discover(carrier, url, browser))
            except Exception as e:
                print(f"[{carrier}] 실패: {e}")
                summary.append({"carrier": carrier, "url": url, "error": str(e)})
            time.sleep(5)  # 사이트 간 간격
        browser.close()
    (OUT / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"완료: {OUT}/summary.json")


if __name__ == "__main__":
    main()
