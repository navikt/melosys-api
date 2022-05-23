package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedGrunnlag implements StegBehandler {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final EessiService eessiService;

    public OpprettSedGrunnlag(BehandlingsgrunnlagService behandlingsgrunnlagService, EessiService eessiService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_SED_GRUNNLAG;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        SedGrunnlag sedGrunnlag = eessiService.hentSedGrunnlag(melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
        behandlingsgrunnlagService.opprettSedGrunnlag(prosessinstans.getBehandling().getId(), sedGrunnlag);
    }
}
