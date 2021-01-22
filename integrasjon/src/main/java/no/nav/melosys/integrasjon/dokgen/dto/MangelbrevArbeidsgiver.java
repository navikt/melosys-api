package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

public class MangelbrevArbeidsgiver extends Mangelbrev {

    private final String navnFullmektig;

    private MangelbrevArbeidsgiver(MangelbrevBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        super(brevbestilling);
        this.navnFullmektig = brevbestilling.getFullmektigNavn();
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public static MangelbrevArbeidsgiver av(MangelbrevBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        return new MangelbrevArbeidsgiver(brevbestilling);
    }
}
