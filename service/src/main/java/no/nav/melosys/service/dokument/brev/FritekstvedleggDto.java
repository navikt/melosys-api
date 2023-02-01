package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.brev.utkast.Utkast;

public record FritekstvedleggDto(
    String tittel,
    String fritekst
) {
    public static FritekstvedleggDto av(Utkast.FritekstVedlegg fritekstVedlegg) {
        return new FritekstvedleggDto(fritekstVedlegg.tittel(), fritekstVedlegg.fritekst());
    }

    public Utkast.FritekstVedlegg tilUtkast() {
        return new Utkast.FritekstVedlegg(this.tittel(), this.fritekst());
    }
}
