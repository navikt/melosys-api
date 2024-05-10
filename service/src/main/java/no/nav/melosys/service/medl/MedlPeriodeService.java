package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import jakarta.transaction.Transactional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedlPeriodeService {
    private static final Logger log = LoggerFactory.getLogger(MedlPeriodeService.class);

    private final PersondataFasade persondataFasade;
    private final MedlService medlService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingService behandlingService;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private final MedlAnmodningsperiodeService medlAnmodningsperiodeService;
    private final UtpekingsperiodeRepository utpekingsperiodeRepository;
    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    private final FagsakService fagsakService;

    private static final String FEIL_VED_OPPDATERING_MEDL = "Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling ";

    public MedlPeriodeService(PersondataFasade persondataFasade,
                              MedlService medlService,
                              BehandlingsresultatService behandlingsresultatService,
                              BehandlingService behandlingService,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository,
                              MedlAnmodningsperiodeService medlAnmodningsperiodeService,
                              UtpekingsperiodeRepository utpekingsperiodeRepository,
                              MedlemskapsperiodeRepository medlemskapsperiodeRepository,
                              FagsakService fagsakService) {
        this.persondataFasade = persondataFasade;
        this.medlService = medlService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.medlAnmodningsperiodeService = medlAnmodningsperiodeService;
        this.utpekingsperiodeRepository = utpekingsperiodeRepository;
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.fagsakService = fagsakService;
    }

    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fnr, "fnr kan ikke være null");
        return medlService.hentPeriodeListe(fnr, fom, tom);
    }

    public void opprettPeriodeForeløpig(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID) {
        log.info("Oppretter foreløpig periode i MEDL for behandling {}", behandlingID);
        String fnr = hentFnr(behandlingID);
        Long medlPeriodeID =
            medlService.opprettPeriodeForeløpig(fnr, periodeOmLovvalg, hentKildedokumenttype(behandlingID));
        lagreMedlPeriodeId(medlPeriodeID, periodeOmLovvalg, behandlingID);
    }

    public void opprettPeriodeUnderAvklaring(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID) {
        log.info("Oppretter periode under avklaring i MEDL for behandling {}", behandlingID);
        String fnr = hentFnr(behandlingID);
        Long medlPeriodeID =
            medlService.opprettPeriodeUnderAvklaring(fnr, periodeOmLovvalg, hentKildedokumenttype(behandlingID));
        lagreMedlPeriodeId(medlPeriodeID, periodeOmLovvalg, behandlingID);
    }

    public void oppdaterPeriodeUnderAvklaring(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID) {
        medlService.oppdaterPeriodeUnderAvklaring(periodeOmLovvalg, hentKildedokumenttype(behandlingID));
    }

    public void opprettPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, Long behandlingID) {
        String fnr = hentFnr(behandlingID);
        log.info("Oppretter endelig lovvalgsperiode i MEDL for behandling {}", behandlingID);
        Long medlPeriodeID = medlService.opprettPeriodeEndelig(fnr, lovvalgsperiode, hentKildedokumenttype(behandlingID));
        lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandlingID);
    }

    public void opprettPeriodeEndelig(long behandlingId, Medlemskapsperiode medlemskapsperiode) {
        String fnr = hentFnr(behandlingId);
        log.info("Oppretter endelig medlemskapsperiode i MEDL for behandling {}", behandlingId);
        Long medlPeriodeId = medlService.opprettPeriodeEndelig(fnr, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        if (medlPeriodeId == null) {
            throw new TekniskException(FEIL_VED_OPPDATERING_MEDL + behandlingId);
        }

        lagreMedlPeriodeId(medlPeriodeId, medlemskapsperiode);
    }

    public void opprettOpphørtPeriode(long behandlingId, Medlemskapsperiode medlemskapsperiode) {
        String fnr = hentFnr(behandlingId);
        log.info("Oppretter opphørt medlemskapsperiode i MEDL for behandling {}", behandlingId);
        Long medlPeriodeId = medlService.opprettOpphørtPeriode(fnr, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        if (medlPeriodeId == null) {
            throw new TekniskException(FEIL_VED_OPPDATERING_MEDL + behandlingId);
        }

        lagreMedlPeriodeId(medlPeriodeId, medlemskapsperiode);
    }

    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode) {
        log.info("Oppdaterer MEDL-periode {} for lovvalgsperiode", lovvalgsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeEndelig(lovvalgsperiode,
            hentKildedokumenttype(lovvalgsperiode.getBehandlingsresultat().getId()));
    }

    public void oppdaterPeriodeEndelig(long behandlingID, Medlemskapsperiode medlemskapsperiode) {
        log.info("Oppdaterer MEDL-periode {} for medlemskapsperiode", medlemskapsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeEndelig(medlemskapsperiode, hentKildedokumenttype(behandlingID));
    }

    public void oppdaterOpphørtPeriode(long behandlingID, Medlemskapsperiode medlemskapsperiode) {
        log.info("Oppdaterer opphørt MEDL-periode {} for medlemskapsperiode", medlemskapsperiode.getMedlPeriodeID());
        medlService.oppdaterOpphørtPeriode(medlemskapsperiode, hentKildedokumenttype(behandlingID));
    }

    public void oppdaterPeriodeForeløpig(Lovvalgsperiode lovvalgsperiode) {
        log.info("Oppdaterer MEDL-periode {} til status foreløpig", lovvalgsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeForeløpig(lovvalgsperiode,
            hentKildedokumenttype(lovvalgsperiode.getBehandlingsresultat().getId()));
    }

    public void avvisPeriode(Long medlPeriodeID) {
        log.info("Avviser MEDL-periode {} med status avvist", medlPeriodeID);
        avvisPeriode(medlPeriodeID, StatusaarsakMedl.AVVIST);
    }

    public void avvisPeriodeFeilregistrert(long medlPeriodeID) {
        log.info("Avviser MEDL-periode {} med status feilregistrert", medlPeriodeID);
        avvisPeriode(medlPeriodeID, StatusaarsakMedl.FEILREGISTRERT);
    }

    public void avvisPeriodeOpphørt(long medlPeriodeID) {
        log.info("Avviser MEDL-periode {} med status opphørt", medlPeriodeID);
        avvisPeriode(medlPeriodeID, StatusaarsakMedl.OPPHORT);
    }

    @Transactional
    public void avsluttTidligerMedlPeriode(String saksnummer) {
        var fagsak = fagsakService.hentFagsak(saksnummer);

        Behandling behandling = fagsak.finnTidligstInaktivBehandling();
        if (behandling != null) {
            Optional<Lovvalgsperiode> lovvalgsperiode = finnLovvalgsperiode(behandling);
            if (lovvalgsperiode.isPresent() && lovvalgsperiode.get().getMedlPeriodeID() != null) {
                log.info("Avslutter tidligere periode for fagsak {}", fagsak.getSaksnummer());
                avvisPeriode(lovvalgsperiode.get().getMedlPeriodeID());
            }
        }
    }

    private void avvisPeriode(long medlPeriodeId, StatusaarsakMedl statusaarsakMedl) {
        medlService.avvisPeriode(medlPeriodeId, statusaarsakMedl);
    }

    private String hentFnr(Long behandlingID) {
        Behandling behandling = behandlingsresultatService.hentBehandlingsresultat(behandlingID).getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentBruker();
        return persondataFasade.hentFolkeregisterident(bruker.getAktørId());
    }

    private Optional<Lovvalgsperiode> finnLovvalgsperiode(Behandling behandling) {
        return hentBehandlingsresultat(behandling).getLovvalgsperioder().stream().findFirst();
    }

    private Behandlingsresultat hentBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, PeriodeOmLovvalg periodeOmLovvalg, long behandlingID) {
        if (medlPeriodeID == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingID);
        }

        if (periodeOmLovvalg instanceof Lovvalgsperiode lovvalgsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode);
        } else if (periodeOmLovvalg instanceof Anmodningsperiode anmodningsperiode) {
            anmodningsperiode.setMedlPeriodeID(medlPeriodeID);
            medlAnmodningsperiodeService.lagreAnmodningsperiode(anmodningsperiode);
        } else if (periodeOmLovvalg instanceof Utpekingsperiode utpekingsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, utpekingsperiode);
        } else {
            throw new UnsupportedOperationException("Uventet periode med bestemmelse kan ikke lagres");
        }
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode) {
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }


    private void lagreMedlPeriodeId(Long medlPeriodeID, Utpekingsperiode utpekingsperiode) {
        utpekingsperiode.setMedlPeriodeID(medlPeriodeID);
        utpekingsperiodeRepository.save(utpekingsperiode);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeId, Medlemskapsperiode medlemskapsperiode) {
        medlemskapsperiode.setMedlPeriodeID(medlPeriodeId);
        medlemskapsperiodeRepository.save(medlemskapsperiode);
    }

    private KildedokumenttypeMedl hentKildedokumenttype(Long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        boolean erSed = behandling.erBehandlingAvSed();
        var fagsaktype = behandling.getFagsak().getType();
        var behandlingstema = behandling.getTema();

        if (fagsaktype.equals(Sakstyper.TRYGDEAVTALE)) {
            if (behandlingstema == Behandlingstema.REGISTRERING_UNNTAK) {
                return KildedokumenttypeMedl.DOKUMENT;
            } else if (behandlingstema == Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL) {
                return KildedokumenttypeMedl.HENV_SOKNAD;
            }
        } else if (fagsaktype.equals(Sakstyper.EU_EOS) &&
            behandlingstema == Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR) {
            return KildedokumenttypeMedl.A1;
        }

        return erSed ? KildedokumenttypeMedl.SED : KildedokumenttypeMedl.HENV_SOKNAD;
    }
}
