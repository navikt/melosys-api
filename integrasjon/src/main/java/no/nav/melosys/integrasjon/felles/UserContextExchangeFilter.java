package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class UserContextExchangeFilter implements ExchangeFilterFunction {

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        String oidcTokenString = SubjectHandler.getInstance().getOidcTokenString();
        if (oidcTokenString == null) {
            throw new TekniskException("Token mangler! Dette kommer mest sansynlig av at en service ment for frontend kalles fra backend");
        }
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oidcTokenString)
                .build()
        );
    }
}
