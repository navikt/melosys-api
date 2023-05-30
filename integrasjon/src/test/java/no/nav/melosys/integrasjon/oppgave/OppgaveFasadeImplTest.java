package no.nav.melosys.integrasjon.oppgave;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class OppgaveFasadeImplTest {
    @Mock
    private OppgaveConsumer oppgaveConsumer;
    @Captor
    private ArgumentCaptor<OpprettOppgaveDto> opprettOppgaveDtoCaptor;
    @Captor
    private ArgumentCaptor<OppgaveSearchRequest> oppgaveSearchRequestCaptor;
    @Captor
    private ArgumentCaptor<OppgaveDto> oppgaveDtoArgumentCaptor;

    private OppgaveFasadeImpl oppgaveFasadeImpl;

    @BeforeEach
    void setup() {
        oppgaveFasadeImpl = new OppgaveFasadeImpl(oppgaveConsumer);
    }

    @Test
    void opprettOppgave_vurderDokument_setterData() {
        final String behandlingstema = "ae9999";
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.VUR)
            .setTema(Tema.MED)
            .setBehandlingstema(behandlingstema)
            .setFristFerdigstillelse(LocalDate.now());
        oppgaveFasadeImpl.opprettOppgave(oppgaveBuilder.build());

        ArgumentCaptor<OpprettOppgaveDto> captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveConsumer).opprettOppgave(captor.capture());
        OpprettOppgaveDto opprettOppgaveDto = captor.getValue();

        assertThat(opprettOppgaveDto.getOppgavetype()).isEqualTo(Oppgavetyper.VUR.getKode());
        assertThat(opprettOppgaveDto.getBehandlingstema()).isEqualTo(behandlingstema);
        assertThat(opprettOppgaveDto.getFristFerdigstillelse()).isNotNull();
    }

    @Test
    void opprettOppgave_gyldigOppgave_validerDto() {
        Oppgave oppgave = lagOppgave();

        oppgaveFasadeImpl.opprettOppgave(oppgave);
        verify(oppgaveConsumer).opprettOppgave(opprettOppgaveDtoCaptor.capture());

        OpprettOppgaveDto oppgaveDto = opprettOppgaveDtoCaptor.getValue();

        assertThat(oppgaveDto).isNotNull();
        assertThat(oppgaveDto.getJournalpostId()).isEqualTo(oppgave.getJournalpostId());
        assertThat(oppgaveDto.getAktørId()).isEqualTo(oppgave.getAktørId());
        assertThat(oppgaveDto.getOrgnr()).isEqualTo(oppgave.getOrgnr());
        assertThat(oppgaveDto.getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(oppgaveDto.getBeskrivelse()).isEqualTo(OppgaveFasadeImpl.hentNyBeskrivelseHendelseslogg("bla bla", "sak123"));
        assertThat(oppgaveDto.getOppgavetype()).isEqualTo(oppgave.getOppgavetype().getKode());
        assertThat(oppgaveDto.getPrioritet()).isEqualTo(PrioritetType.NORM.toString());
        assertThat(oppgaveDto.getTema()).isEqualTo(oppgave.getTema().getKode());
        assertThat(oppgaveDto.getTildeltEnhetsnr()).isEqualTo(Integer.toString(Konstanter.MELOSYS_ENHET_ID));
        assertThat(oppgaveDto.getTilordnetRessurs()).isEqualTo(oppgave.getTilordnetRessurs());
    }

    @Test
    void finnOppgaveListeMedAnsvarlig_gyldigOppgave_verifiserToKallMotOppgave() {
        OppgaveDto oppgaveDto = new OppgaveDto();
        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class)))
            .thenReturn(Collections.singletonList(oppgaveDto));

        oppgaveFasadeImpl.finnOppgaverMedAnsvarlig("123");
        verify(oppgaveConsumer, times(2)).hentOppgaveListe(oppgaveSearchRequestCaptor.capture());

        List<OppgaveSearchRequest> requests = oppgaveSearchRequestCaptor.getAllValues();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(requests.get(1).getBehandlesAvApplikasjon()).isNullOrEmpty();
        assertThat(requests.get(1).getOppgavetype()[0]).isEqualTo(Oppgavetyper.JFR.getKode());
    }

    @Test
    void finnOppgaveListeMedAnsvarlig_toDuplikateOppgaver_filtrererUtDuplikater() {
        final String oppgaveID = "123duplikat";

        OppgaveDto oppgaveDto1 = new OppgaveDto();
        oppgaveDto1.setId(oppgaveID);
        OppgaveDto oppgaveDto2 = new OppgaveDto();
        oppgaveDto2.setId(oppgaveID);

        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class))).thenReturn(Arrays.asList(oppgaveDto1, oppgaveDto2));

        Set<Oppgave> oppgaver = oppgaveFasadeImpl.finnOppgaverMedAnsvarlig("123");

        assertThat(oppgaver).hasSize(1);
        assertThat(oppgaver.iterator().next().getOppgaveId()).isEqualTo(oppgaveID);
    }

    @Test
    void testMappingMellomDTOogDomainForOppgave() {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setId("1234");
        oppgaveDto.setSaksreferanse("456");
        oppgaveDto.setOppgavetype("BEH_SAK_MK");
        oppgaveDto.setTema("MED");
        oppgaveDto.setSaksreferanse("MEL-111");

        when(oppgaveConsumer.hentOppgave("1234")).thenReturn(oppgaveDto);
        Oppgave oppgave = oppgaveFasadeImpl.hentOppgave("1234");
        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(oppgave.getSaksnummer()).isEqualTo("MEL-111");
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetyper.valueOf("BEH_SAK_MK"));
        assertThat(oppgave.getTema()).isEqualTo(Tema.valueOf("MED"));
    }

    @Test
    void finnUtildelteOppgaverEtterFrist_mottarOppgaveMedOgUtenSaksreferanse_returnererOppgaveMedSaksreferanse() {
        OppgaveDto jfrOppgave = new OppgaveDto();
        jfrOppgave.setOppgavetype("JFR");
        OppgaveDto behOppgave = new OppgaveDto();
        behOppgave.setSaksreferanse("MEL-123");

        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class)))
            .thenReturn(List.of(jfrOppgave, behOppgave));

        List<Oppgave> oppgaver =
            oppgaveFasadeImpl.finnUtildelteOppgaverEtterFrist(null);

        assertThat(oppgaver).hasSize(1);
        assertThat(oppgaver.get(0).getSaksnummer()).isEqualTo("MEL-123");
    }

    @Test
    void oppdaterOppgave_mapperOppgaveOppdateringTilOppgaveDtoRiktig() {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setMappeId("321");
        when(oppgaveConsumer.hentOppgave("123")).thenReturn(oppgaveDto);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .oppgavetype(Oppgavetyper.JFR)
            .tema(Tema.MED)
            .behandlesAvApplikasjon(Fagsystem.MELOSYS)
            .saksnummer("saksnr")
            .behandlingstema("behandlingstema")
            .behandlingstype("behandlingstype")
            .prioritet("prioritet #1")
            .status("heeelt ferdig")
            .tilordnetRessurs("Z133337")
            .fristFerdigstillelse(LocalDate.now())
            .build();


        oppgaveFasadeImpl.oppdaterOppgave("123", oppgaveOppdatering);


        verify(oppgaveConsumer).oppdaterOppgave(oppgaveDtoArgumentCaptor.capture());
        assertThat(oppgaveDtoArgumentCaptor.getValue()).extracting(
            OppgaveDto::getOppgavetype,
            OppgaveDto::getTema,
            OppgaveDto::getBehandlesAvApplikasjon,
            OppgaveDto::getSaksreferanse,
            OppgaveDto::getBehandlingstema,
            OppgaveDto::getBehandlingstype,
            OppgaveDto::getPrioritet,
            OppgaveDto::getStatus,
            OppgaveDto::getTilordnetRessurs,
            OppgaveDto::getFristFerdigstillelse,
            OppgaveDto::getMappeId
            )
            .contains(Oppgavetyper.JFR.getKode(),
                Tema.MED.getKode(),
                Fagsystem.MELOSYS.getKode(),
                "saksnr",
                "behandlingstema",
                "behandlingstype",
                "prioritet #1",
                "heeelt ferdig",
                "Z133337",
                "321",
                LocalDate.now());
    }

    @Test
    void oppdaterOppgave_formatererBeskrivelsesloggRiktig_nårBeskrivelseEksisterer() {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setBeskrivelse("Testy test");
        when(oppgaveConsumer.hentOppgave("123")).thenReturn(oppgaveDto);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .behandlingstema("UTSENDT_ARBEIDSTAKER")
            .beskrivelse("Ny beskrivelse")
            .saksnummer("MEL-123")
            .build();


        oppgaveFasadeImpl.oppdaterOppgave("123", oppgaveOppdatering);


        verify(oppgaveConsumer).oppdaterOppgave(oppgaveDtoArgumentCaptor.capture());
        String oppdateringstidspunkt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        assertThat(oppgaveDtoArgumentCaptor.getValue().getBeskrivelse())
            .isEqualTo(String.format("--- %s (%s, %s) ---\n %s\n\nTesty test",
                oppdateringstidspunkt, "srvmelosys", Fagsystem.MELOSYS.getBeskrivelse(), "Ny beskrivelse - MEL-123"));
    }

    @Test
    void oppdaterOppgave_FormatererBeskrivelsesloggRiktig_nårBeskrivelseIkkeEksisterer() {
        OppgaveDto oppgaveDto = new OppgaveDto();
        when(oppgaveConsumer.hentOppgave("123")).thenReturn(oppgaveDto);

        OppgaveOppdatering oppgaveOppdatering = OppgaveOppdatering.builder()
            .behandlingstema("UTSENDT_ARBEIDSTAKER")
            .beskrivelse("Ny beskrivelse")
            .saksnummer("MEL-123")
            .build();


        oppgaveFasadeImpl.oppdaterOppgave("123", oppgaveOppdatering);


        verify(oppgaveConsumer).oppdaterOppgave(oppgaveDtoArgumentCaptor.capture());
        String oppdateringstidspunkt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        assertThat(oppgaveDtoArgumentCaptor.getValue().getBeskrivelse())
            .isEqualTo(String.format("--- %s (%s, %s) ---\n %s\n",
                oppdateringstidspunkt, "srvmelosys", "Melosys", "Ny beskrivelse - MEL-123"));
    }

    private Oppgave lagOppgave() {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setAktivDato(LocalDate.now());
        oppgaveBuilder.setAktørId("aktoer123");
        oppgaveBuilder.setAktørId("orgnr");
        oppgaveBuilder.setBehandlingstema("abbehandlingstema1234");
        oppgaveBuilder.setBeskrivelse("bla bla");
        oppgaveBuilder.setOpprettetTidspunkt(ZonedDateTime.now());
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
        oppgaveBuilder.setMappeId("321");

        return oppgaveBuilder.build();
    }
}
