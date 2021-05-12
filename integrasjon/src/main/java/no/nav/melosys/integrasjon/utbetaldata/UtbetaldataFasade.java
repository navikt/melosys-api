package no.nav.melosys.integrasjon.utbetaldata;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface UtbetaldataFasade {

    @Retryable(
        value = {IntegrasjonException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1_000, multiplier = 1.5))
    Saksopplysning hentUtbetalingerBarnetrygd(String fnr, LocalDate fom, LocalDate tom);
}
