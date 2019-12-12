package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ØVRIGE_SED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public final class GsakServiceTest {

    @Mock
    private OppgaveConsumer oppgaveConsumer;
    @Mock
    private SakConsumer sakConsumer;
    @Captor
    private ArgumentCaptor<OpprettOppgaveDto> opprettOppgaveDtoCaptor;
    @Captor
    private ArgumentCaptor<OppgaveSearchRequest> oppgaveSearchRequestCaptor;
    @Captor
    private ArgumentCaptor<OppgaveDto> oppgaveDtoCaptor;

    private GsakService gsakService;

    @Before
    public void setup() {
        gsakService = new GsakService(sakConsumer, oppgaveConsumer);
    }

    @Test
    public void opprettOppgave_vurderDokument_setterData() throws Exception {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.VUR)
            .setTema(Tema.MED)
            .setBehandlingstema(Behandlingstema.EU_EOS)
            .setFristFerdigstillelse(LocalDate.now());
        gsakService.opprettOppgave(oppgaveBuilder.build());

        ArgumentCaptor<OpprettOppgaveDto> captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveConsumer).opprettOppgave(captor.capture());
        OpprettOppgaveDto opprettOppgaveDto = captor.getValue();

        assertThat(opprettOppgaveDto.getOppgavetype()).isEqualTo(Oppgavetyper.VUR.getKode());
        assertThat(opprettOppgaveDto.getBehandlingstema()).isEqualTo(Behandlingstema.EU_EOS.getKode());
        assertThat(opprettOppgaveDto.getFristFerdigstillelse()).isNotNull();
    }

    @Test
    public void opprettOppgave_gyldigOppgave_validerDto() throws Exception {
        Oppgave oppgave = lagOppgave();

        gsakService.opprettOppgave(oppgave);
        verify(oppgaveConsumer).opprettOppgave(opprettOppgaveDtoCaptor.capture());

        OpprettOppgaveDto oppgaveDto = opprettOppgaveDtoCaptor.getValue();

        assertThat(oppgaveDto).isNotNull();
        assertThat(oppgaveDto.getJournalpostId()).isEqualTo(oppgave.getJournalpostId());
        assertThat(oppgaveDto.getAktørId()).isEqualTo(oppgave.getAktørId());
        assertThat(oppgaveDto.getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(oppgaveDto.getBehandlingstype()).isEqualTo("ae0034");
        assertThat(oppgaveDto.getBeskrivelse()).isEqualTo("bla bla");
        assertThat(oppgaveDto.getOppgavetype()).isEqualTo(oppgave.getOppgavetype().getKode());
        assertThat(oppgaveDto.getPrioritet()).isEqualTo(PrioritetType.NORM.toString());
        assertThat(oppgaveDto.getTema()).isEqualTo(oppgave.getTema().getKode());
        assertThat(oppgaveDto.getTildeltEnhetsnr()).isEqualTo(Integer.toString(Konstanter.MELOSYS_ENHET_ID));
        assertThat(oppgaveDto.getTilordnetRessurs()).isEqualTo(oppgave.getTilordnetRessurs());
    }

    @Test
    public void finnOppgaveListeMedAnsvarlig_gyldigOppgave_verifiserToKallMotOppgave() throws Exception {
        OppgaveDto oppgaveDto = new OppgaveDto();
        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class))).thenReturn(Collections.singletonList(oppgaveDto));

        gsakService.finnOppgaveListeMedAnsvarlig("123");
        verify(oppgaveConsumer, times(2)).hentOppgaveListe(oppgaveSearchRequestCaptor.capture());

        List<OppgaveSearchRequest> requests = oppgaveSearchRequestCaptor.getAllValues();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0).getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(requests.get(0).getOppgavetype()).isNullOrEmpty();
        assertThat(requests.get(1).getBehandlesAvApplikasjon()).isNullOrEmpty();
        assertThat(requests.get(1).getOppgavetype()[0]).isEqualTo(Oppgavetyper.JFR.getKode());
    }

    @Test
    public void finnOppgaveListeMedAnsvarlig_toDuplikateOppgaver_filtrererUtDuplikater() throws Exception {
        final String oppgaveID = "123duplikat";

        OppgaveDto oppgaveDto1 = new OppgaveDto();
        oppgaveDto1.setId(oppgaveID);
        OppgaveDto oppgaveDto2 = new OppgaveDto();
        oppgaveDto2.setId(oppgaveID);

        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class))).thenReturn(Arrays.asList(oppgaveDto1, oppgaveDto2));

        Set<Oppgave> oppgaver = gsakService.finnOppgaveListeMedAnsvarlig("123");

        assertThat(oppgaver.size()).isEqualTo(1);
        assertThat(oppgaver.iterator().next().getOppgaveId()).isEqualTo(oppgaveID);
    }

    @Test
    public void tildelOppgave_verifiserFelterSatt() throws FunksjonellException, TekniskException {
        final String oppgaveID = "2222";
        final String sakbehandler = "Z111111";
        OppgaveDto oppgave = GsakService.oppgaveMappingDomainTilDto(lagOppgave());
        oppgave.setBeskrivelse("test");
        when(oppgaveConsumer.hentOppgave(eq(oppgaveID))).thenReturn(oppgave);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .tilordnetRessurs(sakbehandler)
            .build();

        gsakService.oppdaterOppgave(oppgaveID, oppgaveOppdatering);
        verify(oppgaveConsumer).oppdaterOppgave(oppgaveDtoCaptor.capture());

        OppgaveDto dto = oppgaveDtoCaptor.getValue();
        assertThat(dto.getPrioritet()).isEqualTo(oppgave.getPrioritet());
        assertThat(dto.getBeskrivelse()).isEqualTo(oppgave.getBeskrivelse());
        assertThat(dto.getStatus()).isEqualTo(oppgave.getStatus());
        assertThat(dto.getTilordnetRessurs()).isEqualTo(oppgaveOppdatering.getTilordnetRessurs());
        assertThat(dto.getFristFerdigstillelse()).isEqualTo(oppgave.getFristFerdigstillelse());
    }


    @Test
    public void oppdaterOppgave_alleFelterSattEksisterendeBeskrivelse_verifiserFelterSatt() throws FunksjonellException, TekniskException {
        final String oppgaveID = "2222";
        OppgaveDto oppgave = GsakService.oppgaveMappingDomainTilDto(lagOppgave());
        oppgave.setBeskrivelse("test");
        when(oppgaveConsumer.hentOppgave(eq(oppgaveID))).thenReturn(oppgave);
        LocalDate nå = LocalDate.now();

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .prioritet(PrioritetType.HOY.name())
            .beskrivelse("test")
            .status("AAPEN")
            .tilordnetRessurs("Meg123")
            .fristFerdigstillelse(nå)
            .build();

        gsakService.oppdaterOppgave(oppgaveID, oppgaveOppdatering);
        verify(oppgaveConsumer).oppdaterOppgave(oppgaveDtoCaptor.capture());

        OppgaveDto dto = oppgaveDtoCaptor.getValue();
        assertThat(dto.getPrioritet()).isEqualTo(oppgaveOppdatering.getPrioritet());
        assertThat(dto.getBeskrivelse()).isEqualTo(oppgaveOppdatering.getBeskrivelse() + "\n" + oppgaveOppdatering.getBeskrivelse());
        assertThat(dto.getStatus()).isEqualTo(oppgaveOppdatering.getStatus());
        assertThat(dto.getTilordnetRessurs()).isEqualTo(oppgaveOppdatering.getTilordnetRessurs());
        assertThat(dto.getFristFerdigstillelse()).isEqualTo(nå);
    }

    @Test
    public void oppdaterOppgave_ingenFelterSattEksisterendeBeskrivelse_verifiserFelterSatt() throws FunksjonellException, TekniskException {
        final String oppgaveID = "2222";
        OppgaveDto oppgave = GsakService.oppgaveMappingDomainTilDto(lagOppgave());
        oppgave.setBeskrivelse("test");
        when(oppgaveConsumer.hentOppgave(eq(oppgaveID))).thenReturn(oppgave);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder().build();
        gsakService.oppdaterOppgave(oppgaveID, oppgaveOppdatering);
        verify(oppgaveConsumer).oppdaterOppgave(oppgaveDtoCaptor.capture());

        OppgaveDto dto = oppgaveDtoCaptor.getValue();
        assertThat(dto.getPrioritet()).isEqualTo(oppgave.getPrioritet());
        assertThat(dto.getBeskrivelse()).isEqualTo(oppgave.getBeskrivelse());
        assertThat(dto.getStatus()).isEqualTo(oppgave.getStatus());
        assertThat(dto.getTilordnetRessurs()).isEqualTo(oppgave.getTilordnetRessurs());
        assertThat(dto.getFristFerdigstillelse()).isEqualTo(oppgave.getFristFerdigstillelse());
    }

    @Test
    public void mapDtoTilDomainTilDto() {
        Oppgave oppgave = lagOppgave();
        OppgaveDto oppgaveDto = GsakService.oppgaveMappingDomainTilDto(oppgave);
        assertThat(oppgaveDto).hasNoNullFieldsOrProperties();
        Oppgave oppgaveMappetTilbake = GsakService.oppgaveMappingDtoTilDomain(oppgaveDto);
        assertThat(oppgaveMappetTilbake).isEqualToComparingFieldByField(oppgave);
    }

    @Test
    public void mapBehandlingstypeTilFelleskodeTilBehandlingstype() {
        EnumSet<Behandlingstyper> behandlingstyper = EnumSet.complementOf(EnumSet.of(ANMODNING_OM_UNNTAK_HOVEDREGEL, ØVRIGE_SED));

        for (Behandlingstyper behandlingstype : behandlingstyper) {
            Behandlingstyper mappetType = GsakService.hentBehandlingstyper(GsakService.hentFellesKode(behandlingstype));
            assertThat(mappetType).isEqualTo(behandlingstype);
        }
    }

    private Oppgave lagOppgave() {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setAktivDato(LocalDate.now());
        oppgaveBuilder.setAktørId("aktoer123");
        oppgaveBuilder.setBehandlingstype(Behandlingstyper.SOEKNAD);
        oppgaveBuilder.setBehandlingstema(Behandlingstema.EU_EOS);
        oppgaveBuilder.setBeskrivelse("bla bla");
        oppgaveBuilder.setOpprettetTidspunkt(LocalDateTime.now());
        oppgaveBuilder.setFristFerdigstillelse(LocalDate.now().plusMonths(1L));
        oppgaveBuilder.setOppgaveId("123");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setJournalpostId("journalpost123");
        oppgaveBuilder.setPrioritet(PrioritetType.NORM);
        oppgaveBuilder.setSaksnummer("sak123");
        oppgaveBuilder.setStatus("tildet");
        oppgaveBuilder.setTema(Tema.MED);
        oppgaveBuilder.setTemagruppe("temagruppe");
        oppgaveBuilder.setTildeltEnhetsnr("4530");
        oppgaveBuilder.setTilordnetRessurs("ressurs123");

        return oppgaveBuilder.build();
    }
}
