package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.SOEKNAD;
import static no.nav.melosys.domain.util.KodeverkUtils.erGyldigKode;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
@Primary
public class GsakService implements GsakFasade {
    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final String OPPGAVE_STATUS_FERDIGSTILT = "FERDIGSTILT";
    private static final String SORTERINGSFELT = "FRIST";
    private static final String OPPGAVE_STATUSKATEGORI_AAPEN = "AAPEN";

    private final SakConsumer sakConsumer;

    private final OppgaveConsumer oppgaveConsumer;

    @Autowired
    public GsakService(SakConsumer sakConsumer, OppgaveConsumer oppgaveConsumer) {
        this.sakConsumer = sakConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
    }

    @Override
    public Long opprettSak(String saksnummer, Behandlingstyper behandlingstype, String aktørId) throws FunksjonellException, TekniskException {
        SakDto sakDto = new SakDto();

        if (behandlingstype.equals(SOEKNAD)) {
            sakDto.setTema(Tema.MED.getKode());
        } else {
            throw new TekniskException("Behandlingtype " + behandlingstype.getBeskrivelse() + " er ikke støttet.");
        }

        sakDto.setAktørId(aktørId);
        sakDto.setApplikasjon(Fagsystem.MELOSYS.getKode());
        sakDto.setSaksnummer(saksnummer);
        sakDto = sakConsumer.opprettSak(sakDto);

        if (sakDto.getId() == null) {
            log.error("Feil ved oppretting av sak i GSAK.");
            throw new IntegrasjonException("Feil ved oppretting av sak i GSAK.");
        }
        log.info("Sak opprettet i GSAK med saksnummer: {}", sakDto.getId());
        return sakDto.getId();
    }

