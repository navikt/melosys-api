package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.GJENBRUK_OPPGAVE;

@Component
public class GjenbrukOppgave implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(GjenbrukOppgave.class);

    private final OppgaveService oppgaveService;

    public GjenbrukOppgave(OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GJENBRUK_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();
        final String oppgaveID = prosessinstans.getData(ProsessDataKey.OPPGAVE_ID);
        final String saksnummer = fagsak.getSaksnummer();

        final Oppgave gjenbruktOppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveID);
        final Oppgave nyOppgave = (oppgaveService.lagBehandlingsoppgave(behandling))
            .setSaksnummer(fagsak.getSaksnummer())
            .setTilordnetRessurs(prosessinstans.hentSaksbehandlerHvisTilordnes())
            .setAktørId(
                fagsak.getHovedpartRolle() == Aktoersroller.VIRKSOMHET
                    ? null
                    : fagsak.hentBrukersAktørID())
            .setOrgnr(
                fagsak.getHovedpartRolle() == Aktoersroller.VIRKSOMHET
                    ? fagsak.hentVirksomhet().getOrgnr()
                    : null)
            .setBeskrivelse(gjenbruktOppgave.getBeskrivelse())
            .build();

        final String opprettetOppgaveID = oppgaveService.opprettOppgave(nyOppgave);

        log.info("Opprettet ny oppgave med ID {} til sak {}, med beskrivelse fra oppgave {}", opprettetOppgaveID, saksnummer, oppgaveID);
    }
}
