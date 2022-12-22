package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.*;

import no.nav.melosys.integrasjon.felles.FeilResponseDto;
import no.nav.melosys.integrasjon.felles.RestErrorHandler;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSvar;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Retryable
public class OppgaveConsumer extends RestErrorHandler {
    // Oppgave (på grunn av Abac) kaster feil om svaret på et søk inneholder oppgaver med 50+ unike personer
    private static final int OPPGAVE_ABAC_ANTALL_LIMIT = 40;
    private static final int OPPGAVE_ANTALL_LIMIT = 400;
    private static final String OPPGAVE_BASE_URI = "/oppgaver";
    private static final String OPPGAVE_URI_MED_ID = OPPGAVE_BASE_URI + "/{oppgaveID}";

    private final WebClient webClient;

    public OppgaveConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public OppgaveDto hentOppgave(String oppgaveId) {
        return webClient.get()
            .uri(OPPGAVE_URI_MED_ID, oppgaveId)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) {
        return hentOppgaveListe(oppgaveSearchRequest, 0);
    }

    private List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest, int offset) {
        final OppgaveSvar oppgaveSvar = hentOppgaveSvar(oppgaveSearchRequest, offset);
        if (oppgaveSvar == null) {
            return Collections.emptyList();
        }
        List<OppgaveDto> oppgaveListe = new ArrayList<>(oppgaveSvar.getOppgaver());
        if (offset <= OPPGAVE_ANTALL_LIMIT && oppgaveSvar.getAntallTreffTotalt() > offset + OPPGAVE_ABAC_ANTALL_LIMIT) {
            oppgaveListe.addAll(hentOppgaveListe(oppgaveSearchRequest, offset + OPPGAVE_ABAC_ANTALL_LIMIT));
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
                    .queryParam("limit", OPPGAVE_ABAC_ANTALL_LIMIT)
                    .queryParam("offset", offset)
                    .queryParamIfPresent("behandlingstype", Optional.ofNullable(oppgaveSearchRequest.getBehandlingstype()))
                    .queryParamIfPresent("behandlingstema", Optional.ofNullable(oppgaveSearchRequest.getBehandlingstema()))
                    .queryParamIfPresent("oppgavetype", tilOptionalListe(oppgaveSearchRequest.getOppgavetype()))
                    .queryParamIfPresent("saksreferanse", tilOptionalListe(oppgaveSearchRequest.getSaksreferanse()))
                    .queryParamIfPresent("tema", tilOptionalListe(oppgaveSearchRequest.getTema()))
                    .build()
            )
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveSvar.class)
            .block();
    }

    private Optional<Collection<String>> tilOptionalListe(String[] array) {
        return array != null ? Optional.of(Arrays.stream(array).toList()) : Optional.empty();
    }

    public OppgaveDto oppdaterOppgave(OppgaveDto request) {
        return webClient.put()
            .uri(OPPGAVE_URI_MED_ID, request.getId())
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(OppgaveDto.class)
            .block();
    }

    public String opprettOppgave(OpprettOppgaveDto request) {
        return webClient.post()
            .uri(OPPGAVE_BASE_URI)
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
