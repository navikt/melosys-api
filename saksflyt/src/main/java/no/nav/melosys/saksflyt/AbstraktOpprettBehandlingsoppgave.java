package no.nav.melosys.saksflyt;

import java.util.Optional;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

public abstract class AbstraktOpprettBehandlingsoppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AbstraktOpprettBehandlingsoppgave.class);

    private final GsakFasade gsakFasade;

    protected AbstraktOpprettBehandlingsoppgave(GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    protected void opprettOppgave(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        boolean skalTilordnes = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).orElse(false);

        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(prosessinstans.getBehandling().getType())
            .setTilordnetRessurs(skalTilordnes ? prosessinstans.getData(SAKSBEHANDLER) : null)
            .setJournalpostId(journalpostId)
            .setAktørId(aktørId)
            .setSaksnummer(saksnummer)
            .build();

        String oppgaveId = gsakFasade.opprettOppgave(oppgave);
        log.info("Opprettet oppgave {} for PID {} og behandling {}", oppgaveId, prosessinstans.getId(), prosessinstans.getBehandling().getId());
    }
}
