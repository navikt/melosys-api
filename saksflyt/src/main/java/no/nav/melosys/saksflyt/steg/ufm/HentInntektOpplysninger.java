package no.nav.melosys.saksflyt.steg.ufm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("UnntakFraMedlemskapHentInntektOpplysninger")
public class HentInntektOpplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentInntektOpplysninger.class);

    private final InntektService inntektService;
    private final SaksopplysningRepository saksopplysningRepository;
    private final BehandlingService behandlingService;

    @Autowired
    HentInntektOpplysninger(InntektService inntektService, SaksopplysningRepository saksopplysningRepository, BehandlingService behandlingService) {
        this.inntektService = inntektService;
        this.saksopplysningRepository = saksopplysningRepository;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_YTELSER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        Instant nå = Instant.now();

        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        Saksopplysning saksopplysning = hentInntektListe(fnr, fom ,tom);
        saksopplysning.setBehandling(prosessinstans.getBehandling());
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        //TODO: skal også sjekke mot UR. Sjekker nå kun mot inntektskomponent. MELOSYS-2496
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }

    private Saksopplysning hentInntektListe(String fnr, LocalDate fom, LocalDate tom) throws SikkerhetsbegrensningException, IntegrasjonException {

        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if(tom == null) {
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
