package no.nav.melosys.integrasjon.eux.consumer;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
public class EuxConsumerImpl implements EuxConsumer {

    private final RestTemplate euxRestTemplate;
    private final RestStsClient restSTSClient;

    private final String RINA_SAKSNUMMER = "RINASaksnummer";
    private final String KORRELASJONS_ID = "KorrelasjonsID";
    private final String BUC_TYPE = "BuCType";
    private final String DOKUMENT_ID = "DokumentID";

    private final String BUC_PATH = "/BUC";
    private final String SED_PATH = "/SED";

    @Autowired
    public EuxConsumerImpl(@Qualifier("euxRestTemplate") RestTemplate restTemplate, RestStsClient restSTSClient) {
        this.euxRestTemplate = restTemplate;
        this.restSTSClient = restSTSClient;
    }

    @Override
    public String opprettRinaSak(String bucType) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(BUC_PATH)
                .queryParam(BUC_TYPE, bucType);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public String hentBuc(String rinaSaksnummer) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(BUC_PATH)
                .queryParam(RINA_SAKSNUMMER, rinaSaksnummer);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public void opprettSed(String rinaSaksnummer, String euFormat, String korrelasjonsId, Object sed) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SED_PATH)
                .queryParam(RINA_SAKSNUMMER, rinaSaksnummer)
                .queryParam("EUFormat", euFormat)
                .queryParam(KORRELASJONS_ID, korrelasjonsId);

        exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(sed, getDefaultHeaders(false)),new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public String hentSed(String rinaSaksnummer, String dokumentId) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SED_PATH)
                .queryParam(RINA_SAKSNUMMER, rinaSaksnummer)
                .queryParam(DOKUMENT_ID, dokumentId);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public void oppdaterSed(String rinaSaksnummer, String korrelasjonsId, String sedType, String dokumentId, Object sed) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SED_PATH)
                .queryParam(RINA_SAKSNUMMER, rinaSaksnummer)
                .queryParam("SEDType", sedType)
                .queryParam(KORRELASJONS_ID, korrelasjonsId);

        exchange(builder.toUriString(), HttpMethod.PUT, new HttpEntity<>(sed, getDefaultHeaders(false)),
                new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public void sendSed(String korrelasjonsId, String dokumentId) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/sendSED")
                .queryParam(DOKUMENT_ID, dokumentId)
                .queryParam(KORRELASJONS_ID, korrelasjonsId);

        exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public List<String> hentTilgjengeligeSedTyper(String rinaSaksnummer) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/TilgjengeligeSEDTyper")
                .queryParam(RINA_SAKSNUMMER, rinaSaksnummer);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<List<String>>() {});
    }

    @Override
    public String opprettBucOgSed(String bucType, String fagSakNummer, String mottakerId, String filType, String korrelasjonsId) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/OpprettBuCogSED")
                .queryParam(BUC_TYPE, bucType)
                .queryParam("FagSakNummer", fagSakNummer)
                .queryParam("MottakerID", mottakerId)
                .queryParam("Filtype", filType)
                .queryParam(KORRELASJONS_ID, korrelasjonsId);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public List<String> bucTypePerSektor() throws MelosysException {
        return exchange("/BuCTypePerSektor", HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<List<String>>() {});
    }

    @Override
    public List<String> getInstitusjoner(String bucType, String landkode) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/Institusjoner")
                .queryParam(BUC_TYPE, bucType)
                .queryParam("landKode", landkode);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<List<String>>() {});
    }

    @Override
    public String leggTilVedlegg(String rinaSaksnummer, String dokumentId, String filType) throws MelosysException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/Vedlegg")
                .queryParam(RINA_SAKSNUMMER, rinaSaksnummer)
                .queryParam(DOKUMENT_ID, dokumentId)
                .queryParam("Filtype", filType);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType) throws MelosysException {
        try {
            ResponseEntity<T> response = euxRestTemplate.exchange(uri, method, entity, responseType);
            return response.getBody();
        } catch (RestClientException e) {
            throw ExceptionMapper.springExTilMelosysEx(e);
        }
    }

    private HttpHeaders getDefaultHeaders(boolean systemAuth) throws MelosysException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(HttpHeaders.AUTHORIZATION, systemAuth ? getSystemOidcAuth() : getAuth());
        return headers;
    }

    // I tilfelle noe automatisering ønskes.
    // Henter token for autentisering ved systembruker, srvmelosys, da eux krever oidc
    private String getSystemOidcAuth() throws MelosysException {
        return "Bearer " + restSTSClient.collectToken();
    }

    @Override
    public boolean isSystem() {
        return false;
    }
}
