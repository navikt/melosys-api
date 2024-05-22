package no.nav.melosys.saksflyt.steg.oppgave

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component

@Component
class GjenbrukOppgave(private val oppgaveService: OppgaveService) : StegBehandler {
    private val log = KotlinLogging.logger {}
    override fun inngangsSteg(): ProsessSteg = ProsessSteg.GJENBRUK_OPPGAVE

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val fagsak = behandling.fagsak
        val oppgaveID = prosessinstans.getData(ProsessDataKey.OPPGAVE_ID)
        val saksnummer = fagsak.saksnummer
        val gjenbruktOppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveID)
        val nyOppgave = oppgaveService.lagBehandlingsoppgave(behandling)
            .setSaksnummer(fagsak.saksnummer)
            .setTilordnetRessurs(prosessinstans.hentSaksbehandlerHvisTilordnes())
            .setAktørId(
                if (fagsak.hovedpartRolle == Aktoersroller.VIRKSOMHET) null else fagsak.hentBrukersAktørID()
            )
            .setOrgnr(
                if (fagsak.hovedpartRolle == Aktoersroller.VIRKSOMHET) fagsak.hentVirksomhet().orgnr else null
            )
            .setBeskrivelse(gjenbruktOppgave.beskrivelse)
            .build()
        val opprettetOppgaveID = oppgaveService.opprettOppgave(nyOppgave)
        log.info(
            "Opprettet ny oppgave med ID $opprettetOppgaveID til sak $saksnummer, med beskrivelse fra oppgave $oppgaveID"
        )
    }
}
