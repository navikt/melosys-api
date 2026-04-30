package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public record DokumentVariant(boolean saksbehandlerHarTilgang, String variantformat, String filtype) {
    public no.nav.melosys.domain.arkiv.DokumentVariant tilDomene() {
        no.nav.melosys.domain.arkiv.DokumentVariant.Filtype mappetFiltype = filtype != null
            ? no.nav.melosys.domain.arkiv.DokumentVariant.Filtype.valueOf(filtype)
            : null;
        return new no.nav.melosys.domain.arkiv.DokumentVariant(
            null,
            mappetFiltype,
            no.nav.melosys.domain.arkiv.DokumentVariant.VariantFormat.valueOf(variantformat),
            saksbehandlerHarTilgang
        );
    }
}
