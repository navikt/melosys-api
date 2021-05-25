package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.UtbetalingV1;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse;

public class UtbetalingConsumerImpl implements UtbetalingConsumer {
    private UtbetalingV1 port;

    public UtbetalingConsumerImpl(UtbetalingV1 port) {
        this.port = port;
    }

    @Override
    public WSHentUtbetalingsinformasjonResponse hentUtbetalingsinformasjon(WSHentUtbetalingsinformasjonRequest request)
        throws HentUtbetalingsinformasjonPersonIkkeFunnet, HentUtbetalingsinformasjonPeriodeIkkeGyldig, HentUtbetalingsinformasjonIkkeTilgang {

        return port.hentUtbetalingsinformasjon(request);
    }
}
