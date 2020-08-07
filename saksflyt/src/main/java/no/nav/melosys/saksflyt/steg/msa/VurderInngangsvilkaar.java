package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.springframework.stereotype.Component;

@Component("MottakSoknadAltinnVurderInngangsvilkaar")
public class VurderInngangsvilkaar implements StegBehandler {

    private final InngangsvilkaarService inngangsvilkaarService;
    private final FagsakService fagsakService;

    public VurderInngangsvilkaar(InngangsvilkaarService inngangsvilkaarService, FagsakService fagsakService) {
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg  inngangsSteg() {
        return ProsessSteg.MSA_VURDER_INNGANGSVILKÅR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        final Behandling behandling = prosessinstans.getBehandling();
        final BehandlingsgrunnlagData søknad = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();

        boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(
            behandling.getId(),
            søknad.soeknadsland.landkoder,
            søknad.periode
        );

        fagsakService.oppdaterType(behandling.getFagsak(), kvalifisererForEF_883_2004);
        prosessinstans.setSteg(ProsessSteg.MSA_OPPRETT_OPPGAVE);
    }
}
