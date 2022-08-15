package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.*;

import no.nav.melosys.integrasjon.felles.FeilResponseDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class OppgaveConsumerImpl implements OppgaveConsumer {
    // Oppgave (/Abac) kaster feil om svaret på et søk inneholder oppgaver med 50+ unike personer
    private static final int OPPGAVE_ANTALL_ABAC_LIMIT = 40;
    private static final String CORRELATION_ID = "X-Correlation-ID";

    private static final String OPPGAVE_BASE_URI = "/oppgaver";
    private static final String OPPGAVE_URI_MED_ID = OPPGAVE_BASE_URI + "/{oppgaveID}";

    private final WebClient webClient;

    public OppgaveConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public OppgaveDto hentOppgave(String oppgaveId) {
        return webClient.get()
            .uri(OPPGAVE_URI_MED_ID, oppgaveId)
            .header(CORRELATION_ID, getCallID())
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    @Override
    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) {
        return hentOppgaveListe(oppgaveSearchRequest, 0);
    }

    private List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest, int offset) {
        final OppgaveSvar oppgaveSvar = hentOppgaveSvar(oppgaveSearchRequest, offset);
        if (oppgaveSvar == null) {
            return Collections.emptyList();
        }
        List<OppgaveDto> oppgaveListe = new ArrayList<>(oppgaveSvar.getOppgaver());
        if (oppgaveSvar.getAntallTreffTotalt() > offset + OPPGAVE_ANTALL_ABAC_LIMIT) {
            oppgaveListe.addAll(hentOppgaveListe(oppgaveSearchRequest, offset + OPPGAVE_ANTALL_ABAC_LIMIT));
        }
        return oppgaveListe;
    }

    private OppgaveSvar hentOppgaveSvar(OppgaveSearchRequest oppgaveSearchRequest, int offset) {
        return webClient.get()
            .uri(OPPGAVE_BASE_URI, uriBuilder ->
                uriBuilder
                    .queryParamIfPresent("aktoerId", Optional.ofNullable(oppgaveSearchRequest.getAktørId()))
                    .queryParamIfPresent("journalpostId", Optional.ofNullable(oppgaveSearchRequest.getJournalpostId()))
                    .queryParamIfPresent("orgnr", Optional.ofNullable(oppgaveSearchRequest.getOrgnr()))
                    .queryParamIfPresent("tildeltEnhetsnr", Optional.ofNullable(oppgaveSearchRequest.getTildeltEnhetsnr()))
                    .queryParamIfPresent("tildeltRessurs", Optional.ofNullable(oppgaveSearchRequest.getTildeltRessurs()))
                    .queryParamIfPresent("sorteringsfelt", Optional.ofNullable(oppgaveSearchRequest.getSorteringsfelt()))
                    .queryParamIfPresent("sorteringsrekkefolge", Optional.ofNullable(oppgaveSearchRequest.getSorteringsrekkefolge()))
                    .queryParamIfPresent("tilordnetRessurs", Optional.ofNullable(oppgaveSearchRequest.getTilordnetRessurs()))
                    .queryParamIfPresent("statuskategori", Optional.ofNullable(oppgaveSearchRequest.getStatusKategori()))
                    .queryParamIfPresent("behandlesAvApplikasjon", Optional.ofNullable(oppgaveSearchRequest.getBehandlesAvApplikasjon()))
                    .queryParam("limit", OPPGAVE_ANTALL_ABAC_LIMIT)
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
    }

    private Optional<Collection<String>> tilOptionalListe(String[] array) {
        return array != null ? Optional.of(Arrays.stream(array).toList()) : Optional.empty();
    }

    @Override
    public OppgaveDto oppdaterOppgave(OppgaveDto request) {
        return webClient.put()
            .uri(OPPGAVE_URI_MED_ID, request.getId())
            .header(CORRELATION_ID, getCallID())
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    @Override
    public String opprettOppgave(OpprettOppgaveDto request) {
        return webClient.post()
            .uri(OPPGAVE_BASE_URI)
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
}
