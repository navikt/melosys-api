package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.util.function.Supplier;
import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestSts;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class ArbeidsforholdContextExchangeFilter implements ExchangeFilterFunction {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";
    private final RestSts restSts;

    public ArbeidsforholdContextExchangeFilter(RestSts restSts) {
        this.restSts = restSts;
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, getTokenSupplier(restSts).get())
                .header(NAV_CONSUMER_TOKEN, restSts.bearerToken())
                .build()
        );
    }

    private Supplier<String> getTokenSupplier(RestSts restSts) {
        // Om vi får lagt inn "0000-ga-aa-register-konsument" i sakbehandler token kan vi benytte dette når tilgjengelig
        // https://nav-it.slack.com/archives/C01BSCJM127/p1649411252534409
        return restSts::bearerToken;
    }
}
