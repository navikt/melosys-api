package no.nav.melosys.service.abac;

import no.nav.freg.abac.core.annotation.context.AbacContext;
import no.nav.freg.abac.core.service.AbacService;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.sikkerhet.abac.PepImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PepAktoerOversetter extends PepImpl {

    protected final TpsFasade tpsFasade;

    @Autowired
    public PepAktoerOversetter(TpsFasade tpsFasade, AbacService abacService, AbacContext abacContext) {
        super(abacService, abacContext);
        this.tpsFasade = tpsFasade;
    }

    public void sjekkTilgangTil(Aktoer aktør) throws SikkerhetsbegrensningException, IkkeFunnetException {
        String fnr = tpsFasade.hentIdentForAktørId(aktør.getAktørId());
        sjekkTilgangTil(fnr);
    }
}
