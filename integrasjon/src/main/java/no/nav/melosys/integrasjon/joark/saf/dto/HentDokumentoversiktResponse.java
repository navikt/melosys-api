package no.nav.melosys.integrasjon.joark.saf.dto;

import java.util.Collection;

import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost;

public record HentDokumentoversiktResponse(
    Collection<Journalpost> journalposter,
    SideInfo sideInfo
) {
}
