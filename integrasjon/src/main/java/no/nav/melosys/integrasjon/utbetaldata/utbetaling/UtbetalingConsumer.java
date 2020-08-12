package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import javax.xml.ws.soap.SOAPFaultException;

import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface UtbetalingConsumer {

    @Retryable(
        value = {SOAPFaultException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 120000, maxDelay = 480000, multiplier = 2))
    HentUtbetalingsinformasjonResponse hentUtbetalingsinformasjon(HentUtbetalingsinformasjonRequest request)
        throws HentUtbetalingsinformasjonPersonIkkeFunnet, HentUtbetalingsinformasjonPeriodeIkkeGyldig, HentUtbetalingsinformasjonIkkeTilgang;
}
