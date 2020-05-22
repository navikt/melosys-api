package no.nav.melosys.saksflyt.steg.afl;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.Soeknadsland;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AflVurderInngangsvilkaar")
public class VurderInngangsvilkaar extends AbstraktStegBehandler {
    private final InngangsvilkaarService inngangsvilkaarService;
    private final FagsakService fagsakService;

    @Autowired
    public VurderInngangsvilkaar(InngangsvilkaarService inngangsvilkaarService,
                                 FagsakService fagsakService) {
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.fagsakService = fagsakService;
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

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Soeknadsland søknadsland = Soeknadsland.av(hentArbeidsLandFraSed(melosysEessiMelding));
        Periode periode = new Periode(melosysEessiMelding.getPeriode().getFom(), melosysEessiMelding.getPeriode().getTom());
        boolean kvalifisererForEF_883_2004  = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandling.getId(), søknadsland, periode);

        fagsakService.oppdaterType(prosessinstans.getBehandling().getFagsak(), kvalifisererForEF_883_2004);

        prosessinstans.setSteg(ProsessSteg.AFL_REGISTERKONTROLL);
    }

    private List<String> hentArbeidsLandFraSed(MelosysEessiMelding melosysEessiMelding) {
        List<String> arbeidsland = melosysEessiMelding.getArbeidssteder().stream()
            .map(a -> a.adresse.land)
            .collect(Collectors.toList());

        return arbeidsland.isEmpty() ? List.of(melosysEessiMelding.getLovvalgsland()) : arbeidsland;
    }
}
