package no.nav.melosys.integrasjon.felles;

import java.io.IOException;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class SystemContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(SystemContextClientRequestInterceptor.class);

    private final RestStsClient restStsClient;

    public SystemContextClientRequestInterceptor(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        try {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + restStsClient.collectToken());
        } catch (MelosysException e) {
            logger.error("Kunne ikke hente oidc-token fra sts", e);
        }

        return execution.execute(request, body);
    }
}
