package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.OppgaveTilbakelegging;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Oppgaveplukker {

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
    public Optional<Oppgave> plukkOppgave(String saksbehandlerID, List<String> sakstyper, List<String> behandlingstyper) {

        // TODO Mapping til fagområde i GSAK må avklares
        List<String> fagområdeKodeListe = new ArrayList<>();
        fagområdeKodeListe.add("MED");
        fagområdeKodeListe.add("UFM");

        // TODO Mapping mellom sakstyper og underkategori. Vi trenger en liste (Yvonne).

        // TODO Mapping mellom behandlingstyper og oppgavetyper

        List<Oppgave> oppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(fagområdeKodeListe, sakstyper.get(0), behandlingstyper);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, oppgaver);

        if (valg.isPresent()) {
            gsakFasade.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
        }

        return valg;
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
        List<OppgaveTilbakelegging> tilbakelegging = oppgaveTilbakkeleggingRepo.findBySaksbehandlerAndOppgaveId(saksbehandlerID, oppgaveId);
        return !tilbakelegging.isEmpty();
    }

}
