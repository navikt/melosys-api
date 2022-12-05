package no.nav.melosys.integrasjon.reststs;

import no.nav.melosys.integrasjon.felles.RestConsumer;

public interface RestStsClient extends RestConsumer {

    String bearerToken();
}
