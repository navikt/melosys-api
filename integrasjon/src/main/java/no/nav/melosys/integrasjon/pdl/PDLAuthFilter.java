package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;

@Component
public class PDLAuthFilter extends GenericContextExchangeFilter {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    public PDLAuthFilter(RestStsClient restStsClient) {
        super(restStsClient);
    }

    @Override
    protected ClientRequest.Builder withClientRequestBuilder(ClientRequest.Builder clientRequestBuilder) {
        clientRequestBuilder.header(NAV_CONSUMER_TOKEN, getSystemToken());
        return super.withClientRequestBuilder(clientRequestBuilder);
    }
}
