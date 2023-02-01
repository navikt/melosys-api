package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.brev.utkast.Utkast;

public record SaksvedleggDto(
    String journalpostID,
    String dokumentID
) {
    public static SaksvedleggDto av(Utkast.Saksvedlegg saksvedlegg) {
        return new SaksvedleggDto(saksvedlegg.journalpostID(), saksvedlegg.dokumentID());
    }

    public Utkast.Saksvedlegg tilUtkast() {
        return new Utkast.Saksvedlegg(this.journalpostID(), this.dokumentID());
    }
}
