package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;

public interface UtbetalingConsumer {

    HentUtbetalingsinformasjonResponse hentUtbetalingsinformasjon(HentUtbetalingsinformasjonRequest request)
        throws HentUtbetalingsinformasjonPersonIkkeFunnet, HentUtbetalingsinformasjonPeriodeIkkeGyldig, HentUtbetalingsinformasjonIkkeTilgang;
}
