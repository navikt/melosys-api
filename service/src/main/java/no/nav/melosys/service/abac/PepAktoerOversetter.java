package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.sikkerhet.abac.PepImpl;

public class PepAktoerOversetter extends PepImpl {

    protected final TpsFasade tpsFasade;

    protected PepAktoerOversetter(TpsFasade tpsFasade) {
        this.tpsFasade = tpsFasade;
    }

    public void sjekkTilgangTil(Aktoer aktør) throws SikkerhetsbegrensningException, IkkeFunnetException {
        String fnr = tpsFasade.hentIdentForAktørId(aktør.getAktørId());
        sjekkTilgangTil(fnr);
    }
}
