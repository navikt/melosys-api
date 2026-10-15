package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflyt.steg.jfr.OpprettArkivsak
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.sak.ArkivsakService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Oppretter arkivsak bare når digital søknad ga ny sak. Eksisterende sak har arkivsak fra før.
 */
@Component
class OpprettArkivsakDigitalSøknad(
    fagsakService: FagsakService,
    arkivsakService: ArkivsakService,
    oppgaveFactory: OppgaveFactory
) : StegBehandler {

    // Egen instans (ikke Spring-bønnen) slik at steget kjører i denne transaksjonen og ikke åpner en ny
    private val opprettArkivsak = OpprettArkivsak(fagsakService, arkivsakService, oppgaveFactory)

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_ARKIVSAK_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        if (prosessinstans.gjelderNySakForDigitalSøknad()) {
            opprettArkivsak.utfør(prosessinstans)
        } else {
            log.info { "Digital søknad gjelder eksisterende sak ${prosessinstans.hentBehandling.fagsak.saksnummer}, oppretter ikke arkivsak" }
        }
    }
}
