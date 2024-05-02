package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public record SaksinfoVirksomhet(String saksnummer, String navnVirksomhet, String orgnr) implements Saksinfo {
    public static Saksinfo av(DokgenBrevbestilling brevbestilling) {
        return new SaksinfoVirksomhet(
                brevbestilling.getBehandling().getFagsak().getSaksnummer(),
            brevbestilling.getOrg().getNavn(),
            brevbestilling.getOrg().getOrgnummer()
        );
    }
}
