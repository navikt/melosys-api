package no.nav.melosys.service.oppgave;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsakstype;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.util.KodeverkUtils;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Oppgaveplukker {

    private static final Logger log =  LoggerFactory.getLogger(Oppgaveplukker.class);

    private final GsakFasade gsakFasade;
    private final FagsakRepository fagsakRepository;
    private final OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    @Autowired
    public Oppgaveplukker(GsakFasade gsakFasade, FagsakRepository fagsakRepository, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo) {
        this.gsakFasade = gsakFasade;
        this.fagsakRepository = fagsakRepository;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
    }

    /**
     * 1) Input valideres
     * 2) Oppgaveplukker henter i GSAK en liste over alle aktive, ikke tildelte oppgaver med oppgitt parametre.
     * 3) Neste oppgave velges basert på prioritet (først) og frist.
     * 4) Oppgaven tildeles til saksbehandleren.
     * 5) Saksnummer knyttes til oppgaven hvis oppgaven er en behandlingsoppgave.
     */
    @Transactional
    public synchronized Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) throws FunksjonellException, TekniskException {
        String type = plukkDto.getOppgavetype();
        Oppgavetype oppgavetype = KodeverkUtils.dekod(Oppgavetype.class, type);

        Tema fagområde = null;
        if (oppgavetype == Oppgavetype.JFR) {
            String fagområdeKode = plukkDto.getFagomrade();
            fagområde = KodeverkUtils.dekod(Tema.class, fagområdeKode);
        }

        List<Fagsakstype> fagsakstypeListe = new ArrayList<>();
        List<Behandlingstype> behandlingstypeListe = new ArrayList<>();
        if (oppgavetype == Oppgavetype.BEH_SAK) {

            List<String> sakstyper = plukkDto.getSakstyper();
            for (String s : sakstyper) {
                fagsakstypeListe.add(KodeverkUtils.dekod(Fagsakstype.class, s));
            }

            List<String> behandlingstyper = plukkDto.getBehandlingstyper();
            for (String b : behandlingstyper) {
                // FIXME: Internt kodeverk må oppdateres i frontend.
                if (b.equals("SKND")) {
                    behandlingstypeListe.add(Behandlingstype.SØKNAD);
                    continue;
                }
                behandlingstypeListe.add(KodeverkUtils.dekod(Behandlingstype.class, b));
            }
        }

        List<Oppgave> oppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(oppgavetype, fagområde, fagsakstypeListe, behandlingstypeListe);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, oppgaver);

        if (valg.isPresent()) {
            Oppgave oppgave = valg.get();
            // Tildeler oppgaven
            gsakFasade.tildelOppgave(oppgave.getOppgaveId(), saksbehandlerID);
            if (oppgave.erBehandling()) {
                // Finner fagsak og saksnummer som svarer til behandlingsoppgaven.
                Fagsak fagsak = fagsakRepository.findByGsakSaksnummer(oppgave.getGsakSaksnummer());
                if (fagsak == null) {
                    throw new IkkeFunnetException("Fant ikke fagsak med Gsak saksnummer " + oppgave.getGsakSaksnummer());
                }
                oppgave.setSaksnummer(fagsak.getSaksnummer());
            }
        }

        return valg;
    }

    @Transactional
    public synchronized void leggTilbakeOppgave(String oppgaveId, String saksbehandlerID, String begrunnelse) throws FunksjonellException, TekniskException {
        Oppgave oppgave = gsakFasade.hentOppgave(oppgaveId);

        if (oppgave == null) {
            log.error("Fant ikke oppgave med oppgaveId " + oppgaveId);
            throw new IkkeFunnetException("Fant ikke oppgave med oppgaveId " + oppgaveId);
        }
        gsakFasade.leggTilbakeOppgave(oppgaveId);

        OppgaveTilbakelegging oppgaveTilbakelegging = new OppgaveTilbakelegging();
        oppgaveTilbakelegging.setOppgaveId(oppgaveId);
        oppgaveTilbakelegging.setSaksbehandlerId(saksbehandlerID);
        oppgaveTilbakelegging.setBegrunnelse(begrunnelse);
        oppgaveTilbakelegging.setRegistrertDato(LocalDateTime.now());
        oppgaveTilbakkeleggingRepo.save(oppgaveTilbakelegging);
        log.info("Oppgave med oppgaveId {} er lagt tilbake. ", oppgaveId);
    }

    private Optional<Oppgave> velgNeste(String saksbehandlerID, List<Oppgave> oppgaver) {

        Optional<Oppgave> valg = oppgaver.stream().min(høyestTilLavestPrioritet);
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

    private static final Comparator<Oppgave> høyestTilLavestPrioritet = (a, b) -> {
        // Merk: Bryter med konvensjonen (a == b og b == c → a == c), men dette er ok.
        int res = 0;
        if (a.getPrioritet() == b.getPrioritet())
            res = 0;
        else if (a.getPrioritet() == PrioritetType.HOY)
            res = -1;
        else if (b.getPrioritet() == PrioritetType.HOY)
            res = 1;
        else if (a.getPrioritet() == PrioritetType.NORM)
            res = -1;
        else if (b.getPrioritet() == PrioritetType.NORM)
            res = 1;
        if (res == 0) {
            if (a.getFristFerdigstillelse() == null || b.getFristFerdigstillelse() == null)
                return 0;
            return a.getFristFerdigstillelse().compareTo(b.getFristFerdigstillelse());
        }
        return res;
    };

}
