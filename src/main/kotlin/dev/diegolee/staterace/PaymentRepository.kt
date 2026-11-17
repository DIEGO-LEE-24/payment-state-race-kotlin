package dev.diegolee.staterace

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PaymentRepository(private val jdbc: JdbcTemplate) {

    fun create(paymentId: Long, states: Map<String, ItemState>) {
        jdbc.update("INSERT INTO payment(id, item_states) VALUES (?, ?)", paymentId, ItemStates.format(states))
        jdbc.update("INSERT INTO payment_session(payment_id) VALUES (?)", paymentId)
    }

    fun read(paymentId: Long): Map<String, ItemState> =
        ItemStates.parse(
            jdbc.queryForObject("SELECT item_states FROM payment WHERE id = ?", String::class.java, paymentId)!!
        )

    fun write(paymentId: Long, states: Map<String, ItemState>) {
        jdbc.update("UPDATE payment SET item_states = ? WHERE id = ?", ItemStates.format(states), paymentId)
    }

    /** 상태를 바꾸려는 모든 경로가 먼저 지나가야 하는 문. 결제 행을 잠근다. */
    fun lockForStateWrite(paymentId: Long) {
        jdbc.queryForObject("SELECT id FROM payment WHERE id = ? FOR UPDATE", Long::class.java, paymentId)
    }

    /** 세션 행을 잠그고 처음 본 시각을 한 번만 기록한다. 상태 보호가 목적이 아니다. */
    fun stampFirstSeen(paymentId: Long) {
        jdbc.queryForObject(
            "SELECT payment_id FROM payment_session WHERE payment_id = ? FOR UPDATE", Long::class.java, paymentId
        )
        jdbc.update(
            "UPDATE payment_session SET first_seen_at = now() WHERE payment_id = ? AND first_seen_at IS NULL", paymentId
        )
    }

    fun deleteAll() {
        jdbc.update("DELETE FROM payment_session")
        jdbc.update("DELETE FROM payment")
    }
}
