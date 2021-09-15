package no.nav.melosys.service.oppgave;


import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.service.persondata.PersondataFasade;
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
    private final PersondataFasade persondataFasade;
    private static final String UKJENT = "UKJENT";

    @Autowired
    public OppgaveService(BehandlingService behandlingService,
                          FagsakService fagsakService,
                          OppgaveFasade oppgaveFasade,
                          SaksopplysningerService saksopplysningerService,
                          BehandlingsgrunnlagService behandlingsgrunnlagService,
                          PersondataFasade persondataFasade) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.oppgaveFasade = oppgaveFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.persondataFasade = persondataFasade;
    }

    public List<Oppgave> finnOppgaverMedBrukerID(String brukerIdent) {
        String aktørId = persondataFasade.hentAktørIdForIdent(brukerIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finner ikke aktørId for ident " + brukerIdent);
        }
        return oppgaveFasade.finnOppgaverMedBrukerID(aktørId);
    }

    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) {
        Collection<Oppgave> oppgaverFraDomain = oppgaveFasade.finnOppgaverMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    public void ferdigstillOppgave(String oppgaveID) {
        log.info("Ferdigstiller oppgave {}", oppgaveID);
        oppgaveFasade.ferdigstillOppgave(oppgaveID);
    }

    public void ferdigstillOppgaveMedSaksnummer(String fagSaksnummer) {
        finnÅpenOppgaveMedFagsaksnummer(fagSaksnummer).ifPresentOrElse(
            oppgave -> ferdigstillOppgave(oppgave.getOppgaveId()),
            () -> log.warn("Sak {} har ingen oppgaver å ferdigstille.", fagSaksnummer)
        );
    }

    public void leggTilbakeOppgaveMedSaksnummer(String fagSaksnummer) {
        Oppgave oppgave = hentÅpenOppgaveMedFagsaksnummer(fagSaksnummer);
        oppgaveFasade.leggTilbakeOppgave(oppgave.getOppgaveId());
    }

    public Optional<Oppgave> finnSisteAvsluttetOppgaveMedFagsaksnummer(String saksnummer) {
        List<Oppgave> oppgaver = oppgaveFasade.finnAvsluttetOppgaverMedSaksnummer(saksnummer);

        if (oppgaver.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(oppgaver.get(0));
    }

    public Optional<Oppgave> finnÅpenOppgaveMedFagsaksnummer(String saksnummer) {
        List<Oppgave> oppgaver = oppgaveFasade.finnÅpneOppgaverMedSaksnummer(saksnummer);

        if (!oppgaver.isEmpty()) {
            if (oppgaver.size() > 1) {
                throw new TekniskException("Det finnes flere aktive behandlingsoppgaver for sak " + saksnummer);
            }
            return Optional.of(oppgaver.get(0));
        } else {
            return Optional.empty();
        }
    }

    public Oppgave hentSisteAvsluttetOppgaveMedFagsaksnummer(String saksnummer) {
        return finnSisteAvsluttetOppgaveMedFagsaksnummer(saksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen oppgave med saksnummer " + saksnummer));
    }

    public Oppgave hentÅpenOppgaveMedFagsaksnummer(String saksnummer) {
        return finnÅpenOppgaveMedFagsaksnummer(saksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen åpen oppgave med saksnummer " + saksnummer));
    }

    public Oppgave hentOppgaveMedOppgaveID(String oppgaveID) {
        return oppgaveFasade.hentOppgave(oppgaveID);
    }

    public Behandling hentSistAktiveBehandling(String saksnummer) {
        return fagsakService.hentFagsak(saksnummer).hentSistAktiveBehandling();
    }

    public void opprettEllerGjenbrukBehandlingsoppgave(Behandling behandling, String journalpostID, String aktørID, @Nullable String tilordnetRessurs) {

        Optional<Oppgave> eksisterendeOppgave = finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        if (eksisterendeOppgave.isEmpty()) {
            String beskrivelse = behandling.erElektroniskSøknad() ? "Mottatt elektronisk søknad" : null;
            Oppgave oppgave = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType())
                .setTilordnetRessurs(tilordnetRessurs)
                .setJournalpostId(journalpostID)
                .setBeskrivelse(beskrivelse)
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

    public void opprettJournalføringsoppgave(String journalpostID, String aktørID) {
        final String oppgaveID = opprettOppgave(OppgaveFactory.lagJournalføringsoppgave(journalpostID).setAktørId(aktørID).build());
        log.info("Journalføringsoppgave {} opprettet for journalpost {}", oppgaveID, journalpostID);
    }

    public String opprettOppgave(Oppgave oppgave) {
        return oppgaveFasade.opprettOppgave(oppgave);
    }

    public void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering) {
        oppgaveFasade.oppdaterOppgave(oppgaveID, oppgaveOppdatering);
    }

    public void oppdaterOppgaveMedSaksnummer(String fagSaksnummer, OppgaveOppdatering oppgaveOppdatering) {
        finnÅpenOppgaveMedFagsaksnummer(fagSaksnummer).ifPresentOrElse(
            oppg -> oppdaterOppgave(oppg.getOppgaveId(), oppgaveOppdatering),
            () -> log.warn("Sak {} har ingen åpne oppgaver å oppdatere.", fagSaksnummer)
        );
    }

    public void tildelOppgave(String oppgaveID, String saksbehandler) {
        oppgaveFasade.oppdaterOppgave(oppgaveID, OppgaveOppdatering.builder().tilordnetRessurs(saksbehandler).build());
    }

    public String gjenopprettSisteAvsluttetOppgaveMedFagsaksnummer(String saksnummer) {
        Oppgave oppgave = hentSisteAvsluttetOppgaveMedFagsaksnummer(saksnummer);

        Oppgave gjenopprettetOppgave = new Oppgave.Builder()
            .setAktørId(oppgave.getAktørId())
            .setBehandlingstema(oppgave.getBehandlingstema())
            .setBehandlingstype(oppgave.getBehandlingstype())
            .setBeskrivelse(oppgave.getBeskrivelse())
            .setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon())
            .setOpprettetTidspunkt(oppgave.getOpprettetTidspunkt())
            .setFristFerdigstillelse(oppgave.getFristFerdigstillelse())
            .setJournalpostId(oppgave.getJournalpostId())
            .setOppgavetype(oppgave.getOppgavetype())
            .setPrioritet(oppgave.getPrioritet())
            .setSaksnummer(oppgave.getSaksnummer())
            .setTema(oppgave.getTema())
            .setTemagruppe(oppgave.getTemagruppe())
            .setTilordnetRessurs(oppgave.getTilordnetRessurs())
            .setTildeltEnhetsnr(oppgave.getTildeltEnhetsnr())
            .setVersjon(oppgave.getVersjon())
            .setAktivDato(oppgave.getAktivDato())
            .setStatus("UNDER_BEHANDLING")
            .build();

        String gammelOppgaveId = oppgave.getOppgaveId();
        log.info("Gjenoppretter oppgave med id {} knyttet til saksnummer {}", gammelOppgaveId, saksnummer);

        String nyOppgaveId = opprettOppgave(gjenopprettetOppgave);

        log.info("Gjenopprettet oppgave med id {} knyttet til saksnummer {} som ny oppgave med id {}", gammelOppgaveId, saksnummer, nyOppgaveId);
        return oppgave.getOppgaveId();
    }

    private List<OppgaveDto> oppgaverTilDtoer(Collection<Oppgave> oppgaverFraDomain) {
        return oppgaverFraDomain.stream()
            .map(this::tilOppgaveDtoHåndterException)
            .filter(Objects::nonNull)
            .toList();
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

    private OppgaveDto tilOppgaveDto(Oppgave oppgave) {
        OppgaveDto dest;

        if (oppgave.erJournalFøring()) {
            JournalfoeringsoppgaveDto jfrOppgaveDto = new JournalfoeringsoppgaveDto();
            jfrOppgaveDto.setJournalpostID(oppgave.getJournalpostId());
            dest = jfrOppgaveDto;
            String aktørId = oppgave.getAktørId();
            String fnr = aktørId != null ? persondataFasade.hentFolkeregisterident(aktørId) : null;
            if (StringUtils.isNotEmpty(fnr)) {
                dest.setFnr(fnr);
                dest.setSammensattNavn(persondataFasade.hentSammensattNavn(fnr));
            } else {
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
                Soeknadsland søknadsland = hentSøknadsland(søknadDokument);
                behOppgaveDto.setLand(SoeknadslandDto.av(søknadsland));
                behOppgaveDto.setPeriode(mapPeriode(søknadDokument));
            } else {
                saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresent(
                    sedDokument -> {
                        Landkoder lovvalgslandKode = sedDokument.getLovvalgslandKode();
                        behOppgaveDto.setLand(SoeknadslandDto.av(lovvalgslandKode));
                        behOppgaveDto.setPeriode(new PeriodeDto(
                            sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
                        );
                    });
            }
            saksopplysningerService.finnPersonOpplysninger(behandling.getId()).ifPresent(
                personDokument -> {
                    behOppgaveDto.setSammensattNavn(personDokument.getSammensattNavn());
                    behOppgaveDto.setFnr(personDokument.hentFolkeregisterident());
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

    private boolean harBeskyttelsesbehov(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        // TODO TPS krever fnr. Kall til hentFolkeregisterident fjernes etter overgang til PDL.
        final String brukersFnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentAktørID());
        if (persondataFasade.harStrengtFortroligAdresse(brukersFnr)) {
            return true;
        } else if (behandling.getBehandlingsgrunnlag() == null) {
            return false;
        }
        for (String fnr : behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().hentFnrMedfølgendeBarn()) {
            if (persondataFasade.harStrengtFortroligAdresse(fnr)) {
                return true;
            }
        }
        return false;
    }
}
