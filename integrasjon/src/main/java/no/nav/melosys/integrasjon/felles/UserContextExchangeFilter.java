package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class UserContextExchangeFilter implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(UserContextExchangeFilter.class);

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        ThreadLocalAccessInfo.fromContextExchangeFilter(clientRequest.url().toString());
        if (ThreadLocalAccessInfo.isProcessCall()) { // Debug only
            ThreadLocalAccessInfo.warnProcessCall(clientRequest.url().toString());
            log.warn("Blir kalt fra prosess\n{}", ThreadLocalAccessInfo.getInfo());
        }

        String oidcTokenString = SubjectHandler.getInstance().getOidcTokenString();
        if (oidcTokenString == null) {
            throw new TekniskException("Token mangler! Dette kommer mest sannsynlig av at en service ment for frontend kalles fra en backend-prosess");
        }
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oidcTokenString)
                .build()
        );
    }
}
