package no.nav.melosys.itest

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestWithTransactionHelper(@Autowired val entityManager: EntityManager) {
    @Transactional
    fun execute(vararg qList: Q) {
        for (q in qList) {
            val query = entityManager.createNativeQuery(q.statement)
            q.params.forEach {
                query.setParameter(it.first, it.second)
            }

            query.executeUpdate()
        }
    }

    data class Q(val statement: String, val params: Array<Pair<String, Any>>)
}
