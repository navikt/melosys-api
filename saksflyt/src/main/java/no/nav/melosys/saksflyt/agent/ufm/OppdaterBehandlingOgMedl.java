package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
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

@Component
public class OppdaterBehandlingOgMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingOgMedl.class);

    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OppdaterMedlFelles felles;
    private final MedlFasade medlFasade;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public OppdaterBehandlingOgMedl(LovvalgsperiodeService lovvalgsperiodeService, OppdaterMedlFelles felles, MedlFasade medlFasade, SaksopplysningRepository saksopplysningRepository) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.felles = felles;
        this.medlFasade = medlFasade;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPDATER_BEHANDLING_OG_MEDL;
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

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(sedDokument.getLovvalgBestemmelse());
        lovvalgsperiode.setFom(sedDokument.getPeriode().getFom());
        lovvalgsperiode.setTom(sedDokument.getPeriode().getTom());
        lovvalgsperiode.setUnntakFraLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setLovvalgsland(sedDokument.getLovvalgsland());
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
        lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);

        if (prosessinstans.getData(ProsessDataKey.ER_ENDRING, Boolean.class)) {
            avsluttTidligerMedlPeriode(prosessinstans.getBehandling().getFagsak());
        }

        lovvalgsperiode = lovvalgsperiodeService.lagreLovvalgsperioder(prosessinstans.getBehandling().getId(), Collections.singletonList(lovvalgsperiode)).iterator().next();
        Long medlId = medlFasade.opprettPeriodeUnderAvklaring(prosessinstans.getData(ProsessDataKey.BRUKER_ID), lovvalgsperiode);
        felles.lagreMedlPeriodeId(medlId, lovvalgsperiode, prosessinstans.getBehandling().getId());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_PERIODE);
    }

    private void avsluttTidligerMedlPeriode(Fagsak fagsak) throws FunksjonellException {
        Behandling tidligereBehandling = fagsak.getTidligsteInaktiveBehandling();

        if (tidligereBehandling != null) {
            Lovvalgsperiode lovvalgsperiode = felles.hentLovvalgsperiode(tidligereBehandling);
            medlFasade.avvisPeriode(lovvalgsperiode, StatusaarsakMedl.OPPHORT);
        }
    }

    private Optional<Saksopplysning> hentSedSaksopplysningFraBehandling(Behandling behandling) {
        return saksopplysningRepository.findByBehandlingAndType(behandling, SaksopplysningType.SED_OPPLYSNINGER);
    }
}
