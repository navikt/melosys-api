package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.DokgenMetaKey;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

public class MangelbrevArbeidsgiver extends Mangelbrev {

    private final String navnFullmektig;

    private MangelbrevArbeidsgiver(DokgenBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        super(brevbestilling);
        this.navnFullmektig = brevbestilling.getVariabeltFelt(DokgenMetaKey.FULLMEKTIGNAVN, String.class);
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public static MangelbrevArbeidsgiver av(DokgenBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        return new MangelbrevArbeidsgiver(brevbestilling);
    }
}
