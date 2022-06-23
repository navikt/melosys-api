package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public class SaksinfoVirksomhet extends Saksinfo {
    private final String navnVirksomhet;
    private final String orgnr;

    private SaksinfoVirksomhet(String saksnummer, String navnVirksomhet, String orgnr) {
        super(saksnummer);
        this.navnVirksomhet = navnVirksomhet;
        this.orgnr = orgnr;
    }

    public String getNavnVirksomhet() {
        return navnVirksomhet;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public static Saksinfo av(DokgenBrevbestilling brevbestilling) {
        return new SaksinfoVirksomhet(
            brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getOrg().getNavn(),
            brevbestilling.getOrg().getOrgnummer()
        );

    }
}
