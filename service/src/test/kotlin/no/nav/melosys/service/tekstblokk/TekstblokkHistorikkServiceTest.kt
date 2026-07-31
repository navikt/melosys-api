package no.nav.melosys.service.tekstblokk

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
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
class TekstblokkHistorikkServiceTest {

    @MockK
    private lateinit var auditRepository: AuditRepository

    private lateinit var service: TekstblokkHistorikkService

    @BeforeEach
    fun setup() {
        service = TekstblokkHistorikkService(auditRepository)
    }

    @Test
    fun `versjonsnummer telles per blokk og tidsrommet lukkes av neste revisjon`() {
        every { auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to 1L)) } returns listOf(
            // Usortert inn: Envers garanterer ingen rekkefølge.
            revisjon(12, tekstblokk("Andre utgave"), RevisionType.MOD, dato(2)),
            revisjon(7, tekstblokk("Første utgave"), RevisionType.ADD, dato(1)),
        )

        val historikk = service.hentHistorikk(1L)

        historikk.map { it.versjon } shouldContainExactly listOf(1, 2)
        historikk.map { it.tittel } shouldContainExactly listOf("Første utgave", "Andre utgave")
        historikk[0].gyldigTil shouldBe historikk[1].gyldigFra
        historikk[1].gyldigTil.shouldBeNull()
    }

    @Test
    fun `endringstype utledes av revisjonstypen`() {
        every { auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to 1L)) } returns listOf(
            revisjon(1, tekstblokk("Tittel"), RevisionType.ADD, dato(1)),
            revisjon(2, tekstblokk("Tittel"), RevisionType.MOD, dato(2)),
        )

        service.hentHistorikk(1L).map { it.endringstype } shouldContainExactly
            listOf(Endringstype.OPPRETTET, Endringstype.ENDRET)
    }

    @Test
    fun `soft delete rapporteres som slettet selv om Envers ser en endring`() {
        every { auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to 1L)) } returns listOf(
            revisjon(1, tekstblokk("Tittel"), RevisionType.ADD, dato(1)),
            revisjon(2, tekstblokk("Tittel", slettet = true), RevisionType.MOD, dato(2)),
        )

        service.hentHistorikk(1L).last().endringstype shouldBe Endringstype.SLETTET
    }

    @Test
    fun `historikken tar med hvem som endret`() {
        every { auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to 1L)) } returns listOf(
            revisjon(1, tekstblokk("Tittel"), RevisionType.ADD, dato(1)),
        )

        val versjon = service.hentHistorikk(1L).single()

        versjon.endretAv shouldBe IDENT
        versjon.endretAvNavn shouldBe NAVN
    }

    @Test
    fun `versjon paa tidspunkt er siste revisjon til og med tidspunktet`() {
        every { auditRepository.getRevisionsBeforeOrAtDate(Tekstblokk::class.java, mapOf("id" to 1L), dato(2)) } returns listOf(
            revisjon(1, tekstblokk("Første utgave"), RevisionType.ADD, dato(1)),
            revisjon(2, tekstblokk("Andre utgave"), RevisionType.MOD, dato(2)),
        )

        val versjon = service.hentVersjonPaaTidspunkt(1L, dato(2))

        versjon.shouldNotBeNull()
        versjon.tittel shouldBe "Andre utgave"
        versjon.versjon shouldBe 2
    }

    @Test
    fun `versjon paa tidspunkt er null naar blokken var slettet`() {
        every { auditRepository.getRevisionsBeforeOrAtDate(Tekstblokk::class.java, mapOf("id" to 1L), dato(3)) } returns listOf(
            revisjon(1, tekstblokk("Tittel"), RevisionType.ADD, dato(1)),
            revisjon(2, tekstblokk("Tittel", slettet = true), RevisionType.MOD, dato(2)),
        )

        service.hentVersjonPaaTidspunkt(1L, dato(3)).shouldBeNull()
    }

    // To lagringer rett etter hverandre havner gjerne på samme millisekund; revisjonsnummeret er monotont
    @Test
    fun `rekkefolgen folger revisjonsnummeret naar tidsstemplene er like`() {
        every { auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to 1L)) } returns listOf(
            revisjon(12, tekstblokk("Andre utgave"), RevisionType.MOD, dato(1)),
            revisjon(7, tekstblokk("Første utgave"), RevisionType.ADD, dato(1)),
        )

        service.hentHistorikk(1L).map { it.tittel } shouldContainExactly listOf("Første utgave", "Andre utgave")
    }

    @Test
    fun `blokk uten revisjoner gir tom historikk`() {
        every { auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to 1L)) } returns emptyList()

        service.hentHistorikk(1L) shouldHaveSize 0
    }

    private fun dato(dagIDesember: Int): Instant =
        LocalDate.of(2026, 12, dagIDesember).atStartOfDay(ZoneId.of(EUROPE_OSLO)).toInstant()

    private fun tekstblokk(tittel: String, slettet: Boolean = false) = Tekstblokk(
        id = 1L,
        tittel = tittel,
        innhold = "<p>$tittel</p>",
        endretAvNavn = NAVN,
        slettetDato = if (slettet) Instant.now() else null,
    ).apply { endretAv = IDENT }

    private fun revisjon(revID: Int, tekstblokk: Tekstblokk, revisionType: RevisionType, tidspunkt: Instant) =
        EntityRevision(
            tekstblokk,
            DefaultRevisionEntity().apply {
                id = revID
                timestamp = tidspunkt.toEpochMilli()
            },
            revisionType,
        )

    private companion object {
        const val IDENT = "A146170"
        const val NAVN = "Margareth Bjørgum"
    }
}
