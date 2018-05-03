package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.BehandleOppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.FinnOppgaveListeFilterMal;
import no.nav.melosys.integrasjon.gsak.oppgave.FinnOppgaveListeRequestMal;
import no.nav.melosys.integrasjon.gsak.oppgave.FinnOppgaveListeSokMal;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.sakapi.SakApiConsumer;
import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakDto;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSFerdigstillOppgaveException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSOppgaveIkkeFunnetException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSOptimistiskLasingException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSSikkerhetsbegrensningException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSAktor;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSAktorType;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSFerdigstillOppgaveRequest;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSLagreOppgave;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSLagreOppgaveRequest;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOppgave;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveRequest;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.binding.HentOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeSortering;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.HentOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.HentOppgaveResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!mocking")
public class GsakService implements GsakFasade {

    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final String FAGOMRÅDE_KODE_MEDLEMSKAP = "MED";
    private static final String FAGOMRÅDE_KODE_UNNTAK = "UFM";
    private static final String FAGSYSTEM_KODE_MELOSYS = "FS22";// TODO (FA) endre når koden er opprettet i GSAK
    private static final int MELOSYS_ENHET_ID = 4530;
    private static final String SAK_TYPE_FAGSAK = "MFS"; // -> Med fagsak
    private static final String SORTERING_MED_FRIST = "FRIST_DATO";
    private static final String SORTERING_STIGENDE = "STIGENDE";

    private SakApiConsumer sakApiConsumer;

    private OppgaveConsumer oppgaveConsumer;

    private BehandleOppgaveConsumer behandleOppgaveConsumer;

    @Autowired
    public GsakService(SakApiConsumer sakApiConsumer, OppgaveConsumer oppgaveConsumer, BehandleOppgaveConsumer behandleOppgaveConsumer) {
        this.sakApiConsumer = sakApiConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
        this.behandleOppgaveConsumer = behandleOppgaveConsumer;
    }

    @Override
    public String opprettSak(Long fagsakId, String fnr) { // FIXME: Kalles med aktørID når TPS-oppslag er på plass
        SakDto sakDto = new SakDto();
        sakDto.setTema(FAGOMRÅDE_KODE_MEDLEMSKAP);
        sakDto.setAktoerId(fnr);
        sakDto.setApplikasjon(FAGSYSTEM_KODE_MELOSYS);
        sakDto.setFagsakNr(FAGSYSTEM_KODE_MELOSYS + fagsakId.toString());

        sakDto = sakApiConsumer.opprettSak(sakDto);

        if (sakDto.getId() == null) {
            log.error("Feil ved oppretting av sak i GSAK.");
            throw new IntegrasjonException("Feil ved oppretting av sak i GSAK.");
        }
        log.info("Sak opprettet i GSAK med saksnummer: {}", sakDto.getId());

        return sakDto.getId().toString();
    }

    @Override
    public void ferdigstillOppgave(String oppgaveId) throws SikkerhetsbegrensningException, TekniskException {
        WSFerdigstillOppgaveRequest request = new WSFerdigstillOppgaveRequest();

        request.setOppgaveId(oppgaveId);
        request.setFerdigstiltAvEnhetId(MELOSYS_ENHET_ID);

        try {
            behandleOppgaveConsumer.ferdigstillOppgave(request);
        } catch (WSSikkerhetsbegrensningException e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (WSFerdigstillOppgaveException e) {
            throw new TekniskException(e);
        }
    }

    // FIXME GSAK oppretter et nytt API med REST tjenester. Den metoden må endres når disse kommer.
    @Override
    public List<no.nav.melosys.domain.Oppgave> finnUtildelteOppgaverEtterFrist(Oppgavetype oppavetype, List<String> fagområdeKodeListe, List<String> sakstyper, List<String> behandlingstyper) throws IntegrasjonException {
        FinnOppgaveListeSokMal sokMal = FinnOppgaveListeSokMal.builder().medAnsvarligEnhetId(Integer.toString(MELOSYS_ENHET_ID)).medFagområdeKodeListe(fagområdeKodeListe).build();

        FinnOppgaveListeFilterMal.Builder filterMalBuilder = FinnOppgaveListeFilterMal.builder();
        FinnOppgaveListeFilterMal filterMal = filterMalBuilder.medAktiv(true).medUfordelte(true).build();
        // TODO mapping med behandlingstyper og sakstyper

        FinnOppgaveListeSortering sortering = new FinnOppgaveListeSortering();
        sortering.setSorteringselementKode(SORTERING_MED_FRIST);
        sortering.setSorteringKode(SORTERING_STIGENDE);

        FinnOppgaveListeRequestMal requestMal = FinnOppgaveListeRequestMal.builder().medSok(sokMal).medFilter(filterMal).medSortering(sortering).build();
        FinnOppgaveListeResponse finnOppgaveListeResponse = oppgaveConsumer.finnOppgaveListe(requestMal);

        List<Oppgave> oppgaver = finnOppgaveListeResponse.getOppgaveListe();
        List<no.nav.melosys.domain.Oppgave> funnet = new ArrayList<>();
        for (Oppgave o : oppgaver) {
            no.nav.melosys.domain.Oppgave oppgave = new no.nav.melosys.domain.Oppgave();
            oppgave.setOppgaveId(o.getOppgaveId());
            oppgave.setAktivFra(KonverteringsUtils.xmlGregorianCalendarToLocalDate(o.getAktivFra()));
            oppgave.setAktivTil(KonverteringsUtils.xmlGregorianCalendarToLocalDate(o.getAktivTil()));
            if (o.getFagomrade() != null) {
                oppgave.setFagomrade(Fagomrade.valueOf(o.getFagomrade().getKode()));
            }
            if (o.getUnderkategori() != null) {
                oppgave.setUnderkategori(Underkategori.valueOf(o.getUnderkategori().getKode()));
            }
            if (o.getOppgavetype() != null) {
                oppgave.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.valueOf(o.getOppgavetype().getKode()));
            }
            oppgave.setGsakSaksnummer(o.getSaksnummer());
            oppgave.setDokumentId(o.getDokumentId());

            funnet.add(oppgave);
        }

        return funnet;
    }