    @Override
    public void ferdigstillOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveID);
        if (oppgave == null) {
            throw new IkkeFunnetException("Oppgave med ID " + oppgaveID + " er ikke funnet.");
        } else {
            oppgave.setStatus(OPPGAVE_STATUS_FERDIGSTILT);
            oppgaveConsumer.oppdaterOppgave(oppgave);
        }
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(Set<Oppgavetyper> oppgavetyper, Set<Sakstyper> sakstyper, Set<Behandlingstyper> behandlingstyper, Set<Behandlingstema> behandlingstemaer)
        throws FunksjonellException, TekniskException {
        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medOppgaveTyper(oppgavetyper.stream().map(Oppgavetyper::getKode).toArray(String[]::new))
            .medBehandlingsTyper(behandlingstyper.stream().map(this::hentFellesKode).toArray(String[]::new))
            .medBehandlingstema(behandlingstemaer.stream().map(Behandlingstema::getKode).toArray(String[]::new))
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medTildeltRessurs(false);

        if (!oppgavetyper.equals(Collections.singleton(Oppgavetyper.JFR))) {
            searchRequestBuilder.medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode());
        }

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());

        return oppgaver.stream().map(GsakService::oppgaveMappingDtoTilDomain).collect(Collectors.toList());
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        OppgaveDto gsakOppgave = oppgaveConsumer.hentOppgave(oppgaveId);

        if (gsakOppgave == null) {
            throw new IkkeFunnetException("Oppgave med oppgaveID " + oppgaveId + " finnes ikke.");
        }
        return oppgaveMappingDtoTilDomain(gsakOppgave);
    }

    @Override
    public String opprettOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException {
        LocalDate idag = LocalDate.now();
        OpprettOppgaveDto oppgaveDto = new OpprettOppgaveDto();
        oppgaveDto.setAktivDato(idag);
        oppgaveDto.setAktørId(oppgave.getAktørId());
        if (oppgave.getBehandlingstype() != null) {
            oppgaveDto.setBehandlingstype(hentFellesKode(oppgave.getBehandlingstype()));
        }
        if (oppgave.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema().getKode());
        }
        oppgaveDto.setBeskrivelse(oppgave.getBeskrivelse());
        oppgaveDto.setFristFerdigstillelse(oppgave.lagFristFerdigstillelse(idag));
        oppgaveDto.setJournalpostId(oppgave.getJournalpostId());
        oppgaveDto.setOppgavetype(oppgave.getOppgavetype().getKode());
        oppgaveDto.setPrioritet(oppgave.getPrioritet().toString());
        oppgaveDto.setSaksreferanse(oppgave.getSaksnummer());
        oppgaveDto.setTema(oppgave.getTema().getKode());
        oppgaveDto.setTildeltEnhetsnr(Integer.toString(MELOSYS_ENHET_ID));
        if (oppgave.getBehandlesAvApplikasjon() != Fagsystem.INTET) {
            oppgaveDto.setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon().getKode());
        }

        oppgaveDto.setTilordnetRessurs(oppgave.getTilordnetRessurs());

        return oppgaveConsumer.opprettOppgave(oppgaveDto);
    }

    @Override
    public void leggTilbakeOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveId);
        }
        oppgave.setTilordnetRessurs(null);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public List<Oppgave> finnOppgaveListeMedAnsvarlig(String tilordnetRessurs) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medTilordnetRessurs(tilordnetRessurs)
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN);

        return hentOppgaverAlleTyper(oppgaveSearchRequestBuilder);
    }

    private List<Oppgave> hentOppgaverAlleTyper(OppgaveSearchRequest.Builder oppgaveSearchRequestBuilder) throws FunksjonellException, TekniskException {
        // Henter oppgaver opprettet av melosys, hvor melosys har satt behandlesAvApplikasjon
        List<OppgaveDto> finnOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(
            oppgaveSearchRequestBuilder.medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode()).build()
        );

        // Henter journalføringsoppgaver. Disse er ikke opprettet av Melosys
        List<OppgaveDto> finnJfrOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(
            oppgaveSearchRequestBuilder.medOppgaveTyper(new String[]{Oppgavetyper.JFR.getKode()})
                .medBehandlesAvApplikasjon(null).build()
        );

        return Stream.of(finnJfrOppgaveListeResponse, finnOppgaveListeResponse)
            .flatMap(Collection::stream)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());
    }

    private static Oppgave oppgaveMappingDtoTilDomain(OppgaveDto oppgave) {
        Oppgave.Builder domainOppgaveBuilder = new Oppgave.Builder();
        domainOppgaveBuilder.setOppgaveId(oppgave.getId());
        domainOppgaveBuilder.setVersjon(oppgave.getVersjon());
        if (oppgave.getPrioritet() != null) {
            domainOppgaveBuilder.setPrioritet(PrioritetType.valueOf(oppgave.getPrioritet()));
        }
        domainOppgaveBuilder.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());
        domainOppgaveBuilder.setJournalpostId(oppgave.getJournalpostId());

        if (oppgave.getSaksreferanse() != null) {
            domainOppgaveBuilder.setSaksnummer(oppgave.getSaksreferanse());
        }

        domainOppgaveBuilder.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());

        if (oppgave.getTema() != null && erGyldigKode(Tema.class, oppgave.getTema())) {
            domainOppgaveBuilder.setTema(Tema.valueOf(oppgave.getTema()));
        } else {
            log.error("Fikk uventet Tema: {} for OppgaveID: {}", oppgave.getTema(), oppgave.getId());
        }

        if (oppgave.getOppgavetype() != null && erGyldigKode(Oppgavetyper.class, oppgave.getOppgavetype())) {
            domainOppgaveBuilder.setOppgavetype(no.nav.melosys.domain.kodeverk.Oppgavetyper.valueOf(oppgave.getOppgavetype()));
        } else {
            log.error("Fikk uventet oppgaveType: {} for OppgaveID: {}", oppgave.getOppgavetype(), oppgave.getId());
        }
        domainOppgaveBuilder.setJournalpostId(oppgave.getJournalpostId());
        domainOppgaveBuilder.setTilordnetRessurs(oppgave.getTilordnetRessurs());
        domainOppgaveBuilder.setAktørId(oppgave.getAktørId());
        return domainOppgaveBuilder.build();
    }

    @Override
    public List<Oppgave> finnBehandlingsoppgaverMedBruker(String aktørId) throws FunksjonellException, TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medAktørId(aktørId)
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode()})
            .medSorteringsfelt(SORTERINGSFELT)
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .medBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode())
            .build();

        return oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest).stream()
            .filter(Objects::nonNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Oppgave finnOppgaveMedSaksnummer(String saksnummer) throws TekniskException, FunksjonellException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medSaksreferanse(new String[]{saksnummer})
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode(), Oppgavetyper.BEH_SED.getKode()})
            .medStatusKategori(OPPGAVE_STATUSKATEGORI_AAPEN)
            .build();

        List<OppgaveDto> finnOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest);
        List<Oppgave> oppgaver = finnOppgaveListeResponse.stream()
            .filter(Objects::nonNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .collect(Collectors.toList());

        if (!oppgaver.isEmpty()) {
            if (oppgaver.size() > 1) {
                throw new TekniskException("Det finnes flere aktive behandlingsoppgaver for sak " + saksnummer);
            }
            return oppgaver.get(0);
        } else {
            throw new IkkeFunnetException("Det finnes ingen aktive behandlingsoppgaver for sak " + saksnummer);
        }
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) throws FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null) {
            throw new IkkeFunnetException(String.format("Feil ved henting av "
                + "oppgave %s for saksbehandler %s:", oppgaveId, saksbehandlerID));
        }
        oppgave.setTilordnetRessurs(saksbehandlerID);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    /**
     * Henter koder fra felleskodeverk: Behandlingstyper.
     */
    private String hentFellesKode(Behandlingstyper behandlingstyper) {
        switch (behandlingstyper) {
            case SOEKNAD: return "ae0034";
            case ENDRET_PERIODE: return "ae0052";
            case ANKE: return "ae0046";
            case KLAGE: return  "ae0058";
            case UTL_MYND_UTPEKT_NORGE: return "ae0112";
            case NY_VURDERING: return "ae0028";
            case UTL_MYND_UTPEKT_SEG_SELV: return "ae0113";
            case REGISTRERING_UNNTAK_NORSK_TRYGD: return "ae0111"; //TODO: avklar om korrekt kode eller trenger ny
            default: throw new IllegalArgumentException(this + " er ikke implementert i felleskodeverk.");
        }
    }
}
