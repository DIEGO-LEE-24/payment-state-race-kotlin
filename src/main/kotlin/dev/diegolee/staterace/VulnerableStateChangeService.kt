package dev.diegolee.staterace

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 결함이 있는 구현.
 *
 * 확정 경로는 "처음 본 시각"을 찍으려고 세션 행을 잠근다. 그 잠금이 부수 효과로
 * 확정끼리는 한 줄로 세운다. 그래서 확정만 동시에 보내는 시험은 통과한다.
 * 취소 경로는 그 잠금을 지나가지 않는다. 확정과 취소가 겹치면 둘 다 같은 옛 값을
 * 읽고, 나중에 쓴 쪽이 먼저 쓴 쪽의 변경을 덮어쓴다.
 */
@Service
class VulnerableStateChangeService(private val payments: PaymentRepository) : StateChangeService {

    @Transactional
    override fun confirm(paymentId: Long, item: String, window: RaceWindow) {
        payments.stampFirstSeen(paymentId)
        val states = payments.read(paymentId).toMutableMap()
        window.betweenReadAndWrite()
        states[item] = ItemState.PAID
        payments.write(paymentId, states)
    }

    @Transactional
    override fun cancel(paymentId: Long, item: String, window: RaceWindow) {
        val states = payments.read(paymentId).toMutableMap()
        window.betweenReadAndWrite()
        states[item] = ItemState.CANCELLED
        payments.write(paymentId, states)
    }
}
