package no.nav.melosys.service.oppgave;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.OppgaveTilbakelegging;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Oppgaveplukker {

    Logger log =  LoggerFactory.getLogger(Oppgaveplukker.class);

    private GsakFasade gsakFasade;
    private FagsakRepository fagsakRepository;
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    @Autowired
    public Oppgaveplukker(GsakFasade gsakFasade, FagsakRepository fagsakRepository, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
    }

    /**
     * 1) Oppgaveplukker henter i GSAK en liste over alle aktive, ikke tildelte oppgaver med oppgitt parametre.
     * 2) Oppgaveplukker velger neste oppgave basert på prioritet (først) og frist.
     * 3) Oppgaveplukker tildeler oppgaven til saksbehandleren.
     * 4) Melosys saksnummer knyttes til oppgaven hvis oppgaven er en behandlingsoppgave.
     */
    public Optional<Oppgave> plukkOppgave(String saksbehandlerID, Oppgavetype oppgavetype, List<String> sakstyper, List<String> behandlingstyper) {

        // TODO Vi må håndtere tema for journalføringsoppgaver
        List<String> fagområdeKodeListe = new ArrayList<>();
        fagområdeKodeListe.add("MED");
        fagområdeKodeListe.add("UFM");

        List<Oppgave> oppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(oppgavetype, fagområdeKodeListe, sakstyper, behandlingstyper);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, oppgaver);

        if (valg.isPresent()) {
            Oppgave oppgave = valg.get();
            // Tildeler oppgaven
            gsakFasade.tildelOppgave(oppgave.getOppgaveId(), saksbehandlerID);

            if (oppgave.erBehandling()) {
                Fagsak fagsak = fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer());
                if (fagsak == null) {
                    throw new RuntimeException("Fant ikke fagsak med Gsak saksnummer " + oppgave.getGsakSaksnummer());
                }
                oppgave.setSaksnummer(fagsak.getSaksnummer());
            }
        }

        return valg;
    }

    public void leggTilbakeOppgave(String oppgaveId, String saksbehandlerID, String begrunnelse) {
        Oppgave oppgave = gsakFasade.hentOppgave(oppgaveId);

        if (oppgave == null) {
            log.error("Fant ikke oppgave med oppgaveId " + oppgaveId);
            throw new RuntimeException("Fant ikke oppgave med oppgaveId " + oppgaveId);
        }

        try {
            gsakFasade.leggTilbakeOppgave(oppgave);

            OppgaveTilbakelegging oppgaveTilbakelegging = new OppgaveTilbakelegging();
            oppgaveTilbakelegging.setOppgaveId(oppgave.getOppgaveId());
            oppgaveTilbakelegging.setSaksbehandlerId(saksbehandlerID);
            oppgaveTilbakelegging.setBegrunnelse(begrunnelse);
            oppgaveTilbakelegging.setRegistrertDato(LocalDateTime.now());
            oppgaveTilbakkeleggingRepo.save(oppgaveTilbakelegging);
        } catch (IntegrasjonException | SikkerhetsbegrensningException | TekniskException e) {
            log.error("Tilbakelegging av oppgave med oppgaveId " + oppgaveId + " feilet");
            throw new RuntimeException("Tilbakelegging av oppgave med oppgaveId " + oppgaveId + " feilet");
        }
    }

    // FIXME Dette er for å hjelpe testing av oppgavehåndtering.
    public void fjernTildeling() {
        gsakFasade.fjernTildeling();
    }

    private Optional<Oppgave> velgNeste(String saksbehandlerID, List<Oppgave> oppgaver) {
        // Oppgaver med høy prioritet velges først.
        Optional<Oppgave> prioritert = oppgaver.stream().filter(Oppgave::harHøyPrioritet).findFirst();

        Optional<Oppgave> valg;
        if (prioritert.isPresent()) {
            valg = prioritert;
        } else {
            // Oppgaver er sortert stigende etter frist.
            valg = oppgaver.stream().findFirst();
        }

        // Vi må ikke tildele en oppgave som var tilbakelagt.
        if (valg.isPresent()) {
            String oppgaveId = valg.get().getOppgaveId();
            if (erTilbakeLagt(saksbehandlerID, oppgaveId)) {
                oppgaver.remove(valg.get());
                return velgNeste(saksbehandlerID, oppgaver);
            }
        }

        return valg;
    }

    // Sjekker tabellen for tilbakelegging.
    private boolean erTilbakeLagt(String saksbehandlerID, String oppgaveId) {
        List<OppgaveTilbakelegging> tilbakelegging = oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(saksbehandlerID, oppgaveId);
        return !tilbakelegging.isEmpty();
    }

}
