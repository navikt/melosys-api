package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.util.KodeverkUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Oppgaveplukker {
    private static final Logger log =  LoggerFactory.getLogger(Oppgaveplukker.class);

    static final List<Oppgavetyper> KJENTE_OPPGAVETYPER = Arrays.asList(Oppgavetyper.BEH_SAK_MK, Oppgavetyper.VUR, Oppgavetyper.BEH_SED);

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
     * 1) Input valideres og behandlingstema og oppgavetyper beregnes.
     * 2) Oppgaveplukker henter i Oppgave en liste over alle aktive, ikke tildelte oppgaver med oppgitt parametre.
     * 3) Neste oppgave velges basert på prioritet (først) og frist.
     * 4) Oppgaven tildeles til saksbehandleren.
     */
    @Transactional(rollbackFor = MelosysException.class)
    public synchronized Optional<Oppgave> plukkOppgave(String saksbehandlerID, PlukkOppgaveInnDto plukkDto) throws FunksjonellException, TekniskException {
        String type = plukkDto.getOppgavetype();
        Oppgavetyper oppgavetype = type == null ? null : KodeverkUtils.dekod(Oppgavetyper.class, type);

        Set<Sakstyper> sakstyper = new HashSet<>();
        Set<Behandlingstema> behandlingstemaer = new HashSet<>();
        Set<Behandlingstyper> behandlingstyper = new HashSet<>();
        Set<Oppgavetyper> oppgavetyper = new HashSet<>();
        if (oppgavetype == Oppgavetyper.JFR) {
            oppgavetyper = Collections.singleton(Oppgavetyper.JFR);
        } else {
            List<String> sakstypeKoder = plukkDto.getSakstyper();
            for (String s : sakstypeKoder) {
                sakstyper.add(KodeverkUtils.dekod(Sakstyper.class, s));
            }
            behandlingstemaer.addAll(hentBehandlingstema(sakstyper));

            List<String> behandlingstypeKoder = plukkDto.getBehandlingstyper();
            if (CollectionUtils.isNotEmpty(behandlingstypeKoder)) {
                for (String behandlingstype : behandlingstypeKoder) {
                    behandlingstyper.add(KodeverkUtils.dekod(Behandlingstyper.class, behandlingstype));
                }
                oppgavetyper.addAll(hentOppgavetyper(behandlingstyper));
            } else {
                // Når behandlingstype ikke velges skal alle behandlingsoppgaver klare til behandling plukkes uansett behandlingstype.
                oppgavetyper.addAll(KJENTE_OPPGAVETYPER);
            }
        }

        List<Oppgave> ufordelteOppgaver = gsakFasade.finnUtildelteOppgaverEtterFrist(oppgavetyper, sakstyper, Collections.emptySet(), behandlingstemaer);
        fjernOppgaverSomVenterForDokumentasjon(ufordelteOppgaver);

        Optional<Oppgave> valg = velgNeste(saksbehandlerID, ufordelteOppgaver);

        if (valg.isPresent()) {
            // Tildeler oppgaven
            gsakFasade.tildelOppgave(valg.get().getOppgaveId(), saksbehandlerID);
        }
        return valg;
    }

    Set<Behandlingstema> hentBehandlingstema(Set<Sakstyper> fagsakstypeListe) {
        return fagsakstypeListe.stream()
            .map(sakstyper -> Behandlingstema.valueOf(sakstyper.name()))
            .collect(Collectors.toSet());
    }

    Set<Oppgavetyper> hentOppgavetyper(Set<Behandlingstyper> behandlingstypeListe) {
        return behandlingstypeListe.stream()
            .map(Oppgave::hentOppgavetype)
            .collect(Collectors.toSet());
    }

    private void fjernOppgaverSomVenterForDokumentasjon(List<Oppgave> oppgaver) throws TekniskException {
        Iterator<Oppgave> iter = oppgaver.iterator();
        while (iter.hasNext()) {
            Oppgave oppgave = iter.next();
            String saksnummer = oppgave.getSaksnummer();
            Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
            if (fagsak == null) {
                log.error("Fant ikke fagsak {} for oppgave {}", saksnummer, oppgave.getOppgaveId());
                throw new TekniskException("Fant ikke fagsak " + saksnummer);
            }
            Behandling behandling = fagsak.getAktivBehandling();
            if (behandling == null) {
                throw new TekniskException("Fant ingen aktiv behandling på fagsak " + saksnummer);
            }

            if (behandling.erVenterForDokumentasjon() && behandling.getDokumentasjonSvarfristDato() == null) {
                throw new TekniskException("Behandling " + behandling.getId() + " tilhørende " + saksnummer + " avventer dokumentasjon, men har ingen svarfristdato");
            }
            if (behandling.erVenterForDokumentasjon() && behandling.getDokumentasjonSvarfristDato().isAfter(Instant.now())) {
                iter.remove();
            }
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public synchronized void leggTilbakeOppgave(String saksbehandlerID, TilbakeleggingDto tilbakelegging) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(tilbakelegging.getBehandlingID())
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke behandling med behandlingID " + tilbakelegging.getBehandlingID()));

        Fagsak fagsak = behandling.getFagsak();
        Oppgave oppgave = gsakFasade.finnOppgaveMedSaksnummer(fagsak.getSaksnummer());

        String oppgaveId = oppgave.getOppgaveId();
        if (!tilbakelegging.isVenterPåDokumentasjon()) {
            OppgaveTilbakelegging oppgaveTilbakelegging = new OppgaveTilbakelegging();
            oppgaveTilbakelegging.setOppgaveId(oppgaveId);
            oppgaveTilbakelegging.setSaksbehandlerId(saksbehandlerID);
            oppgaveTilbakelegging.setBegrunnelse(tilbakelegging.getBegrunnelse());
            oppgaveTilbakelegging.setRegistrertDato(LocalDateTime.now());
            oppgaveTilbakkeleggingRepo.save(oppgaveTilbakelegging);
        }

        gsakFasade.leggTilbakeOppgave(oppgaveId);
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
}
