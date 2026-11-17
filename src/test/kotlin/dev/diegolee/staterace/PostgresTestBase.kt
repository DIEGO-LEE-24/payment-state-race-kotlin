package dev.diegolee.staterace

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
abstract class PostgresTestBase {

    @Autowired
    protected lateinit var payments: PaymentRepository

    @BeforeEach
    fun cleanTables() = payments.deleteAll()

    companion object {
        // 시험 클래스가 여럿이어도 컨테이너는 하나만 띄우고 JVM이 끝날 때 정리된다.
        private val postgres = PostgreSQLContainer("postgres:16-alpine").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
