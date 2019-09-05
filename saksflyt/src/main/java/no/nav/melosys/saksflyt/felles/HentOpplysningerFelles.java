package no.nav.melosys.saksflyt.felles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HentOpplysningerFelles {
    private static final Logger log = LoggerFactory.getLogger(HentOpplysningerFelles.class);

    private final TpsFasade tpsFasade;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final MedlFasade medlFasade;
    private final InntektService inntektService;
    private final UtbetaldataService utbetaldataService;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public HentOpplysningerFelles(@Qualifier("system") TpsFasade tpsFasade,
                                  FagsakService fagsakService,
                                  BehandlingService behandlingService,
                                  MedlFasade medlFasade,
                                  InntektService inntektService,
                                  UtbetaldataService utbetaldataService,
                                  SaksopplysningRepository saksopplysningRepository) {
        this.tpsFasade = tpsFasade;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.medlFasade = medlFasade;
        this.inntektService = inntektService;
        this.utbetaldataService = utbetaldataService;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    public String hentOgLagrePersonopplysninger(String aktørId, Behandling behandling) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        String ident = tpsFasade.hentIdentForAktørId(aktørId);

        fagsakService.leggTilAktør(behandling.getFagsak().getSaksnummer(), Aktoersroller.BRUKER, aktørId);

        Instant nå = Instant.now();

        Saksopplysning saksopplysning = tpsFasade.hentPerson(ident);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        log.info("Persondokument hentet for behandling {}", behandling.getId());
        return ident;
    }

    public void hentOgLagreMedlemskapsopplysninger(long behandlingId, String fnr)
        throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {

        Instant nå = Instant.now();

        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        Saksopplysning saksopplysningMedlemskap = medlFasade.hentPeriodeListe(fnr, fom, tom);
        saksopplysningMedlemskap.setBehandling(behandling);
        saksopplysningMedlemskap.setRegistrertDato(nå);
        saksopplysningMedlemskap.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysningMedlemskap);

        log.info("Medlemskapsdokument hentet for behandling {}", behandling.getId());
    }

    public void hentOgLagreInntektsopplysninger(long behandlingId, String fnr)
        throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {

        Instant nå = Instant.now();

        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        Periode periode = hentPeriodeForYtelser(fom, tom);
        Saksopplysning saksopplysning = inntektService.hentInntektListe(fnr, periode.fom, periode.tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        log.info("Inntektsdokument hentet for behandling {}", behandling.getId());
    }

    public void hentOgLagreUtbetalingsopplysninger(long behandlingId, String fnr) throws FunksjonellException, TekniskException {

        Instant nå = Instant.now();

        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        Periode periode = hentPeriodeForYtelser(fom, tom);
        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd(fnr, periode.fom.atDay(1), periode.tom.atDay(1));
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        log.info("Utbetalingsdokument hentet for behandling {}", behandlingId);
    }

    private static Periode hentPeriodeForYtelser(LocalDate fom, LocalDate tom) {

        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if (tom == null) {
            fomMnd = YearMonth.from(fom);
            tomMnd = YearMonth.from(fom.plusYears(2));
        } else if (fom.isBefore(nå) && tom.isAfter(nå)) { //1. Periode påbegynt: utbetalinger periode med 2 mnd tilbake
            fomMnd = YearMonth.from(fom.minusMonths(2L));
            tomMnd = YearMonth.from(tom);
        } else if (fom.isAfter(nå)) { //2. Periode ikke påbegynt. Inneværende mnd og 2 mnd tilbake
            fomMnd = YearMonth.from(nå.minusMonths(2L));
            tomMnd = YearMonth.from(nå);
        } else { //3. Avsluttet: sjekker hele periode
            fomMnd = YearMonth.from(fom);
            tomMnd = YearMonth.from(tom);
        }

        return new Periode(fomMnd, tomMnd);
    }

    private static final class Periode {
        private YearMonth fom;
        private YearMonth tom;

        Periode(YearMonth fom, YearMonth tom) {
            this.fom = fom;
            this.tom = tom;
        }
    }
}
