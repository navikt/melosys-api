package no.nav.melosys.saksflyt.felles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HentOpplysningerFelles {
    private static final Logger log = LoggerFactory.getLogger(HentOpplysningerFelles.class);

    private final TpsFasade tpsFasade;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final MedlFasade medlFasade;
    private final InntektService inntektService;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public HentOpplysningerFelles(@Qualifier("system") TpsFasade tpsFasade,
                                  FagsakService fagsakService,
                                  BehandlingService behandlingService,
                                  MedlFasade medlFasade,
                                  InntektService inntektService,
                                  SaksopplysningRepository saksopplysningRepository) {
        this.tpsFasade = tpsFasade;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.medlFasade = medlFasade;
        this.inntektService = inntektService;
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

    @Transactional
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

    @Transactional
    public void hentOgLagreInntektsopplysninger(long behandlingId, String fnr)
        throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {

        Instant nå = Instant.now();

        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        Saksopplysning saksopplysning = hentInntektListe(fnr, fom, tom);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        log.info("Inntektsdokument hentet for behandling {}", behandling.getId());
    }

    private Saksopplysning hentInntektListe(String fnr, LocalDate fom, LocalDate tom) throws SikkerhetsbegrensningException, IntegrasjonException {

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

        return inntektService.hentInntektListe(fnr, fomMnd, tomMnd);
    }
}
