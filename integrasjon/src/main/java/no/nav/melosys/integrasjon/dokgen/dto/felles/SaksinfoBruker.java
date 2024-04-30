package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public record SaksinfoBruker(String saksnummer, String navnBruker, String fnr) implements Saksinfo {
    public static Saksinfo av(DokgenBrevbestilling brevbestilling) {
        return new SaksinfoBruker(
                brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getPersondokument().getSammensattNavn(),
            brevbestilling.getPersondokument().hentFolkeregisterident()
        );
    }
}
