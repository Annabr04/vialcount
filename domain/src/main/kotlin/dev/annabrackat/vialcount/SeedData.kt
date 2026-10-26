package dev.annabrackat.vialcount

/**
 * Synthetic shelf lines for training. Not a live inventory extract.
 * SKUs are invented. There is no patient and no manufacturer NDC.
 */
object SeedData {
    fun lines(): List<CountLine> = listOf(
        CountLine(
            sku = "VC-1042",
            name = "Amoxicillin 500 mg capsules",
            location = "Bay A / Bin 12",
            expectedQty = 48,
        ),
        CountLine(
            sku = "VC-2188",
            name = "Cetirizine 10 mg tablets",
            location = "Bay B / Bin 04",
            expectedQty = 120,
        ),
        CountLine(
            sku = "VC-3301",
            name = "Ibuprofen 200 mg tablets",
            location = "Bay C / Bin 19",
            expectedQty = 30,
        ),
        CountLine(
            sku = "VC-4410",
            name = "Omeprazole 20 mg capsules",
            location = "Bay A / Bin 07",
            expectedQty = 16,
        ),
    )
}
