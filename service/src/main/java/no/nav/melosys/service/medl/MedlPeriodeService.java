package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedlPeriodeService {
    private static final Logger log = LoggerFactory.getLogger(MedlPeriodeService.class);

    private final TpsFasade tpsFasade;
    private final MedlFasade medlFasade;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;
    private final UtpekingsperiodeRepository utpekingsperiodeRepository;

    private static final String FEIL_VED_OPPDATERING_MEDL = "Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling ";

    public MedlPeriodeService(TpsFasade tpsFasade,
                              MedlFasade medlFasade,
                              BehandlingsresultatService behandlingsresultatService,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository,
                              AnmodningsperiodeRepository anmodningsperiodeRepository, UtpekingsperiodeRepository utpekingsperiodeRepository) {
        this.tpsFasade = tpsFasade;
        this.medlFasade = medlFasade;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
        this.utpekingsperiodeRepository = utpekingsperiodeRepository;
    }

    public Saksopplysning hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException {
        return medlFasade.hentPeriodeListe(fnr, fom, tom);
    }

    public void opprettPeriodeForeløpig(Medlemskapsperiode medlemskapsperiode, Long behandlingID, boolean erSed) throws TekniskException, FunksjonellException {
        opprettPeriodeForeløpig(medlemskapsperiode, behandlingID, erSed, hentFnr(behandlingID));
    }

    public void opprettPeriodeUnderAvklaring(Medlemskapsperiode medlemskapsperiode, Long behandlingID, boolean erSed) throws TekniskException, FunksjonellException {
        opprettPeriodeUnderAvklaring(medlemskapsperiode, behandlingID, erSed, hentFnr(behandlingID));
    }

    public void opprettPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, Long behandlingID, boolean erSed) throws TekniskException, FunksjonellException {
        opprettPeriodeEndelig(lovvalgsperiode, behandlingID, erSed, hentFnr(behandlingID));
    }

    public void opprettPeriodeForeløpig(Medlemskapsperiode medlemskapsperiode, Long behandlingID, boolean erSed, String fnr) throws TekniskException, FunksjonellException {
        log.info("Oppretter foreløpig periode i MEDL for behandling {}", behandlingID);
        Long medlPeriodeID = medlFasade.opprettPeriodeForeløpig(fnr, medlemskapsperiode, hentKildedokumenttype(erSed));
        lagreMedlPeriodeId(medlPeriodeID, medlemskapsperiode, behandlingID);
    }

    public void opprettPeriodeUnderAvklaring(Medlemskapsperiode medlemskapsperiode, Long behandlingID, boolean erSed, String fnr) throws TekniskException, FunksjonellException {
        log.info("Oppretter periode under avklaring i MEDL for behandling {}", behandlingID);
        Long medlPeriodeID = medlFasade.opprettPeriodeUnderAvklaring(fnr, medlemskapsperiode, hentKildedokumenttype(erSed));
        lagreMedlPeriodeId(medlPeriodeID, medlemskapsperiode, behandlingID);
    }

    public void opprettPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, Long behandlingID, boolean erSed, String fnr) throws TekniskException, FunksjonellException {
        log.info("Oppretter endelig periode i MEDL for behandling {}", behandlingID);
        Long medlPeriodeID = medlFasade.opprettPeriodeEndelig(fnr != null ? fnr : hentFnr(behandlingID), lovvalgsperiode, hentKildedokumenttype(erSed));
        lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandlingID);
    }

    public void oppdaterPeriodeEndelig(Lovvalgsperiode lovvalgsperiode, boolean erSed) throws FunksjonellException, TekniskException {
        log.info("Oppdaterer MEDL-periode {} til status endelig", lovvalgsperiode.getMedlPeriodeID());
        medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, hentKildedokumenttype(erSed));
    }

    public void avvisPeriode(Long medlPeriodeID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.info("Avviser MEDL-periode {} med status avvist", medlPeriodeID);
        medlFasade.avvisPeriode(medlPeriodeID, StatusaarsakMedl.AVVIST);
    }

    public void avvisPeriodeFeilregistrert(long medlPeriodeID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.info("Avviser MEDL-periode {} med status feilregistrert", medlPeriodeID);
        medlFasade.avvisPeriode(medlPeriodeID, StatusaarsakMedl.FEILREGISTRERT);
    }

    public void avvisPeriodeOpphørt(long medlPeriodeID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.info("Avviser MEDL-periode {} med status opphørt", medlPeriodeID);
        medlFasade.avvisPeriode(medlPeriodeID, StatusaarsakMedl.OPPHORT);
    }

    private String hentFnr(Long behandlingID) throws TekniskException, IkkeFunnetException {
        Behandling behandling = behandlingsresultatService.hentBehandlingsresultat(behandlingID).getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentBruker();
        return tpsFasade.hentIdentForAktørId(bruker.getAktørId());
    }

    private Optional<Lovvalgsperiode> finnLovvalgsperiode(Behandling behandling) throws IkkeFunnetException {
        return hentBehandlingsresultat(behandling).getLovvalgsperioder().stream().findFirst();
    }

    private Behandlingsresultat hentBehandlingsresultat(Behandling behandling) throws IkkeFunnetException {
        return behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Medlemskapsperiode medlemskapsperiode, long behandlingID) throws FunksjonellException {
        if (medlemskapsperiode instanceof Lovvalgsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, (Lovvalgsperiode) medlemskapsperiode, behandlingID);
        } else if (medlemskapsperiode instanceof Anmodningsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, (Anmodningsperiode) medlemskapsperiode, behandlingID);
        } else if (medlemskapsperiode instanceof Utpekingsperiode) {
            lagreMedlPeriodeId(medlPeriodeID, (Utpekingsperiode) medlemskapsperiode, behandlingID);
        } else {
            throw new UnsupportedOperationException("Uventet medlemskapsperiode kan ikke lagres");
        }
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingID);
        }
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Anmodningsperiode anmodningsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingID);
        }
        anmodningsperiode.setMedlPeriodeID(medlPeriodeID);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Utpekingsperiode utpekingsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException(FEIL_VED_OPPDATERING_MEDL + behandlingID);
        }
        utpekingsperiode.setMedlPeriodeID(medlPeriodeID);
        utpekingsperiodeRepository.save(utpekingsperiode);
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

    private KildedokumenttypeMedl hentKildedokumenttype(boolean erSed) {
        return erSed ? KildedokumenttypeMedl.SED : KildedokumenttypeMedl.HENV_SOKNAD;
    }
}
