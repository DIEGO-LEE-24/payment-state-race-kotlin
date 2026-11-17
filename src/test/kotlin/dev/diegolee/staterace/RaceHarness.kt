package dev.diegolee.staterace

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 두 요청을 같은 순간에 보내고, 둘 다 "읽은 뒤 · 쓰기 전"에서 만나게 한다.
 *
 * 한쪽이 잠금에 막혀 그 지점에 오지 못하면(고친 구현, 또는 같은 잠금을 타는 두 확정)
 * 먼저 온 쪽은 [meetTimeoutMillis]만 기다린 뒤 혼자 진행한다.
 */
class RaceHarness(private val meetTimeoutMillis: Long = 200) {

    fun runTogether(first: (RaceWindow) -> Unit, second: (RaceWindow) -> Unit) {
        val meetingPoint = CyclicBarrier(2)
        val window = RaceWindow {
            try {
                meetingPoint.await(meetTimeoutMillis, TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
                // 상대가 오지 않았다. 혼자 진행한다.
            }
        }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val a = pool.submit { first(window) }
            val b = pool.submit { second(window) }
            a.get(10, TimeUnit.SECONDS)
            b.get(10, TimeUnit.SECONDS)
        } finally {
            pool.shutdownNow()
        }
    }
}
