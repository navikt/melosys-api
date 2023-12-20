package no.nav.melosys.repository

import org.hibernate.envers.AuditReader
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

internal const val EUROPE_OSLO = "Europe/Oslo"

@Repository
class AuditRepository {
    @PersistenceContext
    private val entityManager: EntityManager? = null

    private val auditReader: AuditReader
        get() = AuditReaderFactory.get(entityManager)

    @Transactional(readOnly = true)
    fun <T> getRevisions(tClass: Class<T>, propertiesMap: Map<String, Any>): List<EntityRevision<T>> {
        require(propertiesMap.isNotEmpty()) { "Invalid params." }

        for ((key, value) in propertiesMap) {
            require(!(!StringUtils.hasText(key) || Objects.isNull(value))) { "Invalid property in map: key or value is null or empty." }
        }

        val auditQuery = auditReader.createQuery()
            .forRevisionsOfEntity(tClass, false, true)
        for ((key, value) in propertiesMap) {
            auditQuery.add(AuditEntity.property(key).eq(value))
        }

        val resultList = auditQuery.resultList as List<Array<Any>>
        return resultList.map { array -> EntityRevision(array[0] as T, array[1] as DefaultRevisionEntity, array[2] as RevisionType) }
    }
}

data class EntityRevision<T>(
    val entity: T,
    val revisionInfo: DefaultRevisionEntity,
    val revisionType: RevisionType
) {
    val revisionLocalDateTime : LocalDateTime
        get() = Instant.ofEpochMilli(this.revisionInfo.timestamp).atZone(ZoneId.of(EUROPE_OSLO)).toLocalDateTime()
}
