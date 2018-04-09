package no.nav.melosys.service.oppgave;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.OppgaveTilbakelegging;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Oppgaveplukker {

    Logger log =  LoggerFactory.getLogger(Oppgaveplukker.class);

    private GsakFasade gsakFasade;

    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    @Autowired
    public Oppgaveplukker(GsakFasade gsakFasade, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo) {
        this.gsakFasade = gsakFasade;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
    }

    /**
     * 1) Oppgaveplukker henter i GSAK en liste over alle aktive, ikke tildelte oppgaver med oppgitt parametre.
     * 2) Oppgaveplukker velger neste oppgave basert på prioritet (først) og frist.
     * 3) Oppgaveplukker tildeler oppgaven til saksbehandleren..
     */
    public Optional<Oppgave> plukkOppgave(String saksbehandlerID, String oppgavetype, List<String> sakstyper, List<String> behandlingstyper) {

        // TODO Mapping med fagområde i GSAK må avklares
        List<String> fagområdeKodeListe = new ArrayList<>();
        fagområdeKodeListe.add("MED");
        fagområdeKodeListe.add("UFM");

        List<Oppgave> oppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(oppgavetype, fagområdeKodeListe, sakstyper, behandlingstyper);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, oppgaver);

        if (valg.isPresent()) {
            gsakFasade.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
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

    private Optional<Oppgave> velgNeste(String saksbehandlerID, List<Oppgave> oppgaver) {
        // Oppgaver med høy prioritet velges først.
        Optional<Oppgave> prioritert = oppgaver.stream().filter(oppgave -> oppgave.harHøyPrioritet()).findFirst();

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
