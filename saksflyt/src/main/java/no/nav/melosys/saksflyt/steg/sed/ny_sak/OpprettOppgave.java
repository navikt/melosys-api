package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

@Component("opprettoppgaveSedNySak")
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final GsakFasade gsakFasade;

    public OpprettOppgave(@Qualifier("system") GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
        String journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();

        boolean skalTilordnes = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).orElse(false);

        Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandlingstype)
            .setTilordnetRessurs(skalTilordnes ? prosessinstans.getData(SAKSBEHANDLER) : null)
            .setJournalpostId(journalpostID)
            .setAktørId(aktørID)
            .setSaksnummer(saksnummer)
            .build();

        String oppgaveId = gsakFasade.opprettOppgave(oppgave);
        log.info("Opprettet oppgave {} for behandling {}", oppgaveId, prosessinstans.getBehandling().getId());

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
