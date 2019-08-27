package no.nav.melosys.integrasjon.utbetaldata;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public interface UtbetaldataFasade {

    Saksopplysning hentUtbetalingsinformasjon(String fnr, LocalDate fom, LocalDate tom) throws TekniskException, FunksjonellException;
}
