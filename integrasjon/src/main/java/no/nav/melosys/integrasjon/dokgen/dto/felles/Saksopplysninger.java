package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public record Saksopplysninger(
    String saksnummer,
    String navnBruker,
    String fnr
) {
    public static Saksopplysninger av(DokgenBrevbestilling brevbestilling) {
        return new Saksopplysninger(
            brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getPersondokument().getSammensattNavn(),
            brevbestilling.getPersondokument().hentFolkeregisterident()
        );
    }
}
