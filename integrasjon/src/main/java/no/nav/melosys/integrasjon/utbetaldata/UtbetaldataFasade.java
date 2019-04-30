package no.nav.melosys.integrasjon.utbetaldata;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;

public interface UtbetaldataFasade {

    Saksopplysning hentUtbetalingsinformasjon(String fnr) throws IntegrasjonException, FunksjonellException;
}
