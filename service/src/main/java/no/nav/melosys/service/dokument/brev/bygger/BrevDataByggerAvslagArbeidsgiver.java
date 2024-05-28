package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;

import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;

public class BrevDataByggerAvslagArbeidsgiver implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BrevDataByggerAvslagArbeidsgiver(LandvelgerService landvelgerService,
                                            LovvalgsperiodeService lovvalgsperiodeService,
                                            VilkaarsresultatService vilkaarsresultatService) {
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();

        BrevDataAvslagArbeidsgiver brevData = new BrevDataAvslagArbeidsgiver(saksbehandler);
        brevData.setPerson(dataGrunnlag.getPerson());

        brevData.setHovedvirksomhet(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet());
        brevData.setLovvalgsperiode(lovvalgsperiodeService.hentLovvalgsperiode(behandlingID));
        brevData.setArbeidsland(landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse());

        brevData.setVilkårbegrunnelser121(
            vilkaarsresultatService.finnUtsendingsVilkaarsresultat(behandlingID).map(Vilkaarsresultat::getBegrunnelser).orElse(Collections.emptySet()));
        brevData.setVilkårbegrunnelser121VesentligVirksomhet(
            vilkaarsresultatService.finnVilkaarsresultat(behandlingID, ART12_1_VESENTLIG_VIRKSOMHET).map(Vilkaarsresultat::getBegrunnelser).orElse(Collections.emptySet()));

        return brevData;
    }
}
