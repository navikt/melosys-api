package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface InntektFasade {

    @Retryable(
        value = {IntegrasjonException.class},
        backoff = @Backoff(delay = 1_000, multiplier = 1.5))
    Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom);
}
