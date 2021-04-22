package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public record LogiskVedlegg(String tittel) {
    public no.nav.melosys.domain.arkiv.LogiskVedlegg tilDomene() {
        return new no.nav.melosys.domain.arkiv.LogiskVedlegg(tittel);
    }
}
