package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.util.Collection;

public record HentDokumentoversiktResponse(
    Collection<Journalpost> journalposter,
    SideInfo sideInfo
) {
}
