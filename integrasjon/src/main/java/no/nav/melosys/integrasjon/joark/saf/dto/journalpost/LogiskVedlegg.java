package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public record LogiskVedlegg(String logiskVedleggId, String tittel) {
    public no.nav.melosys.domain.arkiv.LogiskVedlegg tilDomene() {
        return new no.nav.melosys.domain.arkiv.LogiskVedlegg(logiskVedleggId, tittel);
    }
}
