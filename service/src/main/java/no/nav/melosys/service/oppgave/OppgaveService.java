package no.nav.melosys.service.oppgave;


import java.util.*;
import javax.annotation.Nullable;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentPeriode;
import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentSøknadsland;

@Service
public class OppgaveService {
    private static final Logger log = LoggerFactory.getLogger(OppgaveService.class);

    private final BehandlingService behandlingService;
    private final FagsakService fagsakService;
    private final OppgaveFasade oppgaveFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final Unleash unleash;

    private static final String UKJENT = "UKJENT";

    public OppgaveService(BehandlingService behandlingService,
                          FagsakService fagsakService,
                          OppgaveFasade oppgaveFasade,
                          SaksopplysningerService saksopplysningerService,
                          BehandlingsgrunnlagService behandlingsgrunnlagService,
                          PersondataFasade persondataFasade,
                          EregFasade eregFasade,
                          Unleash unleash) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.oppgaveFasade = oppgaveFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.unleash = unleash;
    }

    public List<Oppgave> finnOppgaverMedPersonIdent(String personIdent) {
        String aktørId = persondataFasade.hentAktørIdForIdent(personIdent);
        if (aktørId == null) {
            throw new IkkeFunnetException("Finner ikke aktørId for ident " + personIdent);
        }
        return oppgaveFasade.finnOppgaverMedAktørId(aktørId);
    }

    public List<Oppgave> finnOppgaverMedOrgnr(String orgnr) {
        return oppgaveFasade.finnOppgaverMedOrgnr(orgnr);
    }

    public List<OppgaveDto> hentOppgaverMedAnsvarlig(String ansvarligID) {
        Collection<Oppgave> oppgaverFraDomain = oppgaveFasade.finnOppgaverMedAnsvarlig(ansvarligID);
        return oppgaverTilDtoer(oppgaverFraDomain);
    }

    public List<Oppgave> finnÅpneOppgaverMedJournalpostID(String journalpostID) {
        return oppgaveFasade.finnÅpneOppgaverMedJournalpostID(journalpostID);
    }

    public void feilregistrerOppgaver(Set<Oppgave> oppgaveSet) {
        if (oppgaveSet.isEmpty()) {
            log.debug("Ingen oppgaver skal feilregistreres.");
            return;
        }
        log.info("Feilregistrer oppgave(r) {}", oppgaveSet.stream().map(Oppgave::getOppgaveId));
        oppgaveFasade.feilregistrerOppgaver(oppgaveSet);
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
        return oppgaveFasade.finnAvsluttetOppgaverMedSaksnummer(saksnummer)
            .stream()
            .max(Comparator.comparing(Oppgave::getOpprettetTidspunkt));
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

    public Oppgave hentÅpenOppgaveMedFagsaksnummer(String saksnummer) {
        return finnÅpenOppgaveMedFagsaksnummer(saksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen åpen oppgave med saksnummer " + saksnummer));
    }

    public Oppgave hentOppgaveMedOppgaveID(String oppgaveID) {
        return oppgaveFasade.hentOppgave(oppgaveID);
    }

    public Behandling hentSistAktiveBehandling(String saksnummer) {
        return fagsakService.hentFagsak(saksnummer).hentSistAktivBehandling();
    }

    public void opprettEllerGjenbrukBehandlingsoppgave(Behandling behandling, String journalpostID, @Nullable String aktørID, @Nullable String tilordnetRessurs, @Nullable @Deprecated String beskrivelse, @Nullable String orgnr) {

        Optional<Oppgave> eksisterendeOppgave = finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        if (eksisterendeOppgave.isEmpty()) {
            var oppgaveToggle = unleash.isEnabled("melosys.oppgave.oppretting");
            var oppgaveBuilder = (
                oppgaveToggle
                    ? OppgaveFactory.lagBehandlingsoppgave(behandling)
                    : OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType()))
                .setTilordnetRessurs(tilordnetRessurs)
                .setJournalpostId(journalpostID)
                .setAktørId(aktørID)
                .setOrgnr(orgnr)
                .setSaksnummer(behandling.getFagsak().getSaksnummer());

            if (!oppgaveToggle) {
                oppgaveBuilder.setBeskrivelse(beskrivelse);
            }

            String oppgaveID = StringUtils.isNotEmpty(aktørID) && harBeskyttelsesbehov(behandling.getId())
                ? oppgaveFasade.opprettSensitivOppgave(oppgaveBuilder.build())
                : oppgaveFasade.opprettOppgave(oppgaveBuilder.build());
            log.info("Opprettet oppgave {} for behandling {}", oppgaveID, behandling.getId());
        } else if (tilordnetRessurs != null && !tilordnetRessurs.equals(eksisterendeOppgave.get().getTilordnetRessurs())) {
            log.info("Oppgave eksisterer, oppdaterer tilordnetRessurs for oppgave tilknyttet behandling {}", behandling.getId());
            tildelOppgave(eksisterendeOppgave.get().getOppgaveId(), tilordnetRessurs);
        }
    }

    public void opprettEllerGjenbrukBehandlingsoppgave(Behandling behandling, String journalpostID, String aktørID, @Nullable String tilordnetRessurs) {
        opprettEllerGjenbrukBehandlingsoppgave(behandling, journalpostID, aktørID, tilordnetRessurs, lagOppgaveBeskrivelse(behandling), null);
    }

    /**
     * @deprecated Forsvinner med toggle melosys.oppgave.oppretting
     */
    @Deprecated
    private String lagOppgaveBeskrivelse(Behandling behandling) {
        if (behandling.erElektroniskSøknad()) {
            return "Mottatt elektronisk søknad";
        }
        if (behandling.erNyVurdering()) {
            return "Ny vurdering";
        }
        return null;
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

    public void opprettOppgaveForSak(String saksnummer) {
        log.info("Oppretter ny oppgave for saksnummer {}", saksnummer);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentSistAktivBehandling();
        Optional<Oppgave> oppgave = finnSisteAvsluttetOppgaveMedFagsaksnummer(saksnummer);
        String tilordnetRessurs = oppgave.map(Oppgave::getTilordnetRessurs).orElse(null);
        String beskrivelse = oppgave.map(Oppgave::getBeskrivelse).orElse(null);

        opprettEllerGjenbrukBehandlingsoppgave(behandling, behandling.getInitierendeJournalpostId(), fagsak.hentBrukersAktørID(), tilordnetRessurs, beskrivelse, null);
    }

    public boolean saksbehandlerErTilordnetOppgaveForSaksnummer(String saksbehandler, String saksnummer) {
        return finnÅpenOppgaveMedFagsaksnummer(saksnummer)
            .map(Oppgave::getTilordnetRessurs)
            .filter(saksbehandler::equals)
            .isPresent();
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
        OppgaveDto resultat;

        if (oppgave.erJournalFøring()) {
            resultat = lagJournalføringsoppgaveDto(oppgave);
        } else if (oppgave.erBehandling() || oppgave.erVurderDokument() || oppgave.erSedBehandling()) {
            resultat = lagBehandlingsoppgaveDto(oppgave);
        } else {
            throw new TekniskException("Oppgavetype " + oppgave.getOppgavetype() + " støttes ikke");
        }

        resultat.setAktivTil(oppgave.getFristFerdigstillelse());
        resultat.setAnsvarligID(oppgave.getTilordnetRessurs());
        resultat.setOppgaveID(oppgave.getOppgaveId());
        resultat.setPrioritet(oppgave.getPrioritet());
        resultat.setVersjon(oppgave.getVersjon());

        return resultat;
    }

    private OppgaveDto lagJournalføringsoppgaveDto(Oppgave oppgave) {
        JournalfoeringsoppgaveDto journalfoeringsoppgaveDto = new JournalfoeringsoppgaveDto();
        journalfoeringsoppgaveDto.setJournalpostID(oppgave.getJournalpostId());
        String aktørId = oppgave.getAktørId();
        String orgnr = oppgave.getOrgnr();
        oppdaterHovedpartIdentOgNavn(aktørId, orgnr, journalfoeringsoppgaveDto);
        return journalfoeringsoppgaveDto;
    }

    private void oppdaterHovedpartIdentOgNavn(String aktørID, String orgnr, OppgaveDto oppgaveDto) {
        if (aktørID != null) {
            String fnr = persondataFasade.finnFolkeregisterident(aktørID).orElse(null);
            if (StringUtils.isNotEmpty(fnr)) {
                oppgaveDto.setHovedpartIdent(fnr);
                oppgaveDto.setNavn(persondataFasade.hentSammensattNavn(fnr));
                return;
            }
        }
        if (orgnr != null) {
            oppgaveDto.setHovedpartIdent(orgnr);
            oppgaveDto.setNavn(eregFasade.hentOrganisasjonNavn(orgnr));
            return;
        }
        oppgaveDto.setHovedpartIdent(UKJENT);
        oppgaveDto.setNavn(UKJENT);
    }

    private OppgaveDto lagBehandlingsoppgaveDto(Oppgave oppgave) {
        BehandlingsoppgaveDto behOppgaveDto = new BehandlingsoppgaveDto();
        Fagsak fagsak = fagsakService.hentFagsak(oppgave.getSaksnummer());
        behOppgaveDto.setSaksnummer(fagsak.getSaksnummer());
        behOppgaveDto.setSakstype(fagsak.getType());
        behOppgaveDto.setSakstema(fagsak.getTema());

        Behandling behandling = fagsak.hentSistAktivBehandling();
        behandling = behandlingService.hentBehandling(behandling.getId());
        behOppgaveDto.setBehandling(mapBehandling(behandling));

        var aktørID = fagsak.finnBrukersAktørID().orElse(null);
        var orgnr = fagsak.finnVirksomhetsOrgnr().orElse(null);
        oppdaterHovedpartIdentOgNavn(aktørID, orgnr, behOppgaveDto);

        if (orgnr != null) {
            return behOppgaveDto;
        }

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
        return behOppgaveDto;
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
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        final String brukersAktørID = behandling.getFagsak().hentBrukersAktørID();
        if (persondataFasade.harStrengtFortroligAdresse(brukersAktørID)) {
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
