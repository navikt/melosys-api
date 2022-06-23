package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public class SaksinfoBruker extends Saksinfo {
    private final String navnBruker;
    private final String fnr;

    private SaksinfoBruker(String saksnummer, String navnBruker, String fnr) {
        super(saksnummer);
        this.navnBruker = navnBruker;
        this.fnr = fnr;
    }

    public String getNavnBruker() {
        return navnBruker;
    }

    public String getFnr() {
        return fnr;
    }

    public static Saksinfo av(DokgenBrevbestilling brevbestilling) {
        return new SaksinfoBruker(
            brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getPersondokument().getSammensattNavn(),
            brevbestilling.getPersondokument().hentFolkeregisterident()
        );
    }
}
