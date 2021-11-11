package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public record Saksopplysninger(
    String fnr,
    String saksnummer,
    String navnBruker
) {
    public static Saksopplysninger av(DokgenBrevbestilling brevbestilling) {
        return new Saksopplysninger(
            brevbestilling.getPersondokument().hentFolkeregisterident(),
            brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getPersondokument().getSammensattNavn()
        );
    }
}
