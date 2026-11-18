package com.example.domain.islamic

data class ZakatResult(
    val totalAssets: Double,
    val totalDebts: Double,
    val netZakatableWealth: Double,
    val nisabThreshold: Double,
    val isEligible: Boolean,
    val payableZakat: Double
)

object ZakatCalculator {
    const val GOLD_NISAB_GRAMS = 87.48
    const val SILVER_NISAB_GRAMS = 612.36

    fun calculate(
        cashAndBank: Double,
        goldValue: Double,
        silverValue: Double,
        investmentsAndShares: Double,
        businessInventory: Double,
        shortTermDebts: Double,
        silverPricePerGramBdt: Double = 145.0
    ): ZakatResult {
        val totalAssets = (cashAndBank + goldValue + silverValue + investmentsAndShares + businessInventory)
            .coerceAtLeast(0.0)
        val debts = shortTermDebts.coerceAtLeast(0.0)
        val netWealth = (totalAssets - debts).coerceAtLeast(0.0)
        val nisab = SILVER_NISAB_GRAMS * silverPricePerGramBdt.coerceAtLeast(1.0)
        val eligible = netWealth >= nisab
        val zakat = if (eligible) netWealth * 0.025 else 0.0
        return ZakatResult(
            totalAssets = totalAssets,
            totalDebts = debts,
            netZakatableWealth = netWealth,
            nisabThreshold = nisab,
            isEligible = eligible,
            payableZakat = zakat
        )
    }
}
