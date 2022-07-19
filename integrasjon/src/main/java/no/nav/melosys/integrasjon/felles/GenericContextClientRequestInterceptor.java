package no.nav.melosys.integrasjon.felles;

import java.io.IOException;
import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class GenericContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    private final RestStsClient restStsClient;

    public GenericContextClientRequestInterceptor(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Override
    @Nonnull
    public ClientHttpResponse intercept(
        @Nonnull HttpRequest request,
        @Nonnull byte[] body,
        @Nonnull ClientHttpRequestExecution execution) throws IOException {

        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + restStsClient.collectToken());
            return execution.execute(request, body);
        }

        String oidcToken = SubjectHandler.getInstance().getOidcTokenString();
        if (ObjectUtils.isEmpty(oidcToken)) {
            throw new IllegalStateException("Finner ingen bruker-kontekst");
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + oidcToken);
        return execution.execute(request, body);
    }
}
