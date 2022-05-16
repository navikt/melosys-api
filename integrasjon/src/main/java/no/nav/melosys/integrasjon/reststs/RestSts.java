package no.nav.melosys.integrasjon.reststs;

import no.nav.melosys.integrasjon.felles.RestConsumer;

public interface RestSts extends RestConsumer {

    String bearerToken();

    String collectToken();
}
