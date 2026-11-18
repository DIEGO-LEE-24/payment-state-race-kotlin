package dev.diegolee.staterace

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * 시험용 PostgreSQL.
 * - TEST_DB_URL 환경 변수가 있으면 그 DB를 쓴다(CI의 서비스 컨테이너).
 * - 없으면 Testcontainers로 하나 띄운다(로컬). 시험 클래스가 여럿이어도 하나만 띄운다.
 */
@SpringBootTest
abstract class PostgresTestBase {

    @Autowired
    protected lateinit var payments: PaymentRepository

    @BeforeEach
    fun cleanTables() = payments.deleteAll()

    companion object {
        private val externalUrl: String? = System.getenv("TEST_DB_URL")

        private val container: PostgreSQLContainer<*>? =
            if (externalUrl == null) PostgreSQLContainer("postgres:16-alpine").apply { start() } else null

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { externalUrl ?: container!!.jdbcUrl }
            registry.add("spring.datasource.username") { System.getenv("TEST_DB_USER") ?: container!!.username }
            registry.add("spring.datasource.password") { System.getenv("TEST_DB_PASSWORD") ?: container!!.password }
        }
    }
}
