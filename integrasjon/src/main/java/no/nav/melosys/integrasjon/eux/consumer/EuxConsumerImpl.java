package no.nav.melosys.integrasjon.eux.consumer;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import no.nav.melosys.integrasjon.felles.RestSTSClient;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final RestSTSClient restSTSClient;

    @Autowired
    public EuxConsumerImpl(RestTemplate euxRestTemplate, RestSTSClient restSTSClient) {
        this.euxRestTemplate = euxRestTemplate;
        this.restSTSClient = restSTSClient;
    }

    @Override
    public String opprettRinaSak(String bucType) throws TekniskException, FunksjonellException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/BuC")
                .queryParam("BuCType", bucType);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public String hentBuc(String rinaSaksnummer) throws FunksjonellException, TekniskException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/BuC")
                .queryParam("RINASaksnummer", rinaSaksnummer);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public void opprettSED(String rinaSaksnummer, String euFormat, String korrelasjonsId, Object SED) throws TekniskException, FunksjonellException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", rinaSaksnummer)
                .queryParam("EUFormat", euFormat)
                .queryParam("KorrelasjonsID", korrelasjonsId);

        exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(SED, getDefaultHeaders(false)),new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public String hentSED(String rinaSaksnummer, String dokumentId) throws FunksjonellException, TekniskException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", rinaSaksnummer)
                .queryParam("DokumentID", dokumentId);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public void oppdaterSED(String rinaSaksnummer, String korrelasjonsId, String sedType, String dokumentId, Object SED) throws FunksjonellException, TekniskException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", rinaSaksnummer)
                .queryParam("SEDType", sedType)
                .queryParam("KorrelasjonsID", korrelasjonsId);

        exchange(builder.toUriString(), HttpMethod.PUT, new HttpEntity<>(SED, getDefaultHeaders(false)),
                new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public void sendSED(String korrelasjonsId, String dokumentId) throws TekniskException, FunksjonellException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/sendSED")
                .queryParam("DokumentID", dokumentId)
                .queryParam("KorrelasjonsID", korrelasjonsId);

        exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public List<String> hentTilgjengeligeSEDTyper(String rinaSaksnummer) throws FunksjonellException, TekniskException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/TilgjengeligeSEDTyper")
                .queryParam("RINASaksnummer", rinaSaksnummer);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<List<String>>() {});
    }

    @Override
    public String opprettBuCogSED(String bucType, String fagSakNummer, String mottakerId, String filType, String korrelasjonsId) throws TekniskException, FunksjonellException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/OpprettBuCogSED")
                .queryParam("BucType", bucType)
                .queryParam("FagSakNummer", fagSakNummer)
                .queryParam("MottakerID", mottakerId)
                .queryParam("Filtype", filType)
                .queryParam("KorrelasjonsID", korrelasjonsId);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    @Override
    public List<String> bucTypePerSektor() throws FunksjonellException, TekniskException {
        return exchange("/BuCTypePerSektor", HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<List<String>>() {});
    }

    @Override
    public List<String> getInstitusjoner(String bucType, String landkode) throws TekniskException, FunksjonellException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/Institusjoner")
                .queryParam("BucType", bucType)
                .queryParam("landKode", landkode);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<List<String>>() {});
    }

    @Override
    public String leggTilVedlegg(String rinaSaksnummer, String dokumentId, String filType) throws TekniskException, FunksjonellException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/Vedlegg")
                .queryParam("RINASaksnummer", rinaSaksnummer)
                .queryParam("DokumentID", dokumentId)
                .queryParam("Filtype", filType);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity entity, ParameterizedTypeReference<T> responseType) throws FunksjonellException, TekniskException {
        try {
            ResponseEntity<T> response = euxRestTemplate.exchange(uri, method, entity, responseType);
            return response.getBody();
        } catch (RestClientException e) {
            ExceptionMapper.SpringExTilMelosysEx(e);
            return null;
        }
    }

    private HttpHeaders getDefaultHeaders(boolean systemAuth) throws FunksjonellException, TekniskException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(HttpHeaders.AUTHORIZATION, systemAuth ? getSystemOidcAuth() : getAuth());
        return headers;
    }

    // I tilfelle noe automatisering ønskes.
    // Henter token for autentisering ved systembruker, srvmelosys, da eux krever oidc
    private String getSystemOidcAuth() throws FunksjonellException, TekniskException {
        return "Bearer " + restSTSClient.collectToken();
    }

    @Override
    public boolean isSystem() {
        return false;
    }
}
