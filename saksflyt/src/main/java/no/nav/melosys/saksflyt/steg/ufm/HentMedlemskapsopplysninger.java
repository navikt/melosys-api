package no.nav.melosys.saksflyt.steg.ufm;

import java.time.Instant;
import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("UnntakFraMedlemskapHentMedlemskapsopplysninger")
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final SaksopplysningRepository saksopplysningRepository;
    private final MedlFasade medlFasade;
    private final BehandlingService behandlingService;
    @Autowired
    HentMedlemskapsopplysninger(SaksopplysningRepository saksopplysningRepository,
                                MedlFasade medlFasade, BehandlingService behandlingService) {
        this.saksopplysningRepository = saksopplysningRepository;
        this.medlFasade = medlFasade;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_MEDLEMSKAP;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        Instant nå = Instant.now();
        String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        LocalDate fom = sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = sedDokument.getLovvalgsperiode().getTom();

        Saksopplysning saksopplysningMedlemskap = medlFasade.hentPeriodeListe(fnr, fom, tom);
        saksopplysningMedlemskap.setBehandling(prosessinstans.getBehandling());
        saksopplysningMedlemskap.setRegistrertDato(nå);
        saksopplysningMedlemskap.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysningMedlemskap);

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_HENT_YTELSER);
    }
}
