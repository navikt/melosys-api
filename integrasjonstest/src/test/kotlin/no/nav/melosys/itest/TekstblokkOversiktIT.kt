package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkStatus
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

import java.time.Instant

/**
 * Dekker filtreringen i finnOversikt mot ekte Oracle. Spørringen bygger på
 * bundne parametere som ikke kan verifiseres med mocks – særlig statusfilteret,
 * som Hibernate må oversette til noe Oracle forstår.
 */
class TekstblokkOversiktIT(
    @Autowired val tekstblokkRepository: TekstblokkRepository,
    @Autowired val jdbcTemplate: JdbcTemplate,
) : ComponentTestBase() {

    // Tekstblokk-tabellene står ikke i basisklassens truncate-liste
    @BeforeEach
    fun tømTekstblokker() {
        listOf("TEKSTBLOKK_TAG", "TEKSTBLOKK_SAKSTYPE", "TEKSTBLOKK_BEHANDLINGSTEMA", "TEKSTBLOKK").forEach {
            jdbcTemplate.execute("DELETE FROM $it")
        }
    }

    @Test
    fun `inkluderUtkast tar med baade utkast og publiserte`() {
        lagreTestdata()

        val oversikt = tekstblokkRepository.finnOversikt(type = null, inkluderUtkast = true)

        oversikt.map { it.tittel } shouldContainExactly listOf("Alfa utkast", "Beta publisert", "Delta brevmal")
    }

    @Test
    fun `uten utkast kommer kun publiserte med`() {
        lagreTestdata()

        val oversikt = tekstblokkRepository.finnOversikt(type = null, inkluderUtkast = false)

        oversikt.map { it.tittel } shouldContainExactly listOf("Beta publisert", "Delta brevmal")
        oversikt.map { it.status } shouldContainExactly listOf(TekstblokkStatus.PUBLISERT, TekstblokkStatus.PUBLISERT)
    }

    @Test
    fun `type null gir baade tekstblokker og brevmaler`() {
        lagreTestdata()

        val oversikt = tekstblokkRepository.finnOversikt(type = null, inkluderUtkast = true)

        oversikt.map { it.type } shouldContainExactly
            listOf(TekstblokkType.TEKSTBLOKK, TekstblokkType.TEKSTBLOKK, TekstblokkType.BREVMAL)
    }

    @Test
    fun `type gitt gir kun den typen`() {
        lagreTestdata()

        val oversikt = tekstblokkRepository.finnOversikt(TekstblokkType.TEKSTBLOKK, inkluderUtkast = true)

        oversikt.map { it.tittel } shouldContainExactly listOf("Alfa utkast", "Beta publisert")
    }

    @Test
    fun `slettede blokker kommer aldri med`() {
        lagreTestdata()

        listOf(true, false).forEach { inkluderUtkast ->
            tekstblokkRepository.finnOversikt(type = null, inkluderUtkast = inkluderUtkast)
                .map { it.tittel } shouldNotContain "Charlie slettet"
            tekstblokkRepository.finnOversikt(TekstblokkType.TEKSTBLOKK, inkluderUtkast = inkluderUtkast)
                .map { it.tittel } shouldNotContain "Charlie slettet"
        }
    }

    // Lagres i motsatt rekkefølge av den forventede, så ORDER BY faktisk blir prøvd
    private fun lagreTestdata() {
        tekstblokkRepository.save(nyTekstblokk("Delta brevmal", TekstblokkType.BREVMAL, TekstblokkStatus.PUBLISERT))
        tekstblokkRepository.save(
            nyTekstblokk("Charlie slettet", TekstblokkType.TEKSTBLOKK, TekstblokkStatus.PUBLISERT)
                .apply { slettetDato = Instant.now() },
        )
        tekstblokkRepository.save(nyTekstblokk("Beta publisert", TekstblokkType.TEKSTBLOKK, TekstblokkStatus.PUBLISERT))
        tekstblokkRepository.save(nyTekstblokk("Alfa utkast", TekstblokkType.TEKSTBLOKK, TekstblokkStatus.UTKAST))
    }

    private fun nyTekstblokk(tittel: String, type: TekstblokkType, status: TekstblokkStatus) = Tekstblokk(
        tittel = tittel,
        innhold = "<p>$tittel</p>",
        type = type,
        status = status,
    ).apply {
        registrertDato = Instant.now()
        registrertAv = "Z999999"
        endretDato = Instant.now()
        endretAv = "Z999999"
    }
}
