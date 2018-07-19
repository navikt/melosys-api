package no.nav.melosys.service.oppgave;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
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
    public Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) throws FunksjonellException, IkkeFunnetException {
        Oppgavetype oppgavetype;
        String type = plukkDto.getOppgavetype();
        if (KodeverkUtils.erGyldigKode(Oppgavetype.class, type)) {
            oppgavetype = KodeverkUtils.hentKodeverk(Oppgavetype.class, type);
        } else {
            throw new FunksjonellException("Oppgavetype " + type + " støttes ikke.");
        }

        Tema fagområde = null;
        if (oppgavetype == Oppgavetype.JFR) {
            String fagområdeKode = plukkDto.getFagomrade();
            if (KodeverkUtils.erGyldigKode(Tema.class, fagområdeKode)) {
                fagområde = KodeverkUtils.hentKodeverk(Tema.class, fagområdeKode);
            } else {
                throw new FunksjonellException("Fagområde " + fagområdeKode + " støttes ikke.");
            }
        }

        List<FagsakType> fagsakTypeListe = new ArrayList<>();
        List<BehandlingType> behandlingTypeListe = new ArrayList<>();
        if (oppgavetype == Oppgavetype.BEH_SAK) {

            List<String> sakstyper = plukkDto.getSakstyper();
            for (String s : sakstyper) {
                if (KodeverkUtils.erGyldigKode(FagsakType.class, s)) {
                    fagsakTypeListe.add(KodeverkUtils.hentKodeverk(FagsakType.class, s));
                } else {
                    throw new FunksjonellException("FagsakType " + s + " støttes ikke.");
                }
            }

            List<String> behandlingstyper = plukkDto.getBehandlingstyper();
            for (String b : behandlingstyper) {
                if (KodeverkUtils.erGyldigKode(BehandlingType.class, b)) {
                    behandlingTypeListe.add(KodeverkUtils.hentKodeverk(BehandlingType.class, b));
                } else {
                    throw new FunksjonellException("BehandlingType " + b + " støttes ikke.");
                }
            }
        }

        List<Oppgave> oppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(oppgavetype, fagområde, fagsakTypeListe, behandlingTypeListe);

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
