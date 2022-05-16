package no.nav.melosys.integrasjon.felles;

import java.io.IOException;
import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestSts;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class SystemContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    private final RestSts restSts;

    public SystemContextClientRequestInterceptor(RestSts restSts) {
        this.restSts = restSts;
    }

    @Override
    @Nonnull
    public ClientHttpResponse intercept(@Nonnull final HttpRequest request,
                                        @Nonnull final byte[] body,
                                        @Nonnull final ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + restSts.collectToken());
        return execution.execute(request, body);
    }
}
