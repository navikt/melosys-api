package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.felles.FeilResponseDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSvar;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class OppgaveConsumerImpl implements OppgaveConsumer {

    private static final int OPPGAVE_ANTALL_LIMIT = 100;

    private final WebClient webClient;

    public OppgaveConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public OppgaveDto hentOppgave(String oppgaveId) {
        return webClient.get()
            .uri(uri -> uri.path("/{oppgaveID}").build(oppgaveId))
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    @Override
    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) {
        return webClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .queryParamIfPresent("tildeltEnhetsnr", Optional.ofNullable(oppgaveSearchRequest.getTildeltEnhetsnr()))
                    .queryParamIfPresent("tildeltRessurs", Optional.ofNullable(oppgaveSearchRequest.getTildeltRessurs()))
                    .queryParamIfPresent("sorteringsfelt", Optional.ofNullable(oppgaveSearchRequest.getSorteringsfelt()))
                    .queryParamIfPresent("tilordnetRessurs", Optional.ofNullable(oppgaveSearchRequest.getTilordnetRessurs()))
                    .queryParamIfPresent("statuskategori", Optional.ofNullable(oppgaveSearchRequest.getStatusKategori()))
                    .queryParamIfPresent("behandlesAvApplikasjon", Optional.ofNullable(oppgaveSearchRequest.getBehandlesAvApplikasjon()))
                    .queryParam("limit", OPPGAVE_ANTALL_LIMIT)
                    .queryParamIfPresent("behandlingstype", Optional.ofNullable(oppgaveSearchRequest.getBehandlingstype()))
                    .queryParamIfPresent("behandlingstema", Optional.ofNullable(oppgaveSearchRequest.getBehandlingstema()))
                    .queryParamIfPresent("oppgavetype", arrayTilQueryParam(oppgaveSearchRequest.getOppgavetype()))
                    .queryParamIfPresent("saksreferanse", arrayTilQueryParam(oppgaveSearchRequest.getSaksreferanse()))
                    .queryParamIfPresent("tema", arrayTilQueryParam(oppgaveSearchRequest.getTema()))
                    .build()
            ).retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveSvar.class)
            .map(OppgaveSvar::getOppgaver)
            .block();
    }

    private Optional<String> arrayTilQueryParam(String[] array) {
        return array != null ? Optional.of(String.join(",", array)) : Optional.empty();
    }

    @Override
    public void oppdaterOppgave(OppgaveDto request) throws FunksjonellException, TekniskException {
        webClient.put()
            .uri(uriBuilder -> uriBuilder.path("/{oppgaveID}").build(request.getId()))
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .toBodilessEntity()
            .block();
    }

    @Override
    public String opprettOppgave(OpprettOppgaveDto request) throws FunksjonellException, TekniskException {
        return webClient.post()
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .blockOptional()
            .map(OppgaveDto::getId)
            .orElseThrow(() -> new IllegalStateException("Feil"));
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
