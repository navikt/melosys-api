package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class RestSTSClient implements RestConsumer {

    private static final Long EXPIRE_TIME_TO_REFRESH = 60L;

    private volatile LocalDateTime expiryTime = LocalDateTime.now();

    private String token;

    private RestTemplate restTemplate;

    @Autowired
    public RestSTSClient(@Value("REST_STS.url") String url) {
        restTemplate = new RestTemplateBuilder()
                .rootUri(url)
                .build();
    }

    public synchronized String collectToken() throws FunksjonellException, TekniskException {
        if (shouldCollectNewToken()) {
            token = generateToken();
        }

        return token;
    }

    private String generateToken() throws FunksjonellException, TekniskException {

        try {
            ResponseEntity<Token> response = restTemplate.exchange(createUriString(), HttpMethod.GET, createHttpEntity(), Token.class);

            Token token = response.getBody();
            setExpiryTime(token);

            return token.access_token;

        } catch (HttpStatusCodeException ex) {
            ExceptionMapper.SpringExTilMelosysEx(ex);
            return null;
        } catch (Exception ex) {
            throw new TekniskException("Ukjent feil ved henting av OIDC-token fra STS", ex);
        }
    }

    private boolean shouldCollectNewToken() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    private void setExpiryTime(Token token) {
        this.expiryTime = LocalDateTime.now().plus(Duration.ofSeconds(token.expires_in - EXPIRE_TIME_TO_REFRESH));
    }

    private String createUriString() {
        return UriComponentsBuilder.newInstance()
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid").toUriString();
    }

    private HttpEntity createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, getAuth());

        return new HttpEntity(headers);
    }

    @Override
    public boolean isSystem() {
        return true;
    }

    private class Token {
        private String access_token;

        private String token_type;

        private Long expires_in;
    }
}
