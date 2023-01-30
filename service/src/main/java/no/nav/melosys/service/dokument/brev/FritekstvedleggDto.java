package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.brev.utkast.VedleggUtkast;

public record FritekstvedleggDto(
    String tittel,
    String fritekst
) {
    public static FritekstvedleggDto av(VedleggUtkast.FritekstVedlegg fritekstVedlegg) {
        return new FritekstvedleggDto(fritekstVedlegg.tittel(), fritekstVedlegg.fritekst());
    }

    public VedleggUtkast.FritekstVedlegg tilUtkast() {
        return new VedleggUtkast.FritekstVedlegg(this.tittel(), this.fritekst());
    }
}
