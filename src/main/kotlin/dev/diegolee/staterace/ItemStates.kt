package dev.diegolee.staterace

enum class ItemState { PENDING, PAID, CANCELLED }

/** "A=PAID,B=CANCELLED" 꼴의 문자열을 맵으로 읽고 쓴다. */
object ItemStates {
    fun parse(raw: String): Map<String, ItemState> =
        raw.split(",")
            .filter { it.isNotBlank() }
            .associate { part ->
                val (item, state) = part.split("=")
                item to ItemState.valueOf(state)
            }

    fun format(states: Map<String, ItemState>): String =
        states.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value}" }
}
