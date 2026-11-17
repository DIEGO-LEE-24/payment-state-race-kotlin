package dev.diegolee.staterace

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * 같은 시험 두 벌을 결함 있는 구현과 고친 구현에 각각 돌린다.
 *
 * 한 라운드: 결제 한 건에 항목 A(PENDING), B(PAID)가 있다.
 * - 혼합: A 확정과 B 취소를 동시에 보낸다. 기대 결과는 A=PAID, B=CANCELLED.
 * - 단일: A 확정과 B 확정을 동시에 보낸다(B는 PENDING에서 시작). 기대 결과는 둘 다 PAID.
 */
class LostUpdateTest : PostgresTestBase() {

    @Autowired
    lateinit var vulnerable: VulnerableStateChangeService

    @Autowired
    lateinit var safe: SafeStateChangeService

    private val rounds = 40
    private val harness = RaceHarness()

    @Test
    fun `결함 구현 - 확정만 동시에 보내면 유실이 드러나지 않는다`() {
        val lost = countLostRounds(vulnerable, mixed = false)
        println("[vulnerable · confirm+confirm] lost $lost / $rounds")
        assertThat(lost).isZero()
    }

    @Test
    fun `결함 구현 - 확정과 취소를 섞으면 매 라운드 한쪽 변경이 사라진다`() {
        val lost = countLostRounds(vulnerable, mixed = true)
        println("[vulnerable · confirm+cancel] lost $lost / $rounds")
        assertThat(lost).isEqualTo(rounds)
    }

    @Test
    fun `고친 구현 - 확정과 취소를 섞어도 유실이 없다`() {
        val lost = countLostRounds(safe, mixed = true)
        println("[safe · confirm+cancel] lost $lost / $rounds")
        assertThat(lost).isZero()
    }

    @Test
    fun `고친 구현 - 확정만 보내도 유실이 없다`() {
        val lost = countLostRounds(safe, mixed = false)
        println("[safe · confirm+confirm] lost $lost / $rounds")
        assertThat(lost).isZero()
    }

    private fun countLostRounds(service: StateChangeService, mixed: Boolean): Int {
        var lost = 0
        for (round in 1..rounds) {
            val id = round.toLong()
            val startB = if (mixed) ItemState.PAID else ItemState.PENDING
            payments.create(id, mapOf("A" to ItemState.PENDING, "B" to startB))

            harness.runTogether(
                first = { window -> service.confirm(id, "A", window) },
                second = { window ->
                    if (mixed) service.cancel(id, "B", window) else service.confirm(id, "B", window)
                },
            )

            val expected = mapOf("A" to ItemState.PAID, "B" to if (mixed) ItemState.CANCELLED else ItemState.PAID)
            if (payments.read(id) != expected) lost++
        }
        return lost
    }
}
