package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public record DokumentVariant(boolean saksbehandlerHarTilgang, String variantformat) {
    public no.nav.melosys.domain.arkiv.DokumentVariant tilDomene() {
        return new no.nav.melosys.domain.arkiv.DokumentVariant(no.nav.melosys.domain.arkiv.DokumentVariant.VariantFormat.valueOf(variantformat),saksbehandlerHarTilgang);
    }
}
