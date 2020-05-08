package no.nav.melosys.saksflyt.steg.afl;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Component;

@Component("AFLOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private final MedlPeriodeService medlPeriodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final SaksopplysningerService saksopplysningerService;
    private final BehandlingService behandlingService;

    public OppdaterMedl(MedlPeriodeService medlPeriodeService, LovvalgsperiodeService lovvalgsperiodeService, SaksopplysningerService saksopplysningerService, BehandlingService behandlingService) {
        this.medlPeriodeService = medlPeriodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.saksopplysningerService = saksopplysningerService;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPDATER_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final long behandlingId = prosessinstans.getBehandling().getId();

        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingId);
        SedDokument sedDokument = saksopplysningerService.hentSedOpplysninger(prosessinstans.getBehandling().getId());
        behandlingService.oppdaterStatus(prosessinstans.getBehandling().getId(), Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);

        if (lovvalgsperioder.isEmpty()) {
            lovvalgsperioder = lovvalgsperiodeService.lagreLovvalgsperioder(
                behandlingId, Collections.singletonList(sedDokument.opprettInnvilgetLovvalgsperiode())
            );
        }
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();

        medlPeriodeService.opprettPeriodeUnderAvklaring(lovvalgsperiode, prosessinstans.getBehandling().getId(), true);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
