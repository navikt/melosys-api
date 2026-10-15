package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflyt.steg.oppgave.OpprettOppgave
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Oppretter behandlingsoppgave bare når digital søknad ga ny sak. På eksisterende sak har
 * [DigitalSøknadEksisterendeSakHåndterer] allerede opprettet oppgave der det trengs.
 */
@Component
class OpprettOppgaveDigitalSøknad(oppgaveService: OppgaveService) : StegBehandler {

    // Egen instans (ikke Spring-bønnen) slik at steget kjører i denne transaksjonen og ikke åpner en ny
    private val opprettOppgave = OpprettOppgave(oppgaveService)

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_OPPGAVE_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        if (prosessinstans.gjelderNySakForDigitalSøknad()) {
            opprettOppgave.utfør(prosessinstans)
        } else {
            log.info { "Digital søknad gjelder eksisterende sak ${prosessinstans.hentBehandling.fagsak.saksnummer}, oppretter ikke oppgave" }
        }
    }
}
