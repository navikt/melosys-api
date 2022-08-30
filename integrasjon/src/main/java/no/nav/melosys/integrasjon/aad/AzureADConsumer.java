package no.nav.melosys.integrasjon.aad;

import no.nav.melosys.integrasjon.felles.RestConsumer;

public interface AzureADConsumer extends RestConsumer {
    public String hentToken(String tidligereToken, String api);
}
