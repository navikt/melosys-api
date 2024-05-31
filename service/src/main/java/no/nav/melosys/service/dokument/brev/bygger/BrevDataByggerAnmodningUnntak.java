package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

import java.util.Collections;
import java.util.Set;

public class BrevDataByggerAnmodningUnntak implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BrevDataByggerAnmodningUnntak(LandvelgerService landvelgerService,
                                         VilkaarsresultatService vilkaarsresultatService) {
        this.landvelgerService = landvelgerService;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        long behandlingID = dataGrunnlag.getBehandling().getId();
        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() != 1) {
            throw new FunksjonellException(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET.getBeskrivelse());
        }

        Vilkaarsresultat anmodningOmUnntakVilkaarsresultat = hentFørsteGyldigeVilkaarsresultatAnmodningOmUnntak(behandlingID);
        Set<VilkaarBegrunnelse> anmodningOmUnntakVilkaarBegrunnelser = anmodningOmUnntakVilkaarsresultat.getBegrunnelser();

        boolean harVilkaarForUtsending = vilkaarsresultatService.harVilkaarForUtsending(behandlingID);
        Set<VilkaarBegrunnelse> anmodningBegrunnelser = harVilkaarForUtsending ? anmodningOmUnntakVilkaarBegrunnelser : Collections.emptySet();
        Set<VilkaarBegrunnelse> anmodningUtenArt12Begrunnelser = harVilkaarForUtsending ? Collections.emptySet() : anmodningOmUnntakVilkaarBegrunnelser;

        var hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        return new BrevDataAnmodningUnntak(saksbehandler, landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse(), hovedvirksomhet,
            hovedvirksomhet.yrkesaktivitet, anmodningBegrunnelser, anmodningUtenArt12Begrunnelser, anmodningOmUnntakVilkaarsresultat.getBegrunnelseFritekst());
    }

    // Vilkåret for art16 er både oppfylt og har begrunnelser ved anmodning om unntak
    private Vilkaarsresultat hentFørsteGyldigeVilkaarsresultatAnmodningOmUnntak(long behandlingID) {
        var vilkaarsresultat = vilkaarsresultatService.finnUnntaksVilkaarsresultat(behandlingID);
        if (vilkaarsresultat == null || !vilkaarsresultat.isOppfylt() || vilkaarsresultat.getBegrunnelser().isEmpty()) {
            throw new FunksjonellException("Ingen oppfylte unntaksvilkår med vilkårbegrunnelser funnet for brev om orientering anmodning om unntak");
        }
        return vilkaarsresultat;
    }
}
