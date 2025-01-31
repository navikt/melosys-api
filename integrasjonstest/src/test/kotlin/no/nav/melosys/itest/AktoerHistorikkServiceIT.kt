package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.aktoer.AktoerDto
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.aktoer.AktoerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

import java.time.Instant

class AktoerHistorikkServiceIT(
    @Autowired val aktoerHistorikkService: AktoerHistorikkService,
    @Autowired val aktoerService: AktoerService,
    @Autowired val fagsakRepository: FagsakRepository,
) : ComponentTestBase() {
    val saksnummer = "MEL-aktoerhistorikk"

    @Test
    fun testAktoerHistorikk() {
        val fagSak = lagFagsak(saksnummer)
        val aktoerDto = lagAktoerDto()

        val revisionCount = aktoerHistorikkService.hentAktørHistorikk(fagSak, Aktoersroller.BRUKER).size

        aktoerService.lagEllerOppdaterAktoer(fagSak, aktoerDto).also {
            aktoerHistorikkService.hentAktørHistorikk(fagSak, Aktoersroller.BRUKER)
                .shouldHaveSize(revisionCount + 1)
        }
    }

    private fun lagAktoerDto(): AktoerDto {
        return AktoerDto().apply {
            rolleKode = "BRUKER"
            institusjonsID = "institusjonsID"
            utenlandskPersonID = "utenlandskPersonID"
            orgnr = "orgnr"
            personIdent = "21075114491"
            fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
        }
    }

    private fun lagFagsak(saksnummer: String): Fagsak {
        return Fagsak(
            saksnummer, null, Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.OPPRETTET
        ).apply { leggTilRegisteringInfo() }
            .also { fagsakRepository.save(it) }
            .also {
                addCleanUpAction {
                    slettSakMedAvhengigheter(it.saksnummer)
                }
            }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
