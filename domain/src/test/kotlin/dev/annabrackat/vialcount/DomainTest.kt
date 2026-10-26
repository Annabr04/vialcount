package dev.annabrackat.vialcount

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainTest {
    @Test
    fun withinThresholdAccepts() {
        // 5 percent of 100 is 5, which is larger than 2. A gap of 5 is inside.
        val byPercent = CycleCount.submitFirstCount(line(100), 95) as DomainResult.Ok
        assertEquals(Status.ACCEPTED, byPercent.value.status)
        assertEquals(95, byPercent.value.firstCount)

        // 5 percent of 10 is smaller than 2. A gap of 2 is inside because 2 is the larger allowance.
        val byUnits = CycleCount.submitFirstCount(line(10), 8) as DomainResult.Ok
        assertEquals(Status.ACCEPTED, byUnits.value.status)
        assertEquals(8, byUnits.value.firstCount)
    }

    @Test
    fun outsideThresholdAsksForRecount() {
        val wide = CycleCount.submitFirstCount(line(100), 94) as DomainResult.Ok
        assertEquals(Status.NEEDS_RECOUNT, wide.value.status)
        assertEquals(94, wide.value.firstCount)
        assertNull(wide.value.secondCount)

        val narrow = CycleCount.submitFirstCount(line(10), 7) as DomainResult.Ok
        assertEquals(Status.NEEDS_RECOUNT, narrow.value.status)
        assertEquals(7, narrow.value.firstCount)
    }

    @Test
    fun copiedRecountIsRejected() {
        val first = (CycleCount.submitFirstCount(line(100), 90) as DomainResult.Ok).value
        assertEquals(Status.NEEDS_RECOUNT, first.status)

        val copied = CycleCount.submitSecondCount(first, 90, Role.TECH)
        assertCode(copied, "same_count", 100)
        assertEquals(Status.NEEDS_RECOUNT, first.status)
        assertNull(first.secondCount)

        val copiedByLead = CycleCount.submitSecondCount(first, 90, Role.LEAD)
        assertCode(copiedByLead, "same_count", 100)
    }

    @Test
    fun techCannotAcceptAVariance() {
        val pending = line(80, Status.NEEDS_RECOUNT, first = 70)
        val result = CycleCount.acceptVariance(pending, Role.TECH, VarianceReason.DAMAGED)
        assertCode(result, "role", 80)
        assertEquals(Status.NEEDS_RECOUNT, pending.status)
        assertNull(pending.reason)
    }

    @Test
    fun leadCanAcceptWithAReason() {
        val pending = line(80, Status.NEEDS_RECOUNT, first = 70, second = 71)
        val result = CycleCount.acceptVariance(
            pending,
            Role.LEAD,
            VarianceReason.FOUND_IN_OTHER_BIN,
        ) as DomainResult.Ok
        assertEquals(Status.ACCEPTED, result.value.status)
        assertEquals(VarianceReason.FOUND_IN_OTHER_BIN, result.value.reason)
        assertEquals(70, result.value.firstCount)
        assertEquals(71, result.value.secondCount)
    }

    @Test
    fun negativeQtyFails() {
        val first = CycleCount.submitFirstCount(line(42), -1)
        assertCode(first, "negative", 42)

        val pending = line(42, Status.NEEDS_RECOUNT, first = 10)
        val second = CycleCount.submitSecondCount(pending, -3, Role.LEAD)
        assertCode(second, "negative", 42)
        assertEquals(Status.NEEDS_RECOUNT, pending.status)
    }

    @Test
    fun expectedStaysHiddenWhileOpen() {
        val open = line(80)
        assertNull(CycleCount.displayExpected(open, revealed = false))
        assertEquals(80, CycleCount.displayExpected(open, revealed = true))

        val accepted = (CycleCount.submitFirstCount(open, 80) as DomainResult.Ok).value
        assertEquals(Status.ACCEPTED, accepted.status)
        assertEquals(80, CycleCount.displayExpected(accepted, revealed = false))

        val recount = (CycleCount.submitFirstCount(open, 60) as DomainResult.Ok).value
        assertEquals(Status.NEEDS_RECOUNT, recount.status)
        assertEquals(80, CycleCount.displayExpected(recount, revealed = false))
    }

    @Test
    fun recountWithinThresholdAccepts() {
        val first = (CycleCount.submitFirstCount(line(100), 90) as DomainResult.Ok).value
        val second = CycleCount.submitSecondCount(first, 95, Role.TECH) as DomainResult.Ok
        assertEquals(Status.ACCEPTED, second.value.status)
        assertEquals(95, second.value.secondCount)
    }

    @Test
    fun recountStillOutsideStaysForTheLead() {
        val first = (CycleCount.submitFirstCount(line(100), 90) as DomainResult.Ok).value
        val second = CycleCount.submitSecondCount(first, 80, Role.LEAD) as DomainResult.Ok
        assertEquals(Status.NEEDS_RECOUNT, second.value.status)
        assertEquals(80, second.value.secondCount)
        assertNull(second.value.reason)
    }

    @Test
    fun leadCanRejectACount() {
        val pending = line(40, Status.NEEDS_RECOUNT, first = 10)
        val rejected = CycleCount.rejectCount(pending, Role.LEAD) as DomainResult.Ok
        assertEquals(Status.REJECTED, rejected.value.status)

        val tech = CycleCount.rejectCount(pending, Role.TECH)
        assertCode(tech, "role", 40)
        assertEquals(Status.NEEDS_RECOUNT, pending.status)
    }

    @Test
    fun missingReasonIsRejected() {
        val pending = line(40, Status.NEEDS_RECOUNT, first = 10)
        val result = CycleCount.acceptVariance(pending, Role.LEAD, null)
        assertCode(result, "reason", 40)
        assertEquals(Status.NEEDS_RECOUNT, pending.status)
    }

    @Test
    fun wrongStepIsRejected() {
        val open = line(40)
        assertCode(CycleCount.submitSecondCount(open, 40, Role.LEAD), "state", 40)
        assertCode(CycleCount.acceptVariance(open, Role.LEAD, VarianceReason.DATA_ERROR), "state", 40)
        assertCode(CycleCount.rejectCount(open, Role.LEAD), "state", 40)

        val accepted = (CycleCount.submitFirstCount(open, 40) as DomainResult.Ok).value
        assertCode(CycleCount.submitFirstCount(accepted, 40), "state", 40)
    }

    @Test
    fun seedIsFourBlindLines() {
        val lines = SeedData.lines()
        assertEquals(4, lines.size)
        assertEquals(4, lines.map { it.sku }.toSet().size)
        assertTrue(lines.all { it.status == Status.OPEN && it.sku.startsWith("VC-") })
        assertTrue(lines.all { CycleCount.displayExpected(it, revealed = false) == null })
        assertTrue(lines.none { it.name.isBlank() || it.location.isBlank() })
    }

    private fun line(
        expected: Int,
        status: Status = Status.OPEN,
        first: Int? = null,
        second: Int? = null,
    ) = CountLine(
        sku = "VC-1042",
        name = "Amoxicillin 500 mg capsules",
        location = "Bay A / Bin 12",
        expectedQty = expected,
        firstCount = first,
        secondCount = second,
        status = status,
    )

    private fun assertCode(result: DomainResult<*>, code: String, expectedQty: Int) {
        val err = assertIs<DomainResult.Err>(result)
        assertEquals(code, err.code)
        assertFalse(err.message.contains(expectedQty.toString()), err.message)
    }
}
