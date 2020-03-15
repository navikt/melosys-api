package no.nav.melosys.saksflyt.steg.afl;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AFLOppdaterMedl")
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final MedlPeriodeService medlPeriodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final SaksopplysningerService saksopplysningerService;
    private final FagsakService fagsakService;

    public OppdaterMedl(MedlFasade medlFasade, MedlPeriodeService medlPeriodeService, LovvalgsperiodeService lovvalgsperiodeService, SaksopplysningerService saksopplysningerService, FagsakService fagsakService) {
        this.medlFasade = medlFasade;
        this.medlPeriodeService = medlPeriodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.saksopplysningerService = saksopplysningerService;
        this.fagsakService = fagsakService;
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
        SedDokument sedDokument = saksopplysningerService.hentSedOpplysninger(prosessinstans.getBehandling().getId());
        Fagsak fagsak = fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer());
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);

        if (lovvalgsperioder.isEmpty()) {
            lovvalgsperioder = lovvalgsperiodeService.lagreLovvalgsperioder(
                behandlingId, Collections.singletonList(sedDokument.opprettInnvilgetLovvalgsperiode())
            );
        }
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();

        long medlPeriodeID = medlFasade.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode, KildedokumenttypeMedl.SED);
        log.info("Medl-periode opprettet med id {} for behandling {}", medlPeriodeID, behandlingId);
        medlPeriodeService.lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandlingId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
