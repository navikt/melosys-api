package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record Saksopplysninger(
    String saksnummer,
    String navnBruker,
    String fnr
) {
    public static Saksopplysninger av(DokgenBrevbestilling brevbestilling, Aktoersroller mottakerType) {
        if (mottakerType == Aktoersroller.VIRKSOMHET) {
            return new Saksopplysninger(
                brevbestilling.getBehandling().getFagsak().getSaksnummer(),
                brevbestilling.getOrg().getNavn(),
                brevbestilling.getOrg().getOrgnummer()
            );
        }
        return new Saksopplysninger(
            brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getPersondokument().getSammensattNavn(),
            brevbestilling.getPersondokument().hentFolkeregisterident()
        );
    }
}
