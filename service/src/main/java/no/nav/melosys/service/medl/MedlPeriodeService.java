package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MedlPeriodeService {
    private static final Logger log = LoggerFactory.getLogger(MedlPeriodeService.class);

    private final PersondataFasade persondataFasade;
    private final MedlService medlService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;
    private final UtpekingsperiodeRepository utpekingsperiodeRepository;
    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;

    private static final String FEIL_VED_OPPDATERING_MEDL = "Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling ";

    public MedlPeriodeService(@Qualifier("system") PersondataFasade persondataFasade,
                              MedlService medlService,
                              BehandlingsresultatService behandlingsresultatService,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository,
                              AnmodningsperiodeRepository anmodningsperiodeRepository,
                              UtpekingsperiodeRepository utpekingsperiodeRepository,
                              MedlemskapsperiodeRepository medlemskapsperiodeRepository) {
        this.persondataFasade = persondataFasade;
        this.medlService = medlService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.utpekingsperiodeRepository = utpekingsperiodeRepository;
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
    }

    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws TekniskException {
        return medlService.hentPeriodeListe(fnr, fom, tom);
    }

    public void opprettPeriodeForeløpig(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID, boolean erSed) throws TekniskException, FunksjonellException {
        log.info("Oppretter foreløpig periode i MEDL for behandling {}", behandlingID);
        String fnr = hentFnr(behandlingID);
        Long medlPeriodeID = medlService.opprettPeriodeForeløpig(fnr, periodeOmLovvalg, hentKildedokumenttype(erSed));
        lagreMedlPeriodeId(medlPeriodeID, periodeOmLovvalg, behandlingID);
    }

    public void opprettPeriodeUnderAvklaring(PeriodeOmLovvalg periodeOmLovvalg, Long behandlingID, boolean erSed) throws TekniskException, FunksjonellException {
        log.info("Oppretter periode under avklaring i MEDL for behandling {}", behandlingID);
        String fnr = hentFnr(behandlingID);
        Long medlPeriodeID = medlService.opprettPeriodeUnderAvklaring(fnr, periodeOmLovvalg, hentKildedokumenttype(erSed));
        lagreMedlPeriodeId(medlPeriodeID, periodeOmLovvalg, behandlingID);
    }

    public void opprettPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, Long behandlingID, boolean erSed) throws TekniskException, FunksjonellException {
        String fnr = hentFnr(behandlingID);
        log.info("Oppretter endelig periode i MEDL for behandling {}", behandlingID);
        Long medlPeriodeID = medlService.opprettPeriodeEndelig(fnr, lovvalgsperiode, hentKildedokumenttype(erSed));
        lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandlingID);
    }

    public void opprettPeriodeEndelig(long behandlingId, Medlemskapsperiode medlemskapsperiode) throws FunksjonellException {
        String fnr = hentFnr(behandlingId);
        log.info("Oppretter endelig medlemskapsperiode i MEDL for behandling {}", behandlingId);
        Long medlPeriodeId = medlService.opprettPeriodeEndelig(fnr, medlemskapsperiode, KildedokumenttypeMedl.HENV_SOKNAD);

        if (medlPeriodeId == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingId);
        }

        lagreMedlPeriodeId(medlPeriodeId, medlemskapsperiode);
    }

    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, boolean erSed) throws TekniskException {
        log.info("Oppdaterer MEDL-periode {} til status endelig", lovvalgsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeEndelig(lovvalgsperiode, hentKildedokumenttype(erSed));
    }

    public void oppdaterPeriodeForeløpig(Lovvalgsperiode lovvalgsperiode, boolean erSed) throws TekniskException {
        log.info("Oppdaterer MEDL-periode {} til status foreløpig", lovvalgsperiode.getMedlPeriodeID());
        medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, hentKildedokumenttype(erSed));
    }

    public void avvisPeriode(Long medlPeriodeID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.info("Avviser MEDL-periode {} med status avvist", medlPeriodeID);
        avvisPeriode(medlPeriodeID, StatusaarsakMedl.AVVIST);
    }

    public void avvisPeriodeFeilregistrert(long medlPeriodeID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.info("Avviser MEDL-periode {} med status feilregistrert", medlPeriodeID);
        avvisPeriode(medlPeriodeID, StatusaarsakMedl.FEILREGISTRERT);
    }

    public void avvisPeriodeOpphørt(long medlPeriodeID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.info("Avviser MEDL-periode {} med status opphørt", medlPeriodeID);
        avvisPeriode(medlPeriodeID, StatusaarsakMedl.OPPHORT);
    }

    public void avsluttTidligerMedlPeriode(Fagsak fagsak) throws FunksjonellException {
        Behandling tidligereBehandling = fagsak.getTidligsteInaktiveBehandling();

        if (tidligereBehandling != null) {
            Optional<Lovvalgsperiode> lovvalgsperiode = finnLovvalgsperiode(tidligereBehandling);
            if (lovvalgsperiode.isPresent() && lovvalgsperiode.get().getMedlPeriodeID() != null) {
                log.info("Avslutter tidligere periode for fagsak {}", fagsak.getSaksnummer());
                avvisPeriode(lovvalgsperiode.get().getMedlPeriodeID());
            }
        }
    }

    private void avvisPeriode(long medlPeriodeId, StatusaarsakMedl statusaarsakMedl) throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlService.avvisPeriode(medlPeriodeId, statusaarsakMedl);
    }

    private String hentFnr(Long behandlingID) throws TekniskException, IkkeFunnetException {
        Behandling behandling = behandlingsresultatService.hentBehandlingsresultat(behandlingID).getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentBruker();
        return persondataFasade.hentFolkeregisterIdent(bruker.getAktørId());
    }

    private Optional<Lovvalgsperiode> finnLovvalgsperiode(Behandling behandling) throws IkkeFunnetException {
        return hentBehandlingsresultat(behandling).getLovvalgsperioder().stream().findFirst();
    }

    private Behandlingsresultat hentBehandlingsresultat(Behandling behandling) throws IkkeFunnetException {
        return behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, PeriodeOmLovvalg periodeOmLovvalg, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingID);
        }

        if (periodeOmLovvalg instanceof Lovvalgsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, (Lovvalgsperiode) periodeOmLovvalg);
        } else if (periodeOmLovvalg instanceof Anmodningsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, (Anmodningsperiode) periodeOmLovvalg);
        } else if (periodeOmLovvalg instanceof Utpekingsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, (Utpekingsperiode) periodeOmLovvalg);
        } else {
            throw new UnsupportedOperationException("Uventet periode med bestemmelse kan ikke lagres");
        }
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode) {
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Anmodningsperiode anmodningsperiode) {
        anmodningsperiode.setMedlPeriodeID(medlPeriodeID);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Utpekingsperiode utpekingsperiode) {
        utpekingsperiode.setMedlPeriodeID(medlPeriodeID);
        utpekingsperiodeRepository.save(utpekingsperiode);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeId, Medlemskapsperiode medlemskapsperiode) {
        medlemskapsperiode.setMedlPeriodeID(medlPeriodeId);
        medlemskapsperiodeRepository.save(medlemskapsperiode);
    }

    private KildedokumenttypeMedl hentKildedokumenttype(boolean erSed) {
        return erSed ? KildedokumenttypeMedl.SED : KildedokumenttypeMedl.HENV_SOKNAD;
    }
}
