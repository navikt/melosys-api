package no.nav.melosys.service.oppgave;


import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentPeriode;
import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentSøknadsland;

@Service
@Primary
public class OppgaveService {
    private static final Logger log = LoggerFactory.getLogger(OppgaveService.class);

    private final BehandlingService behandlingService;
    private final FagsakService fagsakService;
    private final OppgaveFasade oppgaveFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final TpsFasade tpsFasade;
    private static final String UKJENT = "UKJENT";

    @Autowired
    public OppgaveService(BehandlingService behandlingService,
                          FagsakService fagsakService,
                          OppgaveFasade oppgaveFasade,
                          SaksopplysningerService saksopplysningerService,
                          BehandlingsgrunnlagService behandlingsgrunnlagService, TpsFasade tpsFasade) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.oppgaveFasade = oppgaveFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.tpsFasade = tpsFasade;
    }

    public List<Oppgave> finnOppgaverMedBrukerID(String brukerIdent) throws FunksjonellException, TekniskException {
        String aktørId = tpsFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finner ikke aktørId for ident " + brukerIdent);
        }
        return oppgaveFasade.finnOppgaverMedBrukerID(aktørId);
    }

    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) throws TekniskException, FunksjonellException {
        Collection<Oppgave> oppgaverFraDomain = oppgaveFasade.finnOppgaverMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    public void ferdigstillOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        log.info("Ferdigstiller oppgave {}", oppgaveID);
        oppgaveFasade.ferdigstillOppgave(oppgaveID);
    }

    public void ferdigstillOppgaveMedSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave;
        try {
            oppgave = hentOppgaveMedFagsaksnummer(fagSaksnummer);
        } catch (IkkeFunnetException e) {
            log.debug("Sak {} har ingen oppgaver å ferdigstille.", fagSaksnummer);
            return;
        }
        ferdigstillOppgave(oppgave.getOppgaveId());
    }

    public void leggTilbakeOppgaveMedSaksnummer(String fagSaksnummer) throws FunksjonellException, TekniskException {
        Oppgave oppgave = hentOppgaveMedFagsaksnummer(fagSaksnummer);
        oppgaveFasade.leggTilbakeOppgave(oppgave.getOppgaveId());
    }

    public Optional<Oppgave> finnOppgaveMedFagsaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        List<Oppgave> oppgaver = oppgaveFasade.finnOppgaverMedSaksnummer(saksnummer);

        if (!oppgaver.isEmpty()) {
            if (oppgaver.size() > 1) {
                throw new TekniskException("Det finnes flere aktive behandlingsoppgaver for sak " + saksnummer);
            }
            return Optional.of(oppgaver.get(0));
        } else {
            return Optional.empty();
        }
    }

    public Oppgave hentOppgaveMedFagsaksnummer(String saksnummer) throws FunksjonellException, TekniskException {
        return finnOppgaveMedFagsaksnummer(saksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen oppgave med saksnummer " + saksnummer));
    }

    public Oppgave hentOppgaveMedOppgaveID(String oppgaveID) throws FunksjonellException, TekniskException {
        return oppgaveFasade.hentOppgave(oppgaveID);
    }

    public Behandling hentSistAktiveBehandling(String saksnummer) throws FunksjonellException, TekniskException {
        return fagsakService.hentFagsak(saksnummer).hentSistAktiveBehandling();
    }

    public void opprettEllerGjenbrukBehandlingsoppgave(Behandling behandling, String journalpostID, String aktørID, @Nullable String tilordnetRessurs) throws FunksjonellException, TekniskException {

        Optional<Oppgave> eksisterendeOppgave = finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        if (eksisterendeOppgave.isEmpty()) {
            Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType())
                .setTilordnetRessurs(tilordnetRessurs)
                .setJournalpostId(journalpostID)
                .setAktørId(aktørID)
                .setSaksnummer(behandling.getFagsak().getSaksnummer())
                .build();

            String oppgaveID = harBeskyttelsesbehov(behandling.getId())
                ? oppgaveFasade.opprettSensitivOppgave(oppgave)
                : oppgaveFasade.opprettOppgave(oppgave);
            log.info("Opprettet oppgave {} for behandling {}", oppgaveID, behandling.getId());
        } else if (tilordnetRessurs != null && !tilordnetRessurs.equals(eksisterendeOppgave.get().getTilordnetRessurs())) {
            log.info("Oppgave eksisterer, oppdaterer tilordnetRessurs for oppgave tilknyttet behandling {}", behandling.getId());
            tildelOppgave(eksisterendeOppgave.get().getOppgaveId(), tilordnetRessurs);
        }
    }

    public void opprettJournalføringsoppgave(String journalpostID, String aktørID) throws FunksjonellException, TekniskException {
        final String oppgaveID = opprettOppgave(OppgaveFactory.lagJournalføringsoppgave(journalpostID).setAktørId(aktørID).build());
        log.info("Journalføringsoppgave {} opprettet for journalpost {}", oppgaveID, journalpostID);
    }

    public String opprettOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException {
        return oppgaveFasade.opprettOppgave(oppgave);
    }

    public void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering) throws FunksjonellException, TekniskException {
        oppgaveFasade.oppdaterOppgave(oppgaveID, oppgaveOppdatering);
    }

    public void tildelOppgave(String oppgaveID, String saksbehandler) throws FunksjonellException, TekniskException {
        oppgaveFasade.oppdaterOppgave(oppgaveID, OppgaveOppdatering.builder().tilordnetRessurs(saksbehandler).build());
    }

    private List<OppgaveDto> oppgaverTilDtoer(Collection<Oppgave> oppgaverFraDomain) {
        return oppgaverFraDomain.stream()
            .map(this::tilOppgaveDtoHåndterException)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Nullable
    private OppgaveDto tilOppgaveDtoHåndterException(Oppgave oppgave) {
        try {
            return tilOppgaveDto(oppgave);
        } catch (Exception e) {
            log.error("Kan ikke mappe oppgave {}", oppgave.getOppgaveId(), e);
            return null;
        }
    }

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) throws TekniskException, FunksjonellException {
        OppgaveDto dest;

        if (oppgave.erJournalFøring()) {
            JournalfoeringsoppgaveDto jfrOppgaveDto = new JournalfoeringsoppgaveDto();
            jfrOppgaveDto.setJournalpostID(oppgave.getJournalpostId());
            dest = jfrOppgaveDto;
            String aktørId = oppgave.getAktørId();
            String fnr = aktørId != null ? tpsFasade.hentIdentForAktørId(aktørId) : null;
            if (StringUtils.isNotEmpty(fnr)){
                dest.setFnr(fnr);
                dest.setSammensattNavn(tpsFasade.hentSammensattNavn(fnr));
            }
            else {
                dest.setFnr(UKJENT);
                dest.setSammensattNavn(UKJENT);
            }
        } else if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling()) {
            BehandlingsoppgaveDto behOppgaveDto = new BehandlingsoppgaveDto();
            Fagsak fagsak = fagsakService.hentFagsak(oppgave.getSaksnummer());
            behOppgaveDto.setSaksnummer(fagsak.getSaksnummer());
            behOppgaveDto.setSakstype(fagsak.getType());

            Behandling behandling = fagsak.hentSistAktiveBehandling();
            behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandling.getId());
            behOppgaveDto.setBehandling(mapBehandling(behandling));

            if (behandling.erBehandlingAvSøknad()) {
                Soeknad søknadDokument = (Soeknad) behandlingsgrunnlagService
                    .hentBehandlingsgrunnlag(behandling.getId()).getBehandlingsgrunnlagdata();
                behOppgaveDto.setLand(hentSøknadsland(søknadDokument));
                behOppgaveDto.setPeriode(mapPeriode(søknadDokument));
            } else {
                saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresent(
                    sedDokument -> {
                        behOppgaveDto.setLand(Collections.singletonList(sedDokument.getLovvalgslandKode() != null
                            ? sedDokument.getLovvalgslandKode().getKode() : null));
                        behOppgaveDto.setPeriode(new PeriodeDto(
                            sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
                        );
                    });
            }
            saksopplysningerService.finnPersonOpplysninger(behandling.getId()).ifPresent(
                personDokument -> {
                    behOppgaveDto.setSammensattNavn(personDokument.sammensattNavn);
                    behOppgaveDto.setFnr(personDokument.fnr);
                }
            );

            dest = behOppgaveDto;
        } else {
            throw new TekniskException("Oppgavetype " + oppgave.getOppgavetype() + " støttes ikke");
        }

        dest.setAktivTil(oppgave.getFristFerdigstillelse());
        dest.setAnsvarligID(oppgave.getTilordnetRessurs());
        dest.setOppgaveID(oppgave.getOppgaveId());
        dest.setPrioritet(oppgave.getPrioritet());
        dest.setVersjon(oppgave.getVersjon());

        return dest;
    }

    private BehandlingDto mapBehandling(Behandling behandling) {
        BehandlingDto behandlingDto = new BehandlingDto();
        behandlingDto.setBehandlingID(behandling.getId());
        behandlingDto.setBehandlingsstatus(behandling.getStatus());
        behandlingDto.setBehandlingstype(behandling.getType());
        behandlingDto.setBehandlingstema(behandling.getTema());
        behandlingDto.setRegistrertDato(behandling.getRegistrertDato());
        behandlingDto.setEndretDato(behandling.getEndretDato());
        behandlingDto.setSvarFrist(behandling.getDokumentasjonSvarfristDato());
        // FIXME: Feltet og endepunktet fjernes fra JSON-schema
        behandlingDto.setErUnderOppdatering(false);
        return behandlingDto;
    }

    private static PeriodeDto mapPeriode(Soeknad soeknad) {
        Periode periode = hentPeriode(soeknad);
        return new PeriodeDto(periode.getFom(), periode.getTom());
    }

    private boolean harBeskyttelsesbehov(long behandlingID) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        if (behandling.hentPersonDokument().harBeskyttelsesbehov()) {
            return true;
        } else if (behandling.getBehandlingsgrunnlag() == null) {
            return false;
        }
        for (String fnr : behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().hentFnrMedfølgendeBarn()) {
            if (tpsFasade.harStrengtFortroligAdresse(fnr)) {
                return true;
            }
        }
        return false;
    }
}
