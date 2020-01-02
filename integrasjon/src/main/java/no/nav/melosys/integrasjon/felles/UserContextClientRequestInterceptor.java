package no.nav.melosys.integrasjon.felles;

import java.io.IOException;

import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String oidcToken = SpringSubjectHandler.getInstance().getOidcTokenString();
        if (StringUtils.isEmpty(oidcToken)) {
            throw new IllegalStateException("Finner ingen bruker-kontekst");
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + oidcToken);
        return execution.execute(request, body);
    }
}
