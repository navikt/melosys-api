package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;

public class MangelbrevArbeidsgiver extends Mangelbrev {

    private final String navnFullmektig;

    private MangelbrevArbeidsgiver(MangelbrevBrevbestilling brevbestilling) {
        super(brevbestilling);
        this.navnFullmektig = brevbestilling.getFullmektigNavn();
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public static MangelbrevArbeidsgiver av(MangelbrevBrevbestilling brevbestilling) {
        return new MangelbrevArbeidsgiver(brevbestilling);
    }
}
