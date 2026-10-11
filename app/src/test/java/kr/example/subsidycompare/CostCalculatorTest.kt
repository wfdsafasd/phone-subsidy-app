package kr.example.subsidycompare

import kr.example.subsidycompare.domain.BetterOption
import kr.example.subsidycompare.domain.CostCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class CostCalculatorTest {

    @Test
    fun installment_zeroRate_isEvenSplit() {
        assertEquals(50_000L, CostCalculator.monthlyInstallment(1_200_000, 24, 0.0))
    }

    @Test
    fun installment_withRate_matchesAnnuityFormula() {
        // 1,200,000원, 24개월, 연 5.9% → 월 53,131원
        assertEquals(53_131L, CostCalculator.monthlyInstallment(1_200_000, 24, 5.9))
    }

    @Test
    fun installment_nonPositivePrincipal_isZero() {
        assertEquals(0L, CostCalculator.monthlyInstallment(0, 24, 5.9))
        assertEquals(0L, CostCalculator.monthlyInstallment(-10, 24, 5.9))
    }

    @Test
    fun highSubsidy_favorsSubsidyOption() {
        val c = CostCalculator.compare(releasePrice = 1_200_000, monthlyFee = 89_000, subsidy = 600_000)
        assertEquals(BetterOption.SUBSIDY, c.better)
    }

    @Test
    fun lowSubsidy_favorsSelectDiscount() {
        val c = CostCalculator.compare(releasePrice = 1_200_000, monthlyFee = 89_000, subsidy = 200_000)
        assertEquals(BetterOption.SELECT_DISCOUNT, c.better)
    }

    @Test
    fun subsidyLargerThanPrice_clampsToZeroPrincipal() {
        val c = CostCalculator.compare(releasePrice = 300_000, monthlyFee = 50_000, subsidy = 400_000)
        assertEquals(0L, c.subsidy.devicePrincipal)
        assertEquals(0L, c.subsidy.deviceMonthly)
    }
}
