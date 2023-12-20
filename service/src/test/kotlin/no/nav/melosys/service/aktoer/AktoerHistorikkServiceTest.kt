package no.nav.melosys.service.aktoer

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Fullmakt
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.repository.AuditRepository
import no.nav.melosys.repository.EntityRevision
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
internal class AktoerHistorikkServiceTest {
    @MockK
    private lateinit var auditRepository: AuditRepository

    private lateinit var aktoerHistorikkService: AktoerHistorikkService

    @BeforeEach
    fun setup() {
        aktoerHistorikkService = AktoerHistorikkService(auditRepository)
    }

    @Test
    fun `hentAktørHistorikk returns correct historikk`() {
        val aktoer1 = Aktoer().apply {
            id = 1
            registrertDato = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()
            endretDato = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()
            orgnr = "888888888"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val fullmakt1 = Fullmakt().apply {
            aktoer = aktoer1
            type = Fullmaktstype.FULLMEKTIG_SØKNAD
        }
        aktoer1.apply {
            fullmakter = setOf(fullmakt1)
        }
        val aktoer2 = Aktoer().apply {
            id = 1
            registrertDato = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()
            endretDato = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()
            orgnr = "999999999"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val fullmakt2 = Fullmakt().apply {
            aktoer = aktoer2
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        aktoer2.apply {
            fullmakter = setOf(fullmakt2)
        }

        val revisionEntity1 = DefaultRevisionEntity().apply {
            id = 1
            timestamp = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant().toEpochMilli()
        }
        val revisionEntity2 = DefaultRevisionEntity().apply {
            id = 2
            timestamp = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant().toEpochMilli()
        }

        val entityRevision1 = EntityRevision(aktoer1, revisionEntity1, RevisionType.ADD)
        val entityRevision2 = EntityRevision(aktoer2, revisionEntity2, RevisionType.MOD)
        val revisions = listOf(entityRevision1, entityRevision2)

        every { auditRepository.getRevisions(eq(Aktoer::class.java), any()) } returns revisions


        val result = aktoerHistorikkService.hentAktørHistorikk(Fagsak(), Aktoersroller.FULLMEKTIG)


        result.shouldContainExactly(
            AktoerHistorikk(
                registrertFra = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toLocalDateTime(),
                registretTil = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toLocalDateTime(),
                rolle = Aktoersroller.FULLMEKTIG,
                orgnr = "888888888",
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
            ),
            AktoerHistorikk(
                registrertFra = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toLocalDateTime(),
                registretTil = null,
                rolle = Aktoersroller.FULLMEKTIG,
                orgnr = "999999999",
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            )
        )
    }

}
