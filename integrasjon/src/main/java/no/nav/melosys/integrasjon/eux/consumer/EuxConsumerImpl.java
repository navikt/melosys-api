package no.nav.melosys.integrasjon.eux.consumer;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.eux.model.nav.SED;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EuxConsumerImpl implements EuxConsumer {

    private static final Logger log = LoggerFactory.getLogger(EuxConsumerImpl.class);

    private final RestTemplate euxRestTemplate;
    private final RestStsClient restSTSClient;

    private final String RINA_SAKSNUMMER = "RINASaksnummer";
    private final String KORRELASJONS_ID = "KorrelasjonsId";
    private final String BUC_TYPE = "BuCType";

    private final String BUC_PATH = "/buc/%s";
    private final String SED_PATH = "/buc/%s/sed/%s";
    private final String VEDLEGG_PATH = "/buc/%s/sed/%s/vedlegg";
    private final String BUCDELTAKERE_PATH = "/buc/%s/bucdeltakere";
    private final String DOKUMENTMAL_PATH = "/buc/%s/dokumentmal";
    private final String MULIGEAKSJONER_PATH = "/buc/%s/muligeaksjoner";

    @Autowired
    public EuxConsumerImpl(@Qualifier("euxRestTemplate") RestTemplate restTemplate, RestStsClient restSTSClient) {
        this.euxRestTemplate = restTemplate;
        this.restSTSClient = restSTSClient;
    }

    /**
     * Henter ut eksisterende BuC
     * @param rinaSaksnummer Saksnummer til BuC
     * @return JsonNode klasse. Selve returverdien er et svært komplisert objekt,
     * derfor er ikke det spesifikke objektet spesifisert
     * @throws MelosysException
     */
    @Override
    public JsonNode hentBuC(String rinaSaksnummer) throws MelosysException {

        log.info("Henter buc: {}", rinaSaksnummer);
        String uri = String.format(BUC_PATH, rinaSaksnummer);

        return exchange(uri, HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<JsonNode>() {});
    }

    /**
     * Oppretter ny BuC/RINA-sak
     * @param bucType Type BuC. Eks. LA_BUC_04
     * @return saksnummer til nye sak som er opprettet
     * @throws MelosysException
     */
    @Override
    public String opprettBuC(String bucType) throws MelosysException {
        log.info("Oppretter buc, type: {}", bucType);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/buc")
                .queryParam(BUC_TYPE, bucType);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
                new ParameterizedTypeReference<String>() {});
    }

    /**
     * Sletter en BuC/Rina-sak
     * @param rinaSaksnummer saksnummer til BuC'en
     * @throws MelosysException
     */
    @Override
    public void slettBuC(String rinaSaksnummer) throws MelosysException {
        log.info("Sletter buc: {}", rinaSaksnummer);
        String uri = String.format(BUC_PATH, rinaSaksnummer);

        exchange(uri, HttpMethod.DELETE, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }

    /**
     * Setter mottaker på en BuC/Rina-sak
     * @param rinaSaksnummer saksnummer
     * @param mottakerId id på mottakende enhet
     * @throws MelosysException
     */
    @Override
    public void settMottaker(String rinaSaksnummer, String mottakerId) throws MelosysException {

        log.info("Setter mottaker {} til sak {}", mottakerId, rinaSaksnummer);
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromPath(String.format(BUCDELTAKERE_PATH, rinaSaksnummer))
            .queryParam("MottakerId", mottakerId);

        exchange(builder.toUriString(), HttpMethod.PUT, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }

    /**
     * Henter deltagere i saken
     * @param rinaSaksnummer
     * @return liste over deltagere
     * @throws MelosysException
     */
    @Override
    public List<String> hentDeltagere(String rinaSaksnummer) throws MelosysException {

        log.info("Henter deltakere til sak {}", rinaSaksnummer);
        String uri = String.format(BUCDELTAKERE_PATH, rinaSaksnummer);

        return exchange(uri, HttpMethod.PUT, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * Henter ut en liste over mulige aksjoner
     * @param rinaSaksnummer
     * @return liste over mulige aksjoner på en rina-sak
     * @throws MelosysException
     */
    @Override
    public JsonNode hentMuligeAksjoner(String rinaSaksnummer) throws MelosysException {
        log.info("Henter mulige aksjoner for sak {}", rinaSaksnummer);
        String uri = String.format(MULIGEAKSJONER_PATH, rinaSaksnummer);

        return exchange(uri, HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<JsonNode>() {});
    }

    /**
     * Oppretter en SED på en eksisterende BuC
     * @param rinaSaksnummer saksnummer til BuC/rina-saken
     * @param korrelasjonsId
     * @param sed SED'en som skal legges til rina-saken
     * @return dokumentId' til SED'en
     * @throws MelosysException
     */
    @Override
    public String opprettSed(String rinaSaksnummer, String korrelasjonsId, SED sed) throws MelosysException {

        log.info("Oppretter SED {} på sak {}", sed.getSed(), rinaSaksnummer);
        String uri = UriComponentsBuilder.fromPath(String.format("/buc/%s/sed", rinaSaksnummer))
            .queryParam(KORRELASJONS_ID, korrelasjonsId).toUriString();

        return exchange(uri, HttpMethod.POST, new HttpEntity<>(sed, getDefaultHeaders(false)),
            new ParameterizedTypeReference<String>() {});
    }

    /**
     * Henter ut en eksisterende SED
     * @param rinaSaksnummer saksnummeret hvor SED'en er tilknyttet
     * @param dokumentId id' til SED'en som skal hentes
     * @return
     */
    @Override
    public SED hentSed(String rinaSaksnummer, String dokumentId) throws MelosysException {
        log.info("Henter sed med id {}, fra sak {}", dokumentId, rinaSaksnummer);
        String uri = String.format(SED_PATH, rinaSaksnummer, dokumentId);

        return exchange(uri, HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<SED>() {});
    }

    /**
     * Oppdaterer en eksisterende SED
     * @param rinaSaksnummer saksnummeret
     * @param korrelasjonsId Optional, brukes ikke av eux per nå
     * @param dokumentId Id'en til SED'en som skal oppdateres
     * @param sed Den nye versjonen av SED'en
     * @throws MelosysException
     */
    @Override
    public void oppdaterSed(String rinaSaksnummer, String korrelasjonsId, String dokumentId, SED sed) throws MelosysException {
        log.info("Oppdaterer sed {} på sak {}", dokumentId, rinaSaksnummer);
        String uri = UriComponentsBuilder.fromPath(String.format(SED_PATH, rinaSaksnummer, dokumentId))
            .queryParam(KORRELASJONS_ID, korrelasjonsId).toUriString();


        exchange(uri, HttpMethod.PUT, new HttpEntity<>(sed, getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }

    /**
     * Sletter en eksisterende SED
     * @param rinaSaksnummer saksnummeret
     * @param dokumentId ID til SED som skal slettes
     * @throws MelosysException
     */
    @Override
    public void slettSed(String rinaSaksnummer, String dokumentId) throws MelosysException {
        log.info("Sletter sed {} på sak {}", dokumentId, rinaSaksnummer);
        String uri = String.format(SED_PATH, rinaSaksnummer, dokumentId);

        exchange(uri, HttpMethod.DELETE, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }


    /**
     * Sender en SED til mottakkere. Mottakere må være satt før den kan sendes.
     * @param rinaSaksnummer saksnummeret
     * @param dokumentId id' til SED som skal sendes
     * @param korrelasjonsId optional, ikke brukt av eux per nå
     * @throws MelosysException
     */
    @Override
    public void sendSed(String rinaSaksnummer, String korrelasjonsId, String dokumentId) throws MelosysException {
        log.info("Sender sed {} fra sak {}", dokumentId, rinaSaksnummer);
        String uri = UriComponentsBuilder.fromPath(String.format(SED_PATH, rinaSaksnummer, dokumentId) + "/send")
            .queryParam(KORRELASJONS_ID, korrelasjonsId).toUriString();

        exchange(uri, HttpMethod.POST, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }

    /**
     * Legger til et vedlegg for et dokument
     * @param rinaSaksnummer saksnummeret
     * @param dokumentId id til SED'en vedlegget skal legges til
     * @param filType filtype
     * @param vedlegg Selve vedlegget som skal legges til
     * @return ukjent
     * @throws MelosysException
     */
    @Override
    public String leggTilVedlegg(String rinaSaksnummer, String dokumentId, String filType, Object vedlegg) throws MelosysException {
        log.info("Legger til vedlegg på sak {} og dokument {}", rinaSaksnummer, dokumentId);
        String uri = UriComponentsBuilder.fromPath(String.format(VEDLEGG_PATH, rinaSaksnummer, dokumentId))
            .queryParam("Filtype", filType).toUriString();

        return exchange(uri, HttpMethod.POST, new HttpEntity<>(vedlegg, getDefaultHeaders(false)),
            new ParameterizedTypeReference<String>() {});
    }

    /**
     * Henter et vedlegg tilhørende sak og dokument
     * @param rinaSaksnummer saksnummeret
     * @param dokumentId id til SED'en
     * @param vedleggId id til vedlegget
     * @return
     * @throws MelosysException
     */
    @Override
    public byte[] hentVedlegg(String rinaSaksnummer, String dokumentId, String vedleggId) throws MelosysException {
        log.info("Henter vedlegg for sak {} og dokument {}", rinaSaksnummer, dokumentId);
        String uri = String.format(VEDLEGG_PATH, rinaSaksnummer, dokumentId) + "/" + vedleggId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        headers.add(HttpHeaders.AUTHORIZATION, getAuth()); //TODO; denne må endres om det viser seg at det trengs automatisering på dette steget - gitt at eux støtter dette da

        return exchange(uri, HttpMethod.GET, new HttpEntity<>(headers),
            new ParameterizedTypeReference<byte[]>() {});
    }

    /**
     * Sletter et eksisterende vedlegg tilhørende et dokument(sed)
     * @param rinaSaksnummer saksnummeret
     * @param dokumentId id til sed'en
     * @param vedleggId id til vedlegget
     * @return
     * @throws MelosysException
     */
    @Override
    public void slettVedlegg(String rinaSaksnummer, String dokumentId, String vedleggId) throws MelosysException {
        log.info("Sletter vedlegg {} på sak {} og dokument {}", vedleggId, rinaSaksnummer, dokumentId);
        String uri = String.format(VEDLEGG_PATH, rinaSaksnummer, dokumentId) + "/" + vedleggId;

        exchange(uri, HttpMethod.DELETE, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }

    /**
     * Henter liste av alle SED-typer som kan opprettes i sakens nåværende tilstand
     * @param rinaSaksnummer saksnummeret
     * @return liste av SED-typer
     * @throws MelosysException
     */
    @Override
    public List<String> hentTilgjengeligeSedTyper(String rinaSaksnummer) throws MelosysException {
        log.info("Henter tilgjenglige sed-typer for sak {}", rinaSaksnummer);
        String uri = String.format(BUC_PATH, rinaSaksnummer) + "/sedtyper";

        return exchange(uri, HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * Setter en sak sensitiv
     * @param rinaSaksnummer saksnummeret
     * @throws MelosysException
     */
    @Override
    public void setSakSensitiv(String rinaSaksnummer) throws MelosysException {
        log.info("Setter sak {} sensitiv", rinaSaksnummer);
        String uri = String.format(BUC_PATH, rinaSaksnummer) + "/sensitivsak";

        exchange(uri, HttpMethod.PUT, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }
    /**
     * Fjerner 'sensitiv sak' markør på saken
     * @param rinaSaksnummer saksnummeret
     * @throws MelosysException
     */
    @Override
    public void fjernSensitivPaaSak(String rinaSaksnummer) throws MelosysException {
        log.info("Fjerner 'sensitiv sak' på sak {}", rinaSaksnummer);
        String uri = String.format(BUC_PATH, rinaSaksnummer) + "/sensitivsak";

        exchange(uri, HttpMethod.DELETE, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<Void>() {});
    }

    /**
     * Oppretter en BuC med en tilhørende SED
     * @param bucType Hvilken type buc som skal opprettes. Eks LA_BUC_04
     * @param mottakerId Mottaker sin Rina-id
     * @return id til rina-sak som ble opprettet
     * @throws MelosysException
     */
    @Override
    public String opprettBucOgSed(String bucType, String mottakerId, SED sed) throws MelosysException {
        log.info("Oppretter buc {}, med sed {}, med mottaker {}", bucType, sed.getSed(), mottakerId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/buc/sed")
            .queryParam("BucType", bucType)
            .queryParam("MottakerId", mottakerId);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(sed, getDefaultHeaders(false)),
            new ParameterizedTypeReference<String>() {});
    }

    /**
     * Oppretter en BuC med en tilhørende SED og evt vedlegg
     * @param bucType Hvilken type buc som skal opprettes. Eks LA_BUC_04
     * @param fagSakNummer Optional da eux per 17.01: unknown.. brukes ikke av eux,
     * @param mottakerId Mottaker sin Rina-id
     * @param filType filtype til vedlegg
     * @param korrelasjonsId Optional, ikke brukt av eux per nå
     * @param sed sed'en som skal opprettes
     * @param vedlegg vedlegget som skal legges til saken
     * @return id til rina-sak som ble opprettet
     * @throws MelosysException
     */
    @Override
    public String opprettBucOgSedMedVedlegg(String bucType, String fagSakNummer, String mottakerId, String filType, String korrelasjonsId, SED sed, Object vedlegg) throws MelosysException {
        log.info("Oppretter buc {}, med sed {}, med mottaker {} og legger til vedlegg", bucType, sed.getSed(), mottakerId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/buc/sed/vedlegg")
            .queryParam(BUC_TYPE, bucType)
            .queryParam("FagSakNummer", fagSakNummer)
            .queryParam("MottakerID", mottakerId)
            .queryParam("FilType", filType)
            .queryParam(KORRELASJONS_ID, korrelasjonsId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(HttpHeaders.AUTHORIZATION, getAuth());

        byte[] documentBytes, attachmentBytes;
        try {
            documentBytes = new ObjectMapper().writeValueAsBytes(sed);
            attachmentBytes = new ObjectMapper().writeValueAsBytes(vedlegg);
        } catch (JsonProcessingException ex) {
            throw new TekniskException("Feil ved mapping fra sed til bytes");
        }

        ByteArrayResource document = new ByteArrayResource(documentBytes) {
            @Override
            public String getFilename() {
                return "document";
            }
        };
        ByteArrayResource attachment = new ByteArrayResource(attachmentBytes) {
            @Override
            public String getFilename() {
                return "attachment";
            }
        };

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("document", document);
        map.add("attachment", attachment);

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(map, headers),
            new ParameterizedTypeReference<String>() {});
    }

    /**
     * Henter en liste over mulige BuC'er den påloggede bruker kan opprette
     * @return liste av BuC'er
     * @throws MelosysException
     */
    @Override
    public List<String> bucTypePerSektor() throws MelosysException {
        log.info("Henter buctyper per sektor");
        return exchange("/buctypepersektor", HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * Henter en liste over registrerte institusjoner innenfor spesifiserte EU-land
     * @param bucType BuC/Rina-saksnummer
     * @param landkode kode til landet det skal hente institusjoner fra
     * @return
     * @throws MelosysException
     */
    @Override
    public List<String> hentInstitusjoner(String bucType, String landkode) throws MelosysException {
        log.info("Henter institusjoner for buctype {} og landkode", bucType, landkode);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/institusjoner")
            .queryParam(BUC_TYPE, bucType)
            .queryParam("LandKode", landkode);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * Henter ut hele eller deler av kodeverket
     * @param kodeverk hvilket kodeverk som skal hentes ut. Optional
     * @return Det spesifiserte kodeverket, eller hele kodeverket om ikke spesifisert
     * @throws MelosysException
     */
    @Override
    public JsonNode hentKodeverk(String kodeverk) throws MelosysException {
        log.info("Henter kodeverk {}", kodeverk);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/kodeverk")
            .queryParam("Kodeverk", kodeverk);

        return exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<JsonNode>() {});
    }

    /**
     * Søker etter rina-saker etter gitte parametere. Alle parametere er optional
     * @param fnr fødselsnummer
     * @param fornavn fornavn
     * @param etternavn etternavn
     * @param fødselsdato fødselsdato
     * @param rinaSaksnummer rinaSaksnummer
     * @param bucType bucType
     * @param status status
     * @return JsonNode med rina saker
     * @throws MelosysException
     */
    @Override
    public JsonNode finnRinaSaker(String fnr, String fornavn, String etternavn, String fødselsdato, String rinaSaksnummer, String bucType, String status) throws MelosysException {
        log.info("Søker etter rina-saker");
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/rinasaker")
            .queryParam("Fødselsnummer", fnr)
            .queryParam("Fornavn", fornavn)
            .queryParam("Etternavn", etternavn)
            .queryParam("Fødselsdato", fødselsdato)
            .queryParam(RINA_SAKSNUMMER, rinaSaksnummer)
            .queryParam("BuCType", bucType)
            .queryParam("Status", status);

        //Må vurdere å endre returverdi til en POJO om denne integrasjonen faktisk tas i bruk
        return exchange(builder.build(false).toUriString(), HttpMethod.GET, new HttpEntity<>(getDefaultHeaders(false)),
            new ParameterizedTypeReference<JsonNode>() {});
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType) throws MelosysException {
        try {
            return euxRestTemplate.exchange(uri, method, entity, responseType).getBody();
        } catch (RestClientException e) {
            throw ExceptionMapper.springExTilMelosysEx(e);
        }
    }

    private HttpHeaders getDefaultHeaders(boolean systemAuth) throws MelosysException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