    @Override
    public no.nav.melosys.domain.Oppgave hentOppgave(String oppgaveId) {
        HentOppgaveRequest request = new HentOppgaveRequest();
        request.setOppgaveId(oppgaveId);

        try {
            HentOppgaveResponse response = oppgaveConsumer.hentOppgave(request);
            Oppgave gsakOppgave = response.getOppgave();

            if (gsakOppgave == null) {
                return null;
            }
            no.nav.melosys.domain.Oppgave oppgave = new no.nav.melosys.domain.Oppgave();
            oppgave.setOppgaveId(gsakOppgave.getOppgaveId());
            if (gsakOppgave.getPrioritet() != null) {
                oppgave.setPrioritet(PrioritetType.valueOf(gsakOppgave.getPrioritet().getKode()));
            }
            return oppgave;
        } catch (HentOppgaveOppgaveIkkeFunnet hentOppgaveOppgaveIkkeFunnet) {
            throw new IntegrasjonException(hentOppgaveOppgaveIkkeFunnet);
        }
    }

    @Override
    public String opprettOppgave(OpprettOppgaveRequest request) throws SikkerhetsbegrensningException {
        WSOpprettOppgaveRequest wsRequest = convertToWSRequest(request);

        try {
            WSOpprettOppgaveResponse response = behandleOppgaveConsumer.opprettOppgave(wsRequest);
            return response.getOppgaveId();
        } catch (WSSikkerhetsbegrensningException e) {
            throw new SikkerhetsbegrensningException(e);
        }
    }

