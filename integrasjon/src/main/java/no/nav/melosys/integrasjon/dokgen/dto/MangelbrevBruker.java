package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;

public class MangelbrevBruker extends Mangelbrev {

    private MangelbrevBruker(MangelbrevBrevbestilling brevbestilling) {
        super(brevbestilling);
    }

    public static MangelbrevBruker av(MangelbrevBrevbestilling brevbestilling) {
        return new MangelbrevBruker(brevbestilling);
    }
}
