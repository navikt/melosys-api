package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface InntektFasade {

    @Retryable(
        value = {IntegrasjonException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 120000, maxDelay = 480000, multiplier = 2))
    Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom) throws IntegrasjonException, FunksjonellException;
}
