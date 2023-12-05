package no.nav.melosys.integrasjon.felles;

import java.io.IOException;
import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestSTSService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class SystemContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    private final RestSTSService restSTSService;

    public SystemContextClientRequestInterceptor(RestSTSService restSTSService) {
        this.restSTSService = restSTSService;
    }

    @Override
    @Nonnull
    public ClientHttpResponse intercept(@Nonnull final HttpRequest request,
                                        @Nonnull final byte[] body,
                                        @Nonnull final ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, restSTSService.bearerToken());
        return execution.execute(request, body);
    }
}
