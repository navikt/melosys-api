package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.Fagsystem;
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
import static no.nav.melosys.domain.kodeverk.Behandlingstyper.SOEKNAD;
import static no.nav.melosys.domain.util.KodeverkUtils.erGyldigKode;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
@Primary
public class GsakService implements GsakFasade {
    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final int FRIST_JFR_DAGER = 1;
    private static final int FRIST_VUR_DAGER = 1;
    private static final int FRIST_BEH_UKER = 12;
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
        if (oppgave.erJournalFøring()) {
            oppgaveDto.setFristFerdigstillelse(idag.plusDays(FRIST_JFR_DAGER));
        } else if (oppgave.erBehandling()) {
            oppgaveDto.setFristFerdigstillelse(idag.plusWeeks(FRIST_BEH_UKER));
        } else if (oppgave.erVurderDokument()) {
            oppgaveDto.setFristFerdigstillelse(idag.plusWeeks(FRIST_VUR_DAGER));
        } else {
            throw new FunksjonellException("Type " + oppgave.getOppgavetype().getKode() + " støttes ikke.");
        }
        if (oppgave.getBehandlingstema() != null) {
            oppgaveDto.setBehandlingstema(oppgave.getBehandlingstema().getKode());
        }
        oppgaveDto.setJournalpostId(oppgave.getJournalpostId());
        oppgaveDto.setOppgavetype(oppgave.getOppgavetype().getKode());
        oppgaveDto.setPrioritet(PrioritetType.NORM.toString());
        oppgaveDto.setSaksreferanse(oppgave.getSaksnummer());
        oppgaveDto.setTema(oppgave.getTema().getKode());
        oppgaveDto.setTildeltEnhetsnr(Integer.toString(MELOSYS_ENHET_ID));
        oppgaveDto.setBehandlesAvApplikasjon(Fagsystem.MELOSYS.getKode());

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
        Oppgave domainOppgave = new Oppgave();
        domainOppgave.setOppgaveId(oppgave.getId());
        domainOppgave.setVersjon(oppgave.getVersjon());
        if (oppgave.getPrioritet() != null) {
            domainOppgave.setPrioritet(PrioritetType.valueOf(oppgave.getPrioritet()));
        }
        domainOppgave.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());
        domainOppgave.setJournalpostId(oppgave.getJournalpostId());

        if (oppgave.getSaksreferanse() != null) {
            domainOppgave.setSaksnummer(oppgave.getSaksreferanse());
        }

        domainOppgave.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());

        if (oppgave.getTema() != null && erGyldigKode(Tema.class, oppgave.getTema())) {
            domainOppgave.setTema(Tema.valueOf(oppgave.getTema()));
        } else {
            log.error("Fikk uventet Tema: {} for OppgaveID: {}", oppgave.getTema(), oppgave.getId());
        }

        if (oppgave.getOppgavetype() != null && erGyldigKode(Oppgavetyper.class, oppgave.getOppgavetype())) {
            domainOppgave.setOppgavetype(no.nav.melosys.domain.kodeverk.Oppgavetyper.valueOf(oppgave.getOppgavetype()));
        } else {
            log.error("Fikk uventet oppgaveType: {} for OppgaveID: {}", oppgave.getOppgavetype(), oppgave.getId());
        }
        domainOppgave.setJournalpostId(oppgave.getJournalpostId());
        domainOppgave.setTilordnetRessurs(oppgave.getTilordnetRessurs());
        domainOppgave.setBehandlesAvApplikasjon(oppgave.getBehandlesAvApplikasjon());
        domainOppgave.setAktørId(oppgave.getAktørId());
        return domainOppgave;
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
            .medOppgaveTyper(new String[]{Oppgavetyper.BEH_SAK_MK.getKode(), Oppgavetyper.VUR.getKode()})
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
            case NORGE_UTPEKT: return "ae0112";
            case NY_VURDERING: return "ae0028";
            case PAASTAND_UTL: return "ae0113";
            case UNNTAK_FRA_MEDLEMSKAP: return "ae0111"; //TODO: avklar om korrekt kode eller trenger ny
            default: throw new IllegalArgumentException(this + " er ikke implementert i felleskodeverk.");
        }
    }
}
