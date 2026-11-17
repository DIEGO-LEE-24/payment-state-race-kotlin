package dev.diegolee.staterace

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 고친 구현. 상태를 바꾸는 모든 경로가 같은 문(lockForStateWrite)을 먼저 지나간다.
 * 읽기-고치기-쓰기가 결제 한 건 안에서는 한 번에 하나씩만 일어난다.
 */
@Service
class SafeStateChangeService(private val payments: PaymentRepository) : StateChangeService {

    @Transactional
    override fun confirm(paymentId: Long, item: String, window: RaceWindow) {
        payments.lockForStateWrite(paymentId)
        payments.stampFirstSeen(paymentId)
        change(paymentId, item, ItemState.PAID, window)
    }

    @Transactional
    override fun cancel(paymentId: Long, item: String, window: RaceWindow) {
        payments.lockForStateWrite(paymentId)
        change(paymentId, item, ItemState.CANCELLED, window)
    }

    private fun change(paymentId: Long, item: String, to: ItemState, window: RaceWindow) {
        val states = payments.read(paymentId).toMutableMap()
        window.betweenReadAndWrite()
        states[item] = to
        payments.write(paymentId, states)
    }
}
