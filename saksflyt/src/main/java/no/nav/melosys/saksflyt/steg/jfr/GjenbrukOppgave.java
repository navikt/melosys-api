package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Optional;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.GJENBRUK_OPPGAVE;

@Component
public class GjenbrukOppgave implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(GjenbrukOppgave.class);

    private final OppgaveService oppgaveService;

    @Autowired
    public GjenbrukOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GJENBRUK_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        final String oppgaveID = prosessinstans.getData(ProsessDataKey.OPPGAVE_ID);
        final String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        final String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        final Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
        final Behandlingstema behandlingstema = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class);
        final boolean skalTilordnes = Optional.ofNullable(prosessinstans.getData(ProsessDataKey.SKAL_TILORDNES, Boolean.class)).orElse(false);

        final Oppgave gjenbruktOppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveID);

        final Oppgave nyOppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandlingstema, behandlingstype)
            .setSaksnummer(saksnummer)
            .setTilordnetRessurs(skalTilordnes ? prosessinstans.getData(SAKSBEHANDLER) : null)
            .setAktørId(aktørID)
            .setBeskrivelse(gjenbruktOppgave.getBeskrivelse())
            .build();

        final String opprettetOppgaveID = oppgaveService.opprettOppgave(nyOppgave);

        log.info("PID {} har opprettet ny oppgave med ID {} til sak {}, med beskrivelse fra oppgave {}", prosessinstans.getId(), opprettetOppgaveID, saksnummer, oppgaveID);
    }
}
