package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import no.nav.melosys.service.tekstblokk.Endringstype
import no.nav.melosys.service.tekstblokk.TekstblokkHistorikkService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

import java.time.Instant

/**
 * Verifiserer at Envers-oppsettet (V166) faktisk skriver revisjoner for tekstblokker.
 * Hver lagring skjer i sin egen transaksjon, så den gir én revisjon hver.
 */
class TekstblokkHistorikkServiceIT(
    @Autowired val tekstblokkHistorikkService: TekstblokkHistorikkService,
    @Autowired val tekstblokkRepository: TekstblokkRepository,
) : ComponentTestBase() {

    @Test
    fun `hver lagring gir en ny versjon i historikken`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Første utgave"))
        val id = requireNotNull(lagret.id)

        lagret.tittel = "Andre utgave"
        tekstblokkRepository.save(lagret)

        val historikk = tekstblokkHistorikkService.hentHistorikk(id)

        historikk shouldHaveSize 2
        historikk.map { it.versjon } shouldContainExactly listOf(1, 2)
        historikk.map { it.tittel } shouldContainExactly listOf("Første utgave", "Andre utgave")
        historikk.map { it.endringstype } shouldContainExactly listOf(Endringstype.OPPRETTET, Endringstype.ENDRET)
        historikk.last().gyldigTil.shouldBeNull()
    }

    @Test
    fun `versjon paa tidspunkt gir teksten slik den var da`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Første utgave"))
        val id = requireNotNull(lagret.id)
        val førEndring = Instant.now()

        lagret.tittel = "Andre utgave"
        tekstblokkRepository.save(lagret)

        tekstblokkHistorikkService.hentVersjonPaaTidspunkt(id, førEndring)?.tittel shouldBe "Første utgave"
    }

    private fun nyTekstblokk(tittel: String) = Tekstblokk(
        tittel = tittel,
        innhold = "<p>$tittel</p>",
        type = TekstblokkType.TEKSTBLOKK,
        tags = mutableSetOf("historikk"),
    ).apply {
        registrertDato = Instant.now()
        registrertAv = "Z999999"
        endretDato = Instant.now()
        endretAv = "Z999999"
    }
}
