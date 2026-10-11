package kr.example.subsidycompare.data

import kotlinx.serialization.Serializable

/**
 * 서버(GitHub)에서 받아오는 지원금 데이터 전체.
 * 형식은 data/subsidies.json 과 scripts/validate_data.py 를 참고하세요.
 */
@Serializable
data class SubsidyData(
    /** 데이터 기준 시각 (ISO-8601, 예: 2026-10-07T06:00:00+09:00) */
    val updatedAt: String,
    /** 출처 표시 문구 (예: "스마트초이스") */
    val source: String,
    /** 출처 원문 링크 */
    val sourceUrl: String = "",
    /** 샘플 데이터 여부: true면 앱 상단에 '샘플' 경고가 표시됩니다 */
    val isSample: Boolean = false,
    val plans: List<Plan>,
    val devices: List<Device>,
)

@Serializable
data class Plan(
    val id: String,
    /** "SKT", "KT", "LGU" 중 하나 */
    val carrier: String,
    val name: String,
    /** 월 요금(원, VAT 포함) */
    val monthlyFee: Int,
    /** 데이터 제공량 설명 (예: "무제한", "30GB") */
    val dataDesc: String = "",
)

@Serializable
data class Device(
    val id: String,
    val name: String,
    val maker: String,
    val storage: String = "",
    /** 출고가(원) */
    val releasePrice: Int,
    val subsidies: List<Subsidy>,
)

@Serializable
data class Subsidy(
    val planId: String,
    /** 통신사 공통지원금(원) */
    val amount: Int,
)

enum class Carrier(val code: String, val label: String) {
    SKT("SKT", "SKT"),
    KT("KT", "KT"),
    LGU("LGU", "LG U+");

    companion object {
        fun from(code: String): Carrier? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
