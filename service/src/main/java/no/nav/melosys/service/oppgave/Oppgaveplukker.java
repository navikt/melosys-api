package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.util.KodeverkUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Oppgaveplukker {

    private static final Logger log =  LoggerFactory.getLogger(Oppgaveplukker.class);

    private final GsakFasade gsakFasade;

    private final OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public Oppgaveplukker(GsakFasade gsakFasade, OppgaveTilbakeleggingRepository oppgaveTilbakeleggingRepo,
                          FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository) {
        this.gsakFasade = gsakFasade;
        this.oppgaveTilbakkeleggingRepo = oppgaveTilbakeleggingRepo;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * 1) Input valideres
     * 2) Oppgaveplukker henter i GSAK en liste over alle aktive, ikke tildelte oppgaver med oppgitt parametre.
     * 3) Neste oppgave velges basert på prioritet (først) og frist.
     * 4) Oppgaven tildeles til saksbehandleren.
     * 5) Behandlingen endrer status hvis oppgaven er en behandlingsoppgave.
     */
    @Transactional
    public synchronized Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) throws FunksjonellException, TekniskException {
        String type = plukkDto.getOppgavetype();
        Oppgavetyper oppgavetype = KodeverkUtils.dekod(Oppgavetyper.class, type);

        Tema fagområde = null;
        if (oppgavetype == Oppgavetyper.JFR) {
            String fagområdeKode = plukkDto.getFagomrade();
            fagområde = KodeverkUtils.dekod(Tema.class, fagområdeKode);
        }

        List<Sakstyper> fagsakstypeListe = new ArrayList<>();
        List<Behandlingstyper> behandlingstypeListe = new ArrayList<>();
        List<Behandlingstema> behandlingstemaListe = new ArrayList<>();
        if (oppgavetype == Oppgavetyper.BEH_SAK_MK) {

            List<String> sakstyper = plukkDto.getSakstyper();
            for (String s : sakstyper) {
                fagsakstypeListe.add(KodeverkUtils.dekod(Sakstyper.class, s));
            }

            List<String> behandlingstyper = plukkDto.getBehandlingstyper();
            if (behandlingstyper != null) {
                for (String behandlingstype : behandlingstyper) {
                    behandlingstypeListe.add(KodeverkUtils.dekod(Behandlingstyper.class, behandlingstype));
                }
            }
            behandlingstemaListe.addAll(hentBehandlingstema(fagsakstypeListe));
        }

        List<Oppgave> ufordelteOppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(oppgavetype, fagområde, fagsakstypeListe, behandlingstypeListe, behandlingstemaListe);
        fjernOppgaverSomVenterForDokumentasjon(ufordelteOppgaver);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, ufordelteOppgaver);
        
        if (valg.isPresent()) {
            Oppgave oppgave = valg.get();
            // Tildeler oppgaven
            gsakFasade.tildelOppgave(oppgave.getOppgaveId(), saksbehandlerID);

            if (oppgavetype == Oppgavetyper.BEH_SAK_MK) {
                settBehandlingsstatusUnderBehandling(oppgave.getSaksnummer());
            }
        }
        return valg;
    }

    List<Behandlingstema> hentBehandlingstema(List<Sakstyper> fagsakstypeListe) {
        return fagsakstypeListe.stream()
            .map(sakstyper -> Behandlingstema.valueOf(sakstyper.name()))
            .collect(Collectors.toList());
    }

    private void fjernOppgaverSomVenterForDokumentasjon(List<Oppgave> oppgaver) throws TekniskException {
        Iterator<Oppgave> iter = oppgaver.iterator();
        while (iter.hasNext()) {
            Oppgave oppgave = iter.next();
            Fagsak fagsak = fagsakRepository.findBySaksnummer(oppgave.getSaksnummer());
            if (fagsak == null) {
                log.error("Fant ikke fagsak {} for oppgave {}", oppgave.getSaksnummer(), oppgave.getOppgaveId());
                throw new TekniskException("Fant ikke fagsak " + oppgave.getSaksnummer());
            }
            Behandling behandling = fagsak.getAktivBehandling();

            if (erVenterForDokumentasjon(behandling.getStatus())
                && behandling.getDokumentasjonSvarfristDato() != null
                && behandling.getDokumentasjonSvarfristDato().isAfter(Instant.now())) {
                iter.remove();
            }
        }
    }

    @Transactional
    public synchronized void leggTilbakeOppgave(String saksbehandlerID, TilbakeleggingDto tilbakelegging) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(tilbakelegging.getBehandlingID())
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke behandling med behandlingID " + tilbakelegging.getBehandlingID()));

        Fagsak fagsak = behandling.getFagsak();
        Oppgave oppgave = gsakFasade.finnOppgaveMedSaksnummer(fagsak.getSaksnummer())
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen oppgave for fagsak " + fagsak.getSaksnummer()));

        String oppgaveId = oppgave.getOppgaveId();
        gsakFasade.leggTilbakeOppgave(oppgaveId);

        if (!tilbakelegging.isVenterPåDokumentasjon()) {
            OppgaveTilbakelegging oppgaveTilbakelegging = new OppgaveTilbakelegging();
            oppgaveTilbakelegging.setOppgaveId(oppgaveId);
            oppgaveTilbakelegging.setSaksbehandlerId(saksbehandlerID);
            oppgaveTilbakelegging.setBegrunnelse(tilbakelegging.getBegrunnelse());
            oppgaveTilbakelegging.setRegistrertDato(LocalDateTime.now());
            oppgaveTilbakkeleggingRepo.save(oppgaveTilbakelegging);
        }
        log.info("Oppgave med oppgaveId {} er lagt tilbake. ", oppgaveId);
    }

    private Optional<Oppgave> velgNeste(String saksbehandlerID, List<Oppgave> oppgaver) {

        Optional<Oppgave> valg = oppgaver.stream().min(Oppgave.høyestTilLavestPrioritet);
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

    private void settBehandlingsstatusUnderBehandling(String saksnummer) throws TekniskException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            throw new TekniskException("Fagsak med saksnummer " + saksnummer + " finnes ikke.");
        }
        Behandling behandling = fagsak.getAktivBehandling();
        if (behandling == null) {
            throw new TekniskException("En behandlingsoppgave eksisterer i GSAK for sak " + saksnummer + " men ingen aktive behandlinger finnes.");
        }
        if (behandling.getStatus() == Behandlingsstatus.OPPRETTET) {
            behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingRepository.save(behandling);
        }
    }

    public static boolean erVenterForDokumentasjon(Behandlingsstatus behandlingsstatus) {
        return (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_PART) || (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_UTL);
    }
}
