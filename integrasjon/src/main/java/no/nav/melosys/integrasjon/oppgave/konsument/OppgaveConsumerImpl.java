package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.felles.FeilResponseDto;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSvar;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class OppgaveConsumerImpl implements OppgaveConsumer, RestConsumer {

    private static final int OPPGAVE_ANTALL_LIMIT = 50;
    private static final String CORRELATION_ID = "X-Correlation-ID";

    private final WebClient webClient;

    public OppgaveConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public OppgaveDto hentOppgave(String oppgaveId) {
        return webClient.get()
            .uri(uri -> uri.path("/{oppgaveID}").build(oppgaveId))
            .header(CORRELATION_ID, getCallID())
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    @Override
    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) throws IntegrasjonException {
        return hentOppgaveListe(oppgaveSearchRequest, 0);
    }

    private List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest, int offset) throws IntegrasjonException {
        OppgaveSvar svar = webClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .queryParamIfPresent("tildeltEnhetsnr", Optional.ofNullable(oppgaveSearchRequest.getTildeltEnhetsnr()))
                    .queryParamIfPresent("tildeltRessurs", Optional.ofNullable(oppgaveSearchRequest.getTildeltRessurs()))
                    .queryParamIfPresent("sorteringsfelt", Optional.ofNullable(oppgaveSearchRequest.getSorteringsfelt()))
                    .queryParamIfPresent("tilordnetRessurs", Optional.ofNullable(oppgaveSearchRequest.getTilordnetRessurs()))
                    .queryParamIfPresent("statuskategori", Optional.ofNullable(oppgaveSearchRequest.getStatusKategori()))
                    .queryParamIfPresent("behandlesAvApplikasjon", Optional.ofNullable(oppgaveSearchRequest.getBehandlesAvApplikasjon()))
                    .queryParam("limit", OPPGAVE_ANTALL_LIMIT)
                    .queryParam("offset", offset)
                    .queryParamIfPresent("behandlingstype", Optional.ofNullable(oppgaveSearchRequest.getBehandlingstype()))
                    .queryParamIfPresent("behandlingstema", Optional.ofNullable(oppgaveSearchRequest.getBehandlingstema()))
                    .queryParamIfPresent("oppgavetype", tilOptionalListe(oppgaveSearchRequest.getOppgavetype()))
                    .queryParamIfPresent("saksreferanse", tilOptionalListe(oppgaveSearchRequest.getSaksreferanse()))
                    .queryParamIfPresent("tema", tilOptionalListe(oppgaveSearchRequest.getTema()))
                    .build()
            ).header(CORRELATION_ID, getCallID())
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveSvar.class)
            .block();

        if (svar == null) {
            throw new IntegrasjonException("Feil i integrasjon mot Oppgave");
        }

        List<OppgaveDto> oppgaveListe = new ArrayList<>(svar.getOppgaver());
        if (svar.getAntallTreffTotalt() > offset + OPPGAVE_ANTALL_LIMIT) {
            oppgaveListe.addAll(hentOppgaveListe(oppgaveSearchRequest, offset + OPPGAVE_ANTALL_LIMIT));
        }
        return oppgaveListe;
    }

    private Optional<Collection<String>> tilOptionalListe(String[] array) {
        return array != null ? Optional.of(Arrays.stream(array).collect(Collectors.toList())) : Optional.empty();
    }

    @Override
    public OppgaveDto oppdaterOppgave(OppgaveDto request) throws FunksjonellException, TekniskException {
        return webClient.put()
            .uri(uriBuilder -> uriBuilder.path("/{oppgaveID}").build(request.getId()))
            .header(CORRELATION_ID, getCallID())
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    @Override
    public String opprettOppgave(OpprettOppgaveDto request) throws FunksjonellException, TekniskException {
        return webClient.post()
            .header(CORRELATION_ID, getCallID())
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .map(OppgaveDto::getId)
            .block();
    }

    private Mono<Exception> håndterFeil(ClientResponse clientResponse) {
        final HttpStatus status = clientResponse.statusCode();
        return clientResponse.bodyToMono(FeilResponseDto.class)
            .map(FeilResponseDto::getFeilmelding)
            .map(feilmelding -> tilException(feilmelding, status));
    }

    private MelosysException tilException(String feilmelding, HttpStatus status) {
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            return new SikkerhetsbegrensningException(feilmelding);
        } else if (status == HttpStatus.NOT_FOUND) {
            return new IkkeFunnetException(feilmelding);
        } else if (status.is4xxClientError()) {
            return new FunksjonellException(feilmelding);
        } else { // 5xx
            return new TekniskException(feilmelding);
        }
    }
}
