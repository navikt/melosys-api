package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPut;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@CompilerMessageKey
public class MedlemskapRestConsumer implements RestConsumer {
    private static final String CONSUMER_ID = "srvmelosys";

    private final RestStsClient restStsClient;
    private final WebClient webClient;

    @Autowired
    public MedlemskapRestConsumer(@Value("${medlemskap.rest.url}") String url, RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
        this.webClient = WebClient.builder()
            .baseUrl(url)
            .filter(headerFilter())
            .build();
    }

    public List<MedlemskapsunntakForGet> hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) {

//
//        [
//        {
//            "dekning": "string",
//            "fraOgMed": "string",
//            "grunnlag": "string",
//            "helsedel": true,
//            "ident": "string",
//            "lovvalg": "string",
//            "lovvalgsland": "string",
//            "medlem": true,
//            "sporingsinformasjon": {
//            "besluttet": "string",
//                "kilde": "string",
//                "kildedokument": "string",
//                "opprettet": "2021-01-07T11:17:03.857Z",
//                "opprettetAv": "string",
//                "registrert": "string",
//                "sistEndret": "2021-01-07T11:17:03.857Z",
//                "sistEndretAv": "string",
//                "versjon": 0
//        },
//            "status": "string",
//            "statusaarsak": "string",
//            "studieinformasjon": {
//            "delstudie": true,
//                "soeknadInnvilget": true,
//                "statsborgerland": "string",
//                "studieland": "string"
//        },
//            "tilOgMed": "string",
//            "unntakId": 0
//        }
//]
        return null;
    }

    public MedlemskapsunntakForGet opprettPeriode(MedlemskapsunntakForPost request) {
        return utfør(request, HttpMethod.POST);
    }

    public MedlemskapsunntakForGet oppdaterPeriode(MedlemskapsunntakForPut request) {
        return utfør(request, HttpMethod.PUT);
    }

    private MedlemskapsunntakForGet utfør(Object request, HttpMethod method) {
        return webClient.method(method)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MedlemskapsunntakForGet.class)
            .block();
    }

    private ExchangeFilterFunction headerFilter() {
        return ((clientRequest, next) -> {
            try {
                clientRequest.headers().add(HttpHeaders.AUTHORIZATION, "Bearer " + restStsClient.collectToken());
                clientRequest.headers().add("Nav-Call-Id", getCallID());
                clientRequest.headers().add("Nav-Consumer-Id", CONSUMER_ID);
            } catch (MelosysException e) {
                e.printStackTrace();
            }
            return next.exchange(clientRequest);
        });
    }
}
