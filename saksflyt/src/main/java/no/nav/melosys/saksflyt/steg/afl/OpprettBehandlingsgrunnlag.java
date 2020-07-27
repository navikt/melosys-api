package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettBehandlingsgrunnlag extends AbstraktStegBehandler {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final OpprettSedDokumentFelles opprettSedDokumentFelles;
    private final EessiService eessiService;

    public OpprettBehandlingsgrunnlag(BehandlingsgrunnlagService behandlingsgrunnlagService, OpprettSedDokumentFelles opprettSedDokumentFelles, @Qualifier("system") EessiService eessiService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.opprettSedDokumentFelles = opprettSedDokumentFelles;
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPRETT_BEHANDLINGSGRUNNLAG;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        opprettSedDokumentFelles.opprettSedSaksopplysning(melosysEessiMelding, prosessinstans.getBehandling());

        SedGrunnlag sedGrunnlag = eessiService.hentSedGrunnlag(melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
        behandlingsgrunnlagService.opprettSedGrunnlag(prosessinstans.getBehandling().getId(), sedGrunnlag);

        if (prosessinstans.getBehandling().erNorgeUtpekt()) {
            prosessinstans.setSteg(ProsessSteg.AFL_VURDER_INNGANGSVILKÅR);
        } else  {
            prosessinstans.setSteg(ProsessSteg.AFL_REGISTERKONTROLL);
        }
    }
}
