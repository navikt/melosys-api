package no.nav.melosys.saksflyt.steg.afl;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.felles.UnntaksperiodeUtils;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AFLOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final OppdaterMedlFelles oppdaterMedlFelles;
    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public OppdaterMedl(MedlFasade medlFasade, OppdaterMedlFelles oppdaterMedlFelles, BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.medlFasade = medlFasade;
        this.oppdaterMedlFelles = oppdaterMedlFelles;
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final long behandlingId = prosessinstans.getBehandling().getId();
        final String fnr = prosessinstans.getData(ProsessDataKey.BRUKER_ID);

        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingId);
        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(
            behandlingService.hentBehandling(behandlingId)
        );

        if (lovvalgsperioder.isEmpty()) {
            lovvalgsperioder = lovvalgsperiodeService.lagreLovvalgsperioder(
                behandlingId, Collections.singletonList(UnntaksperiodeUtils.opprettLovvalgsperiode(sedDokument))
            );
        }
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();

        long medlPeriodeID = medlFasade.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode, KildedokumenttypeMedl.SED);
        log.info("Medl-periode opprettet med id {} for behandling {}", medlPeriodeID, behandlingId);
        oppdaterMedlFelles.lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandlingId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
