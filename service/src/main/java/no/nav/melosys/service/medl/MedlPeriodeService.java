package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    private final Unleash unleash;

    private static final String FEIL_VED_OPPDATERING_MEDL = "Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling ";

    public MedlPeriodeService(PersondataFasade persondataFasade,
                              MedlService medlService,
                              BehandlingsresultatService behandlingsresultatService,
                              BehandlingService behandlingService,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository,
                              MedlAnmodningsperiodeService medlAnmodningsperiodeService,
                              UtpekingsperiodeRepository utpekingsperiodeRepository,
                              MedlemskapsperiodeRepository medlemskapsperiodeRepository,
                              Unleash unleash) {
        this.persondataFasade = persondataFasade;
        this.medlService = medlService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.medlAnmodningsperiodeService = medlAnmodningsperiodeService;
        this.utpekingsperiodeRepository = utpekingsperiodeRepository;
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.unleash = unleash;
    }

    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fnr, "fnr kan ikke være null");
        return medlService.hentPeriodeListe(fnr, fom, tom);
    }

    public void opprettPeriodeForeløpig(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID, boolean erSed) {
        log.info("Oppretter foreløpig periode i MEDL for behandling {}", behandlingID);
        String fnr = hentFnr(behandlingID);
        Long medlPeriodeID =
            medlService.opprettPeriodeForeløpig(fnr, periodeOmLovvalg, hentKildedokumenttype(erSed, behandlingID));
        lagreMedlPeriodeId(medlPeriodeID, periodeOmLovvalg, behandlingID);
    }

    public void opprettPeriodeUnderAvklaring(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID, boolean erSed) {
        log.info("Oppretter periode under avklaring i MEDL for behandling {}", behandlingID);
        String fnr = hentFnr(behandlingID);
        Long medlPeriodeID =
            medlService.opprettPeriodeUnderAvklaring(fnr, periodeOmLovvalg, hentKildedokumenttype(erSed, behandlingID));
        lagreMedlPeriodeId(medlPeriodeID, periodeOmLovvalg, behandlingID);
    }

    public void oppdaterPeriodeUnderAvklaring(PeriodeOmLovvalg periodeOmLovvalg, boolean erSed, Long behandlingID) {
        medlService.oppdaterPeriodeUnderAvklaring(periodeOmLovvalg, hentKildedokumenttype(erSed, behandlingID));
    }

    public void opprettPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, Long behandlingID, boolean erSed) {
        String fnr = hentFnr(behandlingID);
        log.info("Oppretter endelig periode i MEDL for behandling {}", behandlingID);
        Long medlPeriodeID = medlService.opprettPeriodeEndelig(fnr, lovvalgsperiode,
            hentKildedokumenttype(erSed, behandlingID));
        lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandlingID);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void opprettPeriodeEndelig(long behandlingId, Medlemskapsperiode medlemskapsperiode) {
        String fnr = hentFnr(behandlingId);
        log.info("Oppretter endelig medlemskapsperiode i MEDL for behandling {}", behandlingId);
        Long medlPeriodeId = medlService.opprettPeriodeEndelig(fnr, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        if (medlPeriodeId == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingId);
        }

        lagreMedlPeriodeId(medlPeriodeId, medlemskapsperiode);
    }

    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, boolean erSed) {
        log.info("Oppdaterer MEDL-periode {} til status endelig", lovvalgsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeEndelig(lovvalgsperiode,
            hentKildedokumenttype(erSed, lovvalgsperiode.getBehandlingsresultat().getId()));
    }

    public void oppdaterPeriodeForeløpig(Lovvalgsperiode lovvalgsperiode, boolean erSed) {
        log.info("Oppdaterer MEDL-periode {} til status foreløpig", lovvalgsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeForeløpig(lovvalgsperiode,
            hentKildedokumenttype(erSed, lovvalgsperiode.getBehandlingsresultat().getId()));
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

    public void avsluttTidligerMedlPeriode(Fagsak fagsak) {
        Optional<Behandling> optionalBehandling = fagsak.finnTidligstInaktivBehandling();
        if (optionalBehandling.isPresent()) {
            Optional<Lovvalgsperiode> lovvalgsperiode = finnLovvalgsperiode(optionalBehandling.get());
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

    private KildedokumenttypeMedl hentKildedokumenttype(boolean erSed, Long behandlingID) {
        if (unleash.isEnabled(ToggleName.REGISTRERING_UNNTAK_FRA_MEDLEMSKAP)) {
            Behandling behandling = behandlingService.hentBehandling(behandlingID);
            var fagsaktype = behandling.getFagsak().getType();
            var behandlingstema = behandling.getTema();

            if (fagsaktype.equals(Sakstyper.TRYGDEAVTALE)) {
                if (behandlingstema == Behandlingstema.REGISTRERING_UNNTAK) {
                    return KildedokumenttypeMedl.DOKUMENT;
                } else if (behandlingstema == Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL) {
                    return KildedokumenttypeMedl.HENV_SOKNAD;
                }
            } else if (fagsaktype.equals(Sakstyper.EU_EOS) &&
                (behandlingstema == Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR ||
                    behandlingstema == Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)) {
                return KildedokumenttypeMedl.PortBlank_A1;
            }
        }

        return erSed ? KildedokumenttypeMedl.SED : KildedokumenttypeMedl.HENV_SOKNAD;
    }
}
