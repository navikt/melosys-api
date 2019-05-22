package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RegistreringUnntakOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final OppdaterMedlFelles felles;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, OppdaterMedlFelles felles, LovvalgsperiodeService lovvalgsperiodeService, SaksopplysningRepository saksopplysningRepository) {
        this.medlFasade = medlFasade;
        this.felles = felles;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPDATER_MEDL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        SedDokument sedDokument = (SedDokument) hentSedSaksopplysningFraBehandling(prosessinstans.getBehandling())
            .orElseThrow(() -> new TekniskException("Finner ikke SED-saksopplysning for behandling " + prosessinstans.getBehandling().getId()))
            .getDokument();


        Collection<Lovvalgsperiode> lagretLovvalgsperiode = lovvalgsperiodeService.lagreLovvalgsperioder(
            prosessinstans.getBehandling().getId(), Collections.singletonList(opprettLovvalgsperiode(sedDokument))
        );

        String ident = SaksopplysningerUtils.hentPersonDokument(prosessinstans.getBehandling()).fnr;

        Lovvalgsperiode lovvalgsperiode = lagretLovvalgsperiode.iterator().next();
        Long medlId = medlFasade.opprettPeriodeEndelig(ident, lovvalgsperiode, KildedokumenttypeMedl.SED);
        felles.lagreMedlPeriodeId(medlId, lovvalgsperiode, prosessinstans.getBehandling().getId());

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    private Lovvalgsperiode opprettLovvalgsperiode(SedDokument sedDokument) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(sedDokument.getLovvalgBestemmelse());
        lovvalgsperiode.setFom(sedDokument.getPeriode().getFom());
        lovvalgsperiode.setTom(sedDokument.getPeriode().getTom());
        lovvalgsperiode.setLovvalgsland(sedDokument.getLovvalgsland());
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
        lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);

        return lovvalgsperiode;
    }

    private Optional<Saksopplysning> hentSedSaksopplysningFraBehandling(Behandling behandling) {
        return saksopplysningRepository.findByBehandlingAndType(behandling, SaksopplysningType.SEDOPPL);
    }
}
