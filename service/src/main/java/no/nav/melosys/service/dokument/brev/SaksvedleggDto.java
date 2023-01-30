package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.brev.utkast.VedleggUtkast;

public record SaksvedleggDto(
    String journalpostID,
    String dokumentID
) {
    public static SaksvedleggDto av(VedleggUtkast.Saksvedlegg saksvedlegg) {
        return new SaksvedleggDto(saksvedlegg.journalpostID(), saksvedlegg.dokumentID());
    }

    public VedleggUtkast.Saksvedlegg tilUtkast() {
        return new VedleggUtkast.Saksvedlegg(this.journalpostID(), this.dokumentID());
    }
}
