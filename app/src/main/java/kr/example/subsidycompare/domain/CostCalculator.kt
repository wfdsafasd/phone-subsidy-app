package kr.example.subsidycompare.domain

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

/** 계산 가정값. 정보 화면에도 그대로 표시됩니다. */
object Assumptions {
    const val CONTRACT_MONTHS = 24
    /** 통신사 단말 할부수수료 (연, %) — 통신사별로 다를 수 있음 */
    const val INSTALLMENT_ANNUAL_RATE_PCT = 5.9
    /** 선택약정 요금 할인율 */
    const val SELECT_DISCOUNT_RATE = 0.25
}

data class OptionCost(
    /** 단말 할부원금 */
    val devicePrincipal: Long,
    /** 월 단말 할부금 (수수료 포함) */
    val deviceMonthly: Long,
    /** 월 요금 (할인 반영) */
    val planMonthly: Long,
) {
    val totalMonthly: Long get() = deviceMonthly + planMonthly
}

enum class BetterOption { SUBSIDY, SELECT_DISCOUNT, SAME }

data class Comparison(
    val subsidy: OptionCost,
    val selectDiscount: OptionCost,
    val subsidyTotal: Long,
    val selectTotal: Long,
) {
    val better: BetterOption
        get() = when {
            subsidyTotal < selectTotal -> BetterOption.SUBSIDY
            selectTotal < subsidyTotal -> BetterOption.SELECT_DISCOUNT
            else -> BetterOption.SAME
        }
    val saving: Long get() = kotlin.math.abs(subsidyTotal - selectTotal)
    val bestTotal: Long get() = minOf(subsidyTotal, selectTotal)
}

object CostCalculator {

    /**
     * 원리금 균등 방식의 월 할부금(원).
     * 원금이 0 이하면 0, 이자율이 0이면 원금 / 개월수.
     */
    fun monthlyInstallment(principal: Long, months: Int, annualRatePct: Double): Long {
        if (principal <= 0 || months <= 0) return 0
        val r = annualRatePct / 100.0 / 12.0
        if (r == 0.0) return (principal.toDouble() / months).roundToLong()
        val payment = principal * r / (1 - (1 + r).pow(-months))
        return payment.roundToLong()
    }

    /**
     * 공시지원금(공통지원금) vs 선택약정 25% 를 계약 기간 총액으로 비교.
     *
     * 단통법 폐지 후에는 유통점 추가지원금을 선택약정과 함께 받을 수 있으므로
     * [extraSupport] 는 두 방식 모두에서 단말 가격을 낮추는 것으로 계산합니다.
     */
    fun compare(
        releasePrice: Int,
        monthlyFee: Int,
        subsidy: Int,
        extraSupport: Int = 0,
        months: Int = Assumptions.CONTRACT_MONTHS,
        annualRatePct: Double = Assumptions.INSTALLMENT_ANNUAL_RATE_PCT,
        selectDiscountRate: Double = Assumptions.SELECT_DISCOUNT_RATE,
    ): Comparison {
        val extra = max(0, extraSupport).toLong()

        val subsidyPrincipal = max(0L, releasePrice.toLong() - subsidy - extra)
        val subsidyOption = OptionCost(
            devicePrincipal = subsidyPrincipal,
            deviceMonthly = monthlyInstallment(subsidyPrincipal, months, annualRatePct),
            planMonthly = monthlyFee.toLong(),
        )

        val selectPrincipal = max(0L, releasePrice.toLong() - extra)
        val selectOption = OptionCost(
            devicePrincipal = selectPrincipal,
            deviceMonthly = monthlyInstallment(selectPrincipal, months, annualRatePct),
            planMonthly = (monthlyFee * (1 - selectDiscountRate)).roundToLong(),
        )

        return Comparison(
            subsidy = subsidyOption,
            selectDiscount = selectOption,
            subsidyTotal = subsidyOption.totalMonthly * months,
            selectTotal = selectOption.totalMonthly * months,
        )
    }
}
