package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("UnntakFraMedlemskapHentMedlemskapsopplysninger")
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final SaksopplysningRepository saksopplysningRepository;
    private final MedlFasade medlFasade;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    HentMedlemskapsopplysninger(SaksopplysningRepository saksopplysningRepository,
                                MedlFasade medlFasade, BehandlingRepository behandlingRepository) {
        this.saksopplysningRepository = saksopplysningRepository;
        this.medlFasade = medlFasade;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_MEDLEMSKAP;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());

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
