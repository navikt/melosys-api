package no.nav.melosys.saksflyt.steg.afl;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AflVurderInngangsvilkaar")
public class VurderInngangsvilkaar extends AbstraktStegBehandler {
    private final InngangsvilkaarService inngangsvilkaarService;
    private final FagsakService fagsakService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Autowired
    public VurderInngangsvilkaar(InngangsvilkaarService inngangsvilkaarService,
                                 FagsakService fagsakService, BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.fagsakService = fagsakService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_VURDER_INNGANGSVILKÅR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        Behandling behandling = prosessinstans.getBehandling();
        if (!behandling.erNorgeUtpekt()) {
            throw new TekniskException("Steget vurderer inngangsvilkår når Norge er utpekt, ikke for " + behandling.getTema());
        }

        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandling.getId());
        BehandlingsgrunnlagData data = behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        List<String> arbeidsland = data.hentUtenlandskeArbeidsstederLandkode();

        boolean kvalifisererForEF_883_2004  = inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            behandling.getId(),
            arbeidsland.isEmpty() ? List.of(Landkoder.NO.getKode()) : arbeidsland,
            data.periode
        );

        fagsakService.oppdaterType(prosessinstans.getBehandling().getFagsak(), kvalifisererForEF_883_2004);

        prosessinstans.setSteg(ProsessSteg.AFL_REGISTERKONTROLL);
    }
}
