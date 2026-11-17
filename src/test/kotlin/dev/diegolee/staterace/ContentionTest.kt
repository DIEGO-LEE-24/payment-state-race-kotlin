package dev.diegolee.staterace

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * 경합 창을 일부러 벌리지 않고, 요청 수를 늘려 가며 고친 구현을 그대로 돌린다.
 * 결제 한 건에 몰리는 요청은 한 줄로 서므로 걸린 시간을 함께 찍어 둔다.
 * 시간은 기계마다 달라 단언하지 않고, 결과가 맞는지만 단언한다.
 */
class ContentionTest : PostgresTestBase() {

    @Autowired
    lateinit var safe: SafeStateChangeService

    @Test
    fun `고친 구현 - 한 결제에 요청이 몰려도 모든 변경이 남는다`() {
        for (threads in listOf(2, 8, 16)) {
            val id = threads.toLong()
            val items = (1..64).map { "I%02d".format(it) }
            payments.create(id, items.associateWith { ItemState.PENDING })

            val pool = Executors.newFixedThreadPool(threads)
            val elapsed = measureTimeMillis {
                items.mapIndexed { index, item ->
                    pool.submit {
                        if (index % 2 == 0) safe.confirm(id, item) else safe.cancel(id, item)
                    }
                }.forEach { it.get(30, TimeUnit.SECONDS) }
            }
            pool.shutdownNow()

            val expected = items.mapIndexed { index, item ->
                item to if (index % 2 == 0) ItemState.PAID else ItemState.CANCELLED
            }.toMap()
            assertThat(payments.read(id)).isEqualTo(expected)
            println("[safe · contention] threads=$threads requests=${items.size} elapsed=${elapsed}ms")
        }
    }
}
