package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.behandling.VilkaarsresultatService;

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

        Vilkaarsresultat art16Vilkaar = hentFørsteGyldigeVilkaarsresultatArt16(behandlingID);
        Set<VilkaarBegrunnelse> art16Begrunnelser = art16Vilkaar.getBegrunnelser();

        boolean harVilkaarForArtikkel12 = vilkaarsresultatService.harVilkaarForUtsending(behandlingID);
        Set<VilkaarBegrunnelse> anmodningBegrunnelser = harVilkaarForArtikkel12 ? art16Begrunnelser : Collections.emptySet();
        Set<VilkaarBegrunnelse> anmodningUtenArt12Begrunnelser = harVilkaarForArtikkel12 ? Collections.emptySet() : art16Begrunnelser;

        var hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        return new BrevDataAnmodningUnntak(saksbehandler, landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse(),
            hovedvirksomhet, hovedvirksomhet.yrkesaktivitet, anmodningBegrunnelser, anmodningUtenArt12Begrunnelser, art16Vilkaar.getBegrunnelseFritekst());
    }

    // Vilkåret for art16 er både oppfylt og har begrunnelser ved anmodning om unntak
    private Vilkaarsresultat hentFørsteGyldigeVilkaarsresultatArt16(long behandlingID) {
        return vilkaarsresultatService.finnUnntaksVilkaarsresultat(behandlingID)
            .filter(v -> v.isOppfylt() && !v.getBegrunnelser().isEmpty())
            .orElseThrow(() -> new TekniskException("Ingen oppfylte unntaksvilkår med vilkårbegrunnelser funnet for brev om orientering anmodning om unntak"));
    }
}