    @Override
    public void leggTilbakeOppgave(no.nav.melosys.domain.Oppgave oppgave) throws IntegrasjonException, SikkerhetsbegrensningException, TekniskException {
        WSLagreOppgaveRequest wsRequest = new WSLagreOppgaveRequest();
        WSLagreOppgave wsOppgave = new WSLagreOppgave();

        try {
            // oppgaveId er String i request til BehandleOppgave_v1.opprettOppgave og i respons fra
            // Oppgave_v3.finnUtildelteOppgaverEtterFrist, men int i request til BehandleOppgave_v1.lagreOppgave
            int oppgaveId = Integer.parseInt(oppgave.getOppgaveId());
            wsOppgave.setOppgaveId(oppgaveId);
        } catch (NumberFormatException e) {
            throw new IntegrasjonException("'" + oppgave.getOppgaveId() + "' er ikke en gyldig oppgaveId");
        }
        wsOppgave.setGjelderBruker(null);
        wsRequest.setEndretAvEnhetId(MELOSYS_ENHET_ID);
        wsRequest.setWsLagreOppgave(wsOppgave);

        try {
            behandleOppgaveConsumer.lagreOppgave(wsRequest);
        } catch (WSOppgaveIkkeFunnetException e) {
            throw new IntegrasjonException(e);
        } catch (WSSikkerhetsbegrensningException e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (WSOptimistiskLasingException e) {
            throw new TekniskException(e);
        }
    }

    WSOpprettOppgaveRequest convertToWSRequest(OpprettOppgaveRequest request) {
        WSOppgave oppgave = new WSOppgave();
        oppgave.setSaksnummer(request.getSaksnummer());
        oppgave.setAnsvarligEnhetId(request.getAnsvarligEnhetId());
        if (request.getFagområde() != null) {
            oppgave.setFagomradeKode(request.getFagområde().name());
        }
        oppgave.setGjelderBruker(byggWSAktør(request.getFnr(), request.getAktørType()));

        try {
            Optional<LocalDate> aktivFra = request.getAktivFra();
            if (aktivFra.isPresent()) {
                oppgave.setAktivFra(KonverteringsUtils.localDateTimeToXMLGregorianCalendar(aktivFra.get().atStartOfDay()));
            }
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException("Kan ikke sette aktiv fradato", e);
        }

        try {
            Optional<LocalDate> aktivTil = request.getAktivTil();
            if (aktivTil.isPresent()) {
                oppgave.setAktivTil(KonverteringsUtils.localDateTimeToXMLGregorianCalendar(aktivTil.get().atStartOfDay()));
            }
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException("Kan ikke sette aktiv tildato", e);
        }

        if (request.getOppgaveType() != null) {
            oppgave.setOppgavetypeKode(request.getOppgaveType().name());
        }
        if (request.getUnderkategoriKode() != null) {
            oppgave.setUnderkategoriKode(request.getUnderkategoriKode().name());
        }
        if (request.getPrioritetType() != null) {
            oppgave.setPrioritetKode(request.getPrioritetType().name());
        }
        oppgave.setBeskrivelse(request.getBeskrivelse());
        oppgave.setLest(request.isLest());
        oppgave.setDokumentId(request.getDokumentId());

        try {
            oppgave.setNormDato(KonverteringsUtils.localDateToXMLGregorianCalendar(request.getNormertBehandlingsTidInnen()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException("Kan ikke sette NormDato", e);
        }
        try {
            oppgave.setMottattDato(KonverteringsUtils.localDateToXMLGregorianCalendar(request.getMottattDato()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException("Kan ikke sette MottattDato", e);
        }

        WSOpprettOppgaveRequest wsRequest = new WSOpprettOppgaveRequest();
        wsRequest.setOpprettetAvEnhetId(request.getOpprettetAvEnhetId());
        wsRequest.setWsOppgave(oppgave);
        return wsRequest;
    }

    @Override
    public List<no.nav.melosys.domain.Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId)
            throws IntegrasjonException {

        FinnOppgaveListeSokMal sokMal = FinnOppgaveListeSokMal.builder().medAnsvarligEnhetId(Integer.toString(MELOSYS_ENHET_ID)).medAnsvarligId(ansvarligId).build();

        FinnOppgaveListeFilterMal.Builder filterMalBuilder = FinnOppgaveListeFilterMal.builder();
        FinnOppgaveListeFilterMal filterMal = filterMalBuilder.medAktiv(true).build();

        FinnOppgaveListeSortering sortering = new FinnOppgaveListeSortering();
        sortering.setSorteringselementKode(SORTERING_MED_FRIST);
        sortering.setSorteringKode(SORTERING_STIGENDE);

        FinnOppgaveListeRequestMal requestMal = FinnOppgaveListeRequestMal.builder().medSok(sokMal).medFilter(filterMal).medSortering(sortering).build();

        List<no.nav.melosys.domain.Oppgave> localDomainObjects = new ArrayList<>();
        FinnOppgaveListeResponse finnOppgaveListeResponse = oppgaveConsumer.finnOppgaveListe(requestMal);
        finnOppgaveListeResponse.getOppgaveListe().forEach(oppgave -> {
            //FIXME: Sjekk for NPE
            no.nav.melosys.domain.Oppgave domainOppave = new no.nav.melosys.domain.Oppgave();
            domainOppave.setOppgaveId(oppgave.getOppgaveId());
            domainOppave.setPrioritet(PrioritetType.valueOf(oppgave.getPrioritet().getKode()));
            domainOppave.setAktivTil(oppgave.getAktivTil().toGregorianCalendar().toZonedDateTime().toLocalDate());
            domainOppave.setDokumentId(oppgave.getDokumentId());
            domainOppave.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.valueOf(oppgave.getOppgavetype().getKode()));
            domainOppave.setGsakSaksnummer(oppgave.getSaksnummer());
            localDomainObjects.add(domainOppave);
        });
        return localDomainObjects;
    }

    @Override
    public List<no.nav.melosys.domain.Oppgave> finnOppgaveListeMedBruker(String ident) throws IntegrasjonException {
        // FIXME Venter på nye GSAK tjenester
        return null;
    }

    @Override
    public void fjernTildeling() {
        // FIXME trenges ikke når integrasjon med GSAK er på plass.
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) {
        // FIXME Venter på nye GSAK tjenester
    }

    private WSAktor byggWSAktør(String ident, AktorType aktørType) {
        WSAktor wsAktor = new WSAktor();
        wsAktor.setIdent(ident);
        if (aktørType == null || AktorType.BLANK == aktørType) {
            return wsAktor;
        } else if (AktorType.ORGANISASJON == aktørType) {
            wsAktor.setAktorType(WSAktorType.ORGANISASJON);
        } else if (AktorType.PERSON == aktørType) {
            wsAktor.setAktorType(WSAktorType.PERSON);
        } else {
            wsAktor.setAktorType(WSAktorType.UKJENT);
        }
        return wsAktor;
    }
}
