package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.UtbetalingV1;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;

public class UtbetalingConsumerImpl implements UtbetalingConsumer {
    private UtbetalingV1 port;

    public UtbetalingConsumerImpl(UtbetalingV1 port) {
        this.port = port;
    }

    @Override
    public HentUtbetalingsinformasjonResponse hentUtbetalingsinformasjon(HentUtbetalingsinformasjonRequest request)
        throws HentUtbetalingsinformasjonPersonIkkeFunnet, HentUtbetalingsinformasjonPeriodeIkkeGyldig, HentUtbetalingsinformasjonIkkeTilgang {

        return port.hentUtbetalingsinformasjon(request);
    }
}
