package no.nav.melosys.integrasjon.felles;

import java.io.IOException;

import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class SystemContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    private final RestStsClient restStsClient;

    public SystemContextClientRequestInterceptor(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Override
    @Nonnull
    public ClientHttpResponse intercept(@Nonnull final HttpRequest request,
                                        @Nonnull final byte[] body,
                                        @Nonnull final ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + restStsClient.collectToken());
        return execution.execute(request, body);
    }
}
