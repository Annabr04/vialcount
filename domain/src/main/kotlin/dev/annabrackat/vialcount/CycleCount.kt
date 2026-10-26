package dev.annabrackat.vialcount

/**
 * Cycle-count rules for a stockroom line.
 * Expected failures come back as [DomainResult.Err]. They are not thrown.
 * Error text never includes the expected quantity.
 */
object CycleCount {
    fun submitFirstCount(line: CountLine, qty: Int): DomainResult<CountLine> {
        if (qty < 0) return err("negative", "Count must be zero or more.")
        if (line.status != Status.OPEN) return err("state", "That step does not apply to this line.")
        val status = if (exceedsAllowance(qty, line.expectedQty)) Status.NEEDS_RECOUNT else Status.ACCEPTED
        return DomainResult.Ok(line.copy(firstCount = qty, status = status))
    }

    fun submitSecondCount(line: CountLine, qty: Int, role: Role): DomainResult<CountLine> {
        if (qty < 0) return err("negative", "Count must be zero or more.")
        if (line.status != Status.NEEDS_RECOUNT || line.firstCount == null) {
            return err("state", "That step does not apply to this line.")
        }
        if (!mayRecount(role)) {
            return err("role", "Only a technician or a lead can submit a recount.")
        }
        if (qty == line.firstCount) {
            return err("same_count", "A recount that copies the first count is not a recount.")
        }
        val status = if (exceedsAllowance(qty, line.expectedQty)) Status.NEEDS_RECOUNT else Status.ACCEPTED
        return DomainResult.Ok(line.copy(secondCount = qty, status = status))
    }

    fun acceptVariance(line: CountLine, role: Role, reason: VarianceReason?): DomainResult<CountLine> {
        if (role != Role.LEAD) return err("role", "Only a lead can accept or reject a variance.")
        if (line.status != Status.NEEDS_RECOUNT) return err("state", "That step does not apply to this line.")
        if (reason == null) {
            return err("reason", "A variance needs a reason: damaged, found in another bin, or data error.")
        }
        return DomainResult.Ok(line.copy(status = Status.ACCEPTED, reason = reason))
    }

    fun rejectCount(line: CountLine, role: Role): DomainResult<CountLine> {
        if (role != Role.LEAD) return err("role", "Only a lead can accept or reject a variance.")
        if (line.status != Status.NEEDS_RECOUNT) return err("state", "That step does not apply to this line.")
        return DomainResult.Ok(line.copy(status = Status.REJECTED))
    }

    /** Null while the line is still open, unless the caller has already chosen to reveal it. */
    fun displayExpected(line: CountLine, revealed: Boolean): Int? {
        if (revealed || line.status != Status.OPEN) return line.expectedQty
        return null
    }

    /**
     * The allowance is the larger of 2 units and 5 percent of expected.
     * A count is outside only when it exceeds both.
     * Percent, in integer math: abs(counted - expected) * 100 > expected * 5.
     * A difference of exactly 2, or an exact 5 percent, is still inside.
     */
    private fun exceedsAllowance(counted: Int, expected: Int): Boolean {
        val delta = kotlin.math.abs(counted.toLong() - expected.toLong())
        val overUnits = delta > 2L
        val overPercent = delta * 100 > expected.toLong() * 5
        return overUnits && overPercent
    }

    /** Technicians and leads both recount. Acceptance and rejection stay with the lead. */
    private fun mayRecount(role: Role): Boolean = when (role) {
        Role.TECH, Role.LEAD -> true
    }

    private fun err(code: String, message: String) = DomainResult.Err(code, message)
}
