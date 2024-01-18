package no.nav.melosys.service.aktoer

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val EUROPE_OSLO = "Europe/Oslo"

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
    fun `fullmektig endres, hent gjeldende aktører på et gitt tidspunkt`() {
        val bruker = lagAktør(1, Aktoersroller.BRUKER, "aktørID", registrert = LocalDate.of(2023, 11, 1), endret = LocalDate.of(2023, 11, 1))
        val fullmektigV1 = lagFullmektig(2, Fullmaktstype.FULLMEKTIG_SØKNAD, "888888888", registrert = LocalDate.of(2023, 12, 1), endret = LocalDate.of(2023, 12, 1))
        val fullmektigV2 = lagFullmektig(2, Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT, "999999999", registrert = LocalDate.of(2023, 12, 1), endret = LocalDate.of(2023, 12, 2))
        // Denne ville ikke returneres fordi etter 2/12/2023:
        // val aktoer3 = lagFullmektig(3, Fullmaktstype.FULLMEKTIG_SØKNAD, "333333333", LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 4))

        val entityRevision0 = entityRevision(0, bruker, RevisionType.ADD)
        val entityRevision1 = entityRevision(1, fullmektigV1, RevisionType.ADD)
        val entityRevision2 = entityRevision(2, fullmektigV2, RevisionType.MOD)
        val revisions = listOf(entityRevision0, entityRevision1, entityRevision2)

        every { auditRepository.getRevisionsBeforeOrAtDate(eq(Aktoer::class.java), any(), any()) } returns revisions


        val tidspunkt = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of("Europe/Oslo")).toLocalDateTime()
        val result = aktoerHistorikkService.hentGyldigeAktørerPåTidspunkt(Fagsak(), Aktoersroller.FULLMEKTIG, tidspunkt)


        result.shouldHaveSize(2)
        result.single { it.id == 1L} shouldBe bruker
        result.single { it.id == 2L} shouldBe fullmektigV2
    }

    @Test
    fun `fullmektiger slettes endres, hent gjeldende aktører på et gitt tidspunkt`() {
        val bruker = lagAktør(1, Aktoersroller.BRUKER, "aktørID", registrert = LocalDate.of(2023, 11, 1), endret = LocalDate.of(2023, 11, 1))
        val fullmektig1Slettes = lagFullmektig(2, Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT, "888888888", registrert = LocalDate.of(2023, 12, 1), endret = LocalDate.of(2023, 12, 1))
        val fullmektig2 = lagFullmektig(3, Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT, "999999999", registrert = LocalDate.of(2023, 12, 1), endret = LocalDate.of(2023, 12, 2))
        val fullmektig3 = lagFullmektig(4, Fullmaktstype.FULLMEKTIG_SØKNAD, "333333333", registrert = LocalDate.of(2023, 12, 1), endret = LocalDate.of(2023, 12, 4))

        val entityRevision0 = entityRevision(0, bruker, RevisionType.ADD)
        val entityRevision1 = entityRevision(1, fullmektig1Slettes, RevisionType.ADD)
        val entityRevision2 = entityRevision(2, fullmektig1Slettes, RevisionType.DEL, LocalDate.of(2023, 12, 3).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant())
        val entityRevision3 = entityRevision(3, fullmektig2, RevisionType.ADD)
        val entityRevision4 = entityRevision(4, fullmektig3, RevisionType.ADD)
        val revisions = listOf(entityRevision0, entityRevision1, entityRevision2, entityRevision3, entityRevision4)

        every { auditRepository.getRevisionsBeforeOrAtDate(eq(Aktoer::class.java), any(), any()) } returns revisions


        val tidspunkt = LocalDate.of(2023, 12, 4).atStartOfDay(ZoneId.of("Europe/Oslo")).toLocalDateTime()
        val result = aktoerHistorikkService.hentGyldigeAktørerPåTidspunkt(Fagsak(), Aktoersroller.FULLMEKTIG, tidspunkt)


        result.shouldHaveSize(3)
        result.find { it.id == 1L} shouldBe bruker
        result.find { it.id == 2L} shouldBe null
        result.find { it.id == 3L} shouldBe fullmektig2
        result.find { it.id == 4L} shouldBe fullmektig3
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

    private fun lagFullmektig(id: Long, fullmaktstype: Fullmaktstype, orgnr: String, registrert: LocalDate, endret: LocalDate): Aktoer {
        val aktoer = lagAktør(id, Aktoersroller.FULLMEKTIG, orgnr, registrert, endret)
        val fullmakt1 = Fullmakt().apply {
            this.aktoer = aktoer
            type = fullmaktstype
        }
        aktoer.apply {
            fullmakter = setOf(fullmakt1)
        }
        return aktoer
    }

    private fun lagAktør(
        id: Long,
        rolle: Aktoersroller,
        ident: String,
        registrert: LocalDate,
        endret: LocalDate
    ): Aktoer = Aktoer().apply {
        this.id = id
        registrertDato = registrert.atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()
        endretDato = endret.atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()
        aktørId = ident.takeIf { it.length != 9 }
        orgnr = ident.takeIf { it.length == 9 }

        this.rolle = rolle
    }

    private fun entityRevision(
        revID: Int,
        aktoer: Aktoer,
        revisionType: RevisionType,
        timestamp: Instant = aktoer.endretDato
    ): EntityRevision<Aktoer> {
        val revisionEntity = DefaultRevisionEntity().apply {
            id = revID
            this.timestamp = timestamp.toEpochMilli()
        }
        return EntityRevision(aktoer, revisionEntity, revisionType)
    }
}
