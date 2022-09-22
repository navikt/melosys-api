package no.nav.melosys.integrasjon.pdl;

import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;

@Component
public class PDLAuthFilter extends GenericContextExchangeFilter {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    public PDLAuthFilter(RestStsClient restStsClient) {
        super(restStsClient);
    }

    @Nonnull
    @Override
    protected ClientRequest createClientRequest(@Nonnull ClientRequest clientRequest) {
        return ClientRequest.from(clientRequest)
            .header(HttpHeaders.AUTHORIZATION, getAutoToken())
            .header(NAV_CONSUMER_TOKEN, getSystemToken())
            .build();
    }
}
