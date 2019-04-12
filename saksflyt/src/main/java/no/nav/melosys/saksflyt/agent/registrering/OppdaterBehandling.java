package no.nav.melosys.saksflyt.agent.registrering;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OppdaterBehandling extends AbstraktStegBehandler {

    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OppdaterMedlFelles felles;
    private final MedlFasade medlFasade;
    private final SaksopplysningRepository saksopplysningRepository;

    public OppdaterBehandling(LovvalgsperiodeService lovvalgsperiodeService, OppdaterMedlFelles felles, MedlFasade medlFasade, SaksopplysningRepository saksopplysningRepository) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.felles = felles;
        this.medlFasade = medlFasade;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPDATER_BEHANDLING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    @Transactional(rollbackFor = MelosysException.class)
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        SedDokument sedDokument = (SedDokument) hentSedSaksopplysningFraBehandling(prosessinstans.getBehandling())
            .orElseThrow(() -> new TekniskException("Finner ikke SED-saksopplysning for behandling " + prosessinstans.getBehandling().getId()))
            .getDokument();

        boolean erEndring = prosessinstans.getData(ProsessDataKey.ER_ENDRING, Boolean.class);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(sedDokument.getLovvalgBestemmelse());
        lovvalgsperiode.setFom(sedDokument.getPeriode().getFom());
        lovvalgsperiode.setTom(sedDokument.getPeriode().getTom());
        lovvalgsperiode.setUnntakFraLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setLovvalgsland(sedDokument.getLovvalgsland());

        if (erEndring) {
            //TODO: MELOSYS-2532. Bruke medl-periode fra tidligere behandling
        }

        lovvalgsperiodeService.lagreLovvalgsperioder(prosessinstans.getBehandling().getId(), Collections.singletonList(lovvalgsperiode));

        Long medlId = medlFasade.opprettPeriodeUnderAvklaring(prosessinstans.getData(ProsessDataKey.BRUKER_ID), lovvalgsperiode);
        felles.lagreMedlPeriodeId(medlId, lovvalgsperiode, prosessinstans.getBehandling().getId());

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_VALIDER_PERIODE);
    }

    private Optional<Saksopplysning> hentSedSaksopplysningFraBehandling(Behandling behandling) {
        return saksopplysningRepository.findByBehandlingAndType(behandling, SaksopplysningType.SEDOPPLYSNINGER);
    }
}
