package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.BehandleOppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.integrasjon.gsak.behandlesak.BehandleSakConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.FinnOppgaveListeFilterMal;
import no.nav.melosys.integrasjon.gsak.oppgave.FinnOppgaveListeRequestMal;
import no.nav.melosys.integrasjon.gsak.oppgave.FinnOppgaveListeSokMal;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSFerdigstillOppgaveException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSSikkerhetsbegrensningException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSAktor;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSAktorType;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSFerdigstillOppgaveRequest;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOppgave;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveRequest;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveResponse;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Fagomraader;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Fagsystemer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Person;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Sak;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Sakstyper;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeSortering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GsakService implements GsakFasade {

    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final String FAGOMRÅDE_KODE_MEDLEMSKAP = "MED";
    private static final String FAGOMRÅDE_KODE_UNNTAK = "UFM";
    private static final String FAGSYSTEM_KODE_MELOSYS = "FS22";// TODO (FA) endre når koden er opprettet i GSAK
    private static final int MELOSYS_ENHET_ID = 4530;
    private static final String SAK_TYPE_FAGSAK = "MFS"; // -> Med fagsak
    private static final String SORTERING_MED_FRIST = "FRIST_DATO";
    private static final String SORTERING_STIGENDE = "STIGENDE";

    private BehandleSakConsumer behandleSakConsumer;

    private OppgaveConsumer oppgaveConsumer;

    private BehandleOppgaveConsumer behandleOppgaveConsumer;

    @Autowired
    public GsakService(BehandleSakConsumer behandleSakConsumer, OppgaveConsumer oppgaveConsumer, BehandleOppgaveConsumer behandleOppgaveConsumer) {
        this.behandleSakConsumer = behandleSakConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
        this.behandleOppgaveConsumer = behandleOppgaveConsumer;
    }

    @Override
    public String opprettSak(Long fagsakId, String fnr) throws IntegrasjonException {
        Sak sak = new Sak();
        Fagomraader fagområde = new Fagomraader();
        fagområde.setValue(FAGOMRÅDE_KODE_MEDLEMSKAP);
        sak.setFagomraade(fagområde);

        Sakstyper sakstype = new Sakstyper();
        sakstype.setValue(SAK_TYPE_FAGSAK);
        sak.setSakstype(sakstype);

        Aktoer aktør = new Person();
        aktør.setIdent(fnr);
        sak.getGjelderBrukerListe().add(aktør);

        Fagsystemer fagsystem = new Fagsystemer();
        fagsystem.setValue(FAGSYSTEM_KODE_MELOSYS);
        sak.setFagsystem(fagsystem);

        String fagsystemSakId = FAGSYSTEM_KODE_MELOSYS + fagsakId.toString();
        sak.setFagsystemSakId(fagsystemSakId);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest();
        opprettSakRequest.setSak(sak);

        try {
            OpprettSakResponse response = behandleSakConsumer.opprettSak(opprettSakRequest);
            log.debug("Sak opprettet i GSAK med saksnummer: {}", response.getSakId());
            return response.getSakId();
        } catch (OpprettSakSakEksistererAllerede e) {
            throw new IntegrasjonException(e);
        } catch (OpprettSakUgyldigInput e) {// NOSONAR
            throw new IntegrasjonException(e);
        }
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
    public List<no.nav.melosys.domain.Oppgave> finnUtildelteOppgaverEtterFrist(String oppavetype, List<String> fagområdeKodeListe, List<String> sakstyper, List<String> behandlingstyper) throws IntegrasjonException {
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
                oppgave.setOppgavetype(Oppgavetype.valueOf(o.getOppgavetype().getKode()));
            }
            oppgave.setSaksnummer(o.getSaksnummer());
            oppgave.setDokumentId(o.getDokumentId());

            funnet.add(oppgave);
        }

        return funnet;
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
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) {
        // FIXME Francois venter på kall
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
