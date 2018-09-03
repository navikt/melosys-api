package no.nav.melosys.integrasjon.gsak;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Kodeverk;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.sak.SakApiConsumer;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.util.KodeverkUtils.erGyldigKode;
import static no.nav.melosys.domain.util.KodeverkUtils.hentAlleKoder;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
@Primary
public class GsakService implements GsakFasade {

    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final String SORTERINGSFELT = "FRIST";

    private static final String OPPGAVE_STATUS_FERDIGSTILT = "FERDIGSTILT";

    private final SakApiConsumer sakApiConsumer;

    private final OppgaveConsumer oppgaveConsumer;

    @Autowired
    public GsakService(SakApiConsumer sakApiConsumer, OppgaveConsumer oppgaveConsumer) {
        this.sakApiConsumer = sakApiConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
    }

    @Override
    public String opprettSak(String saksnummer, BehandlingType behandlingType, String aktørId) throws TekniskException, IntegrasjonException {
        SakDto sakDto = new SakDto();

        if (behandlingType.equals(BehandlingType.SØKNAD)) {
            sakDto.setTema(Tema.MED.getKode());
        } else if (behandlingType.equals(BehandlingType.UNNTAK_MEDL)) {
            sakDto.setTema(Tema.UFM.getKode());
        } else {
            throw new TekniskException("Behandlingtype " + behandlingType.getBeskrivelse() + " er ikke støttet.");
        }

        sakDto.setAktørId(aktørId);
        sakDto.setApplikasjon(Fagsystem.MELOSYS.getKode());
        sakDto.setSaksnummer(saksnummer);
        sakDto = sakApiConsumer.opprettSak(sakDto);

        if (sakDto.getId() == null) {
            log.error("Feil ved oppretting av sak i GSAK.");
            throw new IntegrasjonException("Feil ved oppretting av sak i GSAK.");
        }
        log.info("Sak opprettet i GSAK med saksnummer: {}", sakDto.getId());
        return sakDto.getId().toString();
    }

