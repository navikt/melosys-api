package no.nav.melosys.integrasjon.utbetaldata;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface UtbetaldataFasade {

    @Retryable(
        value = {IntegrasjonException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 120000, maxDelay = 480000, multiplier = 2))
    Saksopplysning hentUtbetalingerBarnetrygd(String fnr, LocalDate fom, LocalDate tom) throws TekniskException, FunksjonellException;
}
