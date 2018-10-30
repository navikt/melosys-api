// FIXME: Må flyttes ned til relevant pakke
package no.nav.melosys.saksflyt.agent;

import java.util.Map;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;

/**
 * Oppretter en oppgave i GSAK.
 *
 * Transisjoner:
 * GSAK_OPPRETT_OPPGAVE -> SEND_FORVALTNINGSMELDING eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(@Qualifier("system")GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
        log.info("OpprettOppgave initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return GSAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandlingstype behandlingstype = prosessinstans.getBehandling().getType(); // Forutsetter at ingen tidligere steg har endret denne
        String aktørID = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        Oppgave oppgave = new Oppgave();

        if (behandlingstype == Behandlingstype.SØKNAD) {
            oppgave.setTema(Tema.MED);
            oppgave.setBehandlingstype(Behandlingstype.SØKNAD);
            oppgave.setOppgavetype(Oppgavetype.BEH_SAK);
        } else {
            String feilmelding = "behandlingstype " + behandlingstype + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        // FIXME: MELOSYS-1401 behandlingstema,temagruppe
        oppgave.setAktørId(aktørID);
        oppgave.setJournalpostId(journalpostID);
        oppgave.setPrioritet(PrioritetType.NORM);
        oppgave.setSaksnummer(saksnummer);

        String oppgaveId = gsakFasade.opprettOppgave(oppgave);

        prosessinstans.setSteg(SEND_FORVALTNINGSMELDING);
        log.info("Opprettet oppgave {} for prosessinstans {}", oppgaveId, prosessinstans.getId());
    }
}
