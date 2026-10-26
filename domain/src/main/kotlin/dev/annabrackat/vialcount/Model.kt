package dev.annabrackat.vialcount

enum class Role { TECH, LEAD }

enum class Status { OPEN, NEEDS_RECOUNT, ACCEPTED, REJECTED }

enum class VarianceReason { DAMAGED, FOUND_IN_OTHER_BIN, DATA_ERROR }

data class CountLine(
    val sku: String,
    val name: String,
    val location: String,
    val expectedQty: Int,
    val firstCount: Int? = null,
    val secondCount: Int? = null,
    val status: Status = Status.OPEN,
    val reason: VarianceReason? = null,
)

sealed interface DomainResult<out T> {
    data class Ok<T>(val value: T) : DomainResult<T>
    data class Err(val code: String, val message: String) : DomainResult<Nothing>
}
