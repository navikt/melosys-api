package no.nav.melosys.integrasjon.felles;

import java.io.IOException;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class OidcTokenClientRequestInterceptor implements ClientHttpRequestInterceptor {

    private final RestStsClient restStsClient;

    public OidcTokenClientRequestInterceptor(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        String token;
        try {
            token = restStsClient.collectToken();
        } catch (MelosysException e) {
            throw new RuntimeException("Kunne ikke hente oidc-token fra sts", e);
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return execution.execute(request, body);
    }
}