    @Override
    public void ferdigstillOppgave(String oppgaveID) throws IkkeFunnetException, FunksjonellException, SikkerhetsbegrensningException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveID);
        if (oppgave == null) {
            throw new IkkeFunnetException("Oppgave med ID " + oppgaveID + " er ikke funnet");
        } else {
            oppgave.setStatus(OPPGAVE_STATUS_FERDIGSTILT);
            oppgaveConsumer.oppdaterOppgave(oppgave);
        }
    }

    //FIXME: Mangler implementasjon for sakstyper
    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(Oppgavetype oppgavetype, Tema tema, List<FagsakType> sakstyper, List<BehandlingType> behandlingstyper) throws TekniskException {
        OppgaveSearchRequest.Builder searchRequestBuilder = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medOppgaveTyper(new String[]{oppgavetype.getKode()})
            .medBehandlingsTyper(behandlingstyper.stream().map(Kodeverk::getKode).toArray(String[]::new))
            .medSorteringsfelt(SORTERINGSFELT);

        if (tema != null) {
            searchRequestBuilder = searchRequestBuilder.medTema( new String[]{tema.getKode()});
        }

        List<OppgaveDto> oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequestBuilder.build());
        List<Oppgave> funnet = new ArrayList<>();

        oppgaver.stream()
            .filter(Objects::isNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .forEach(funnet::add);
        return funnet;
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) throws IkkeFunnetException, TekniskException {
        OppgaveDto gsakOppgave = oppgaveConsumer.hentOppgave(oppgaveId);

        if (gsakOppgave == null) {
            throw new IkkeFunnetException("Oppgave med oppgaveID " + oppgaveId + " finnes ikke");
        }
        return oppgaveMappingDtoTilDomain(gsakOppgave);
    }

    @Override
    public String opprettOppgave(Oppgave request) throws SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setJournalpostId(request.getJournalpostId());
        oppgaveDto.setSaksreferanse(request.getGsakSaksnummer());
        oppgaveDto.setAktørId(request.getAktørId());
        oppgaveDto.setTilordnetRessurs(request.getTilordnetRessurs());
        oppgaveDto.setTema(request.getTema().getKode());
        oppgaveDto.setOppgavetype(request.getOppgavetype().getKode());
        oppgaveDto.setFristFerdigstillelse(request.getFristFerdigstillelse());
        // FIXME: MELOSYS-1401 : skal implementere Behandlingstema,Behandlingstype,Temagruppe
        return oppgaveConsumer.opprettOppgave(oppgaveDto);
    }

    @Override
    public void leggTilbakeOppgave(String oppgaveId) throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveId);
        }
        oppgave.setTilordnetRessurs(null);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }

    @Override
    public List<Oppgave> finnOppgaveListeMedAnsvarlig(String tilordnetRessurs) throws TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medTilordnetRessurs(tilordnetRessurs)
            .medOppgaveTyper(hentAlleKoder(Oppgavetype.class))
            .medSorteringsfelt(SORTERINGSFELT)
            .build();

        List<Oppgave> localDomainObjects = new ArrayList<>();
        List<OppgaveDto> finnOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest);

        finnOppgaveListeResponse.stream()
            .filter(Objects::nonNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .forEach(localDomainObjects::add);

        return localDomainObjects;
    }

    private static Oppgave oppgaveMappingDtoTilDomain(OppgaveDto oppgave) {
        Oppgave domainOppgave = new Oppgave();
        domainOppgave.setOppgaveId(oppgave.getId());
        domainOppgave.setVersjon(oppgave.getVersjon());
        if (oppgave.getPrioritet() != null ) {
            domainOppgave.setPrioritet(PrioritetType.valueOf(oppgave.getPrioritet()));
        }
        domainOppgave.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());
        domainOppgave.setJournalpostId(oppgave.getJournalpostId());

        domainOppgave.setGsakSaksnummer(oppgave.getSaksreferanse());
        domainOppgave.setFristFerdigstillelse(oppgave.getFristFerdigstillelse());

        if (oppgave.getTema() != null && erGyldigKode(Tema.class, oppgave.getTema())) {
            domainOppgave.setTema(Tema.valueOf(oppgave.getTema()));
        }  else {
            log.error("Fikk uventet Tema: {} for OppgaveID: {}", oppgave.getTema(), oppgave.getId());
        }

        if (oppgave.getOppgavetype() != null && erGyldigKode(Oppgavetype.class, oppgave.getOppgavetype())) {
            domainOppgave.setOppgavetype(no.nav.melosys.domain.oppgave.Oppgavetype.valueOf(oppgave.getOppgavetype()));
        } else {
            log.error("Fikk uventet oppgaveType: {} for OppgaveID: {}", oppgave.getOppgavetype(), oppgave.getId());
        }
        domainOppgave.setJournalpostId(oppgave.getJournalpostId());
        domainOppgave.setTilordnetRessurs(oppgave.getTilordnetRessurs());

        return domainOppgave;
    }

    @Override
    public List<Oppgave> finnOppgaveListeMedBruker(String aktørId) throws TekniskException {
        OppgaveSearchRequest oppgaveSearchRequest = new OppgaveSearchRequest.Builder(String.valueOf(MELOSYS_ENHET_ID))
            .medAktørId(aktørId)
            .medOppgaveTyper(hentAlleKoder(Oppgavetype.class))
            .medSorteringsfelt(SORTERINGSFELT)
            .build();
        List<Oppgave> localDomainObjects = new ArrayList<>();

        List<OppgaveDto> finnOppgaveListeResponse = oppgaveConsumer.hentOppgaveListe(oppgaveSearchRequest);
        finnOppgaveListeResponse.stream()
            .filter(Objects::nonNull)
            .map(GsakService::oppgaveMappingDtoTilDomain)
            .forEach(localDomainObjects::add);

        return localDomainObjects;
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        OppgaveDto oppgave = oppgaveConsumer.hentOppgave(oppgaveId);
        if (oppgave == null ) {
            throw new IkkeFunnetException("Feil ved henting av oppgave for oppgaveID:" + oppgaveId);
        }
        oppgave.setTilordnetRessurs(saksbehandlerID);
        oppgaveConsumer.oppdaterOppgave(oppgave);
    }
}
