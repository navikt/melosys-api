package no.nav.melosys.integrasjon.felles;

import java.io.IOException;
import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.aad.AzureADConsumerImpl;
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

    private final AzureADConsumerImpl azureADConsumer;


    public GenericContextClientRequestInterceptor(RestStsClient restStsClient,
                                                  AzureADConsumerImpl azureADConsumer) {
        this.restStsClient = restStsClient;
        this.azureADConsumer = azureADConsumer;
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
        //TODO: SER UT SOM DET BARE ER EESSI SOM BRUKER DETTE

        String oidcToken = SubjectHandler.getInstance().getOidcTokenString();

        String scope = "api://dev-fss.teammelosys.melosys-eessi-q1/.default";
        String issuedToken = azureADConsumer.hentToken(oidcToken, scope);

        System.out.println("Kaller på eessi (?). Scope for dette er: \"" + scope + "\"");

        if (ObjectUtils.isEmpty(oidcToken)) {
            throw new IllegalStateException("Finner ingen bruker-kontekst");
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + issuedToken);
        return execution.execute(request, body);
    }
}
