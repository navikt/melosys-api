package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

public class MangelbrevBruker extends Mangelbrev {

    private MangelbrevBruker(MangelbrevBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        super(brevbestilling);
    }

    public static MangelbrevBruker av(MangelbrevBrevbestilling brevbestilling) throws IkkeFunnetException, TekniskException {
        return new MangelbrevBruker(brevbestilling);
    }
}
