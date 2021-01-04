package no.nav.melosys.integrasjon.oppgave;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.MelosysException;
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
public final class OppgaveFasadeImplTest {
    @Mock
    private OppgaveConsumer oppgaveConsumer;
    @Captor
    private ArgumentCaptor<OpprettOppgaveDto> opprettOppgaveDtoCaptor;
    @Captor
    private ArgumentCaptor<OppgaveSearchRequest> oppgaveSearchRequestCaptor;

    private OppgaveFasadeImpl oppgaveFasadeImpl;

    @BeforeEach
    public void setup() {
        oppgaveFasadeImpl = new OppgaveFasadeImpl(oppgaveConsumer);
    }

    @Test
    void opprettOppgave_vurderDokument_setterData() throws Exception {
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
    void opprettOppgave_gyldigOppgave_validerDto() throws Exception {
        Oppgave oppgave = lagOppgave();

        oppgaveFasadeImpl.opprettOppgave(oppgave);
        verify(oppgaveConsumer).opprettOppgave(opprettOppgaveDtoCaptor.capture());

        OpprettOppgaveDto oppgaveDto = opprettOppgaveDtoCaptor.getValue();

        assertThat(oppgaveDto).isNotNull();
        assertThat(oppgaveDto.getJournalpostId()).isEqualTo(oppgave.getJournalpostId());
        assertThat(oppgaveDto.getAktørId()).isEqualTo(oppgave.getAktørId());
        assertThat(oppgaveDto.getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(oppgaveDto.getBehandlingstype()).isEqualTo(oppgave.getBehandlingstype());
        assertThat(oppgaveDto.getBeskrivelse()).isEqualTo("bla bla");
        assertThat(oppgaveDto.getOppgavetype()).isEqualTo(oppgave.getOppgavetype().getKode());
        assertThat(oppgaveDto.getPrioritet()).isEqualTo(PrioritetType.NORM.toString());
        assertThat(oppgaveDto.getTema()).isEqualTo(oppgave.getTema().getKode());
        assertThat(oppgaveDto.getTildeltEnhetsnr()).isEqualTo(Integer.toString(Konstanter.MELOSYS_ENHET_ID));
        assertThat(oppgaveDto.getTilordnetRessurs()).isEqualTo(oppgave.getTilordnetRessurs());
    }

    @Test
    void finnOppgaveListeMedAnsvarlig_gyldigOppgave_verifiserToKallMotOppgave() throws Exception {
        OppgaveDto oppgaveDto = new OppgaveDto();
        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class)))
            .thenReturn(Collections.singletonList(oppgaveDto));

        oppgaveFasadeImpl.finnOppgaverMedAnsvarlig("123");
        verify(oppgaveConsumer, times(2)).hentOppgaveListe(oppgaveSearchRequestCaptor.capture());

        List<OppgaveSearchRequest> requests = oppgaveSearchRequestCaptor.getAllValues();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0).getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(requests.get(1).getBehandlesAvApplikasjon()).isNullOrEmpty();
        assertThat(requests.get(1).getOppgavetype()[0]).isEqualTo(Oppgavetyper.JFR.getKode());
    }

    @Test
    void finnOppgaveListeMedAnsvarlig_toDuplikateOppgaver_filtrererUtDuplikater() throws Exception {
        final String oppgaveID = "123duplikat";

        OppgaveDto oppgaveDto1 = new OppgaveDto();
        oppgaveDto1.setId(oppgaveID);
        OppgaveDto oppgaveDto2 = new OppgaveDto();
        oppgaveDto2.setId(oppgaveID);

        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class))).thenReturn(Arrays.asList(oppgaveDto1, oppgaveDto2));

        Set<Oppgave> oppgaver = oppgaveFasadeImpl.finnOppgaverMedAnsvarlig("123");

        assertThat(oppgaver.size()).isEqualTo(1);
        assertThat(oppgaver.iterator().next().getOppgaveId()).isEqualTo(oppgaveID);
    }

    @Test
    void testMappingMellomDTOogDomainForOppgave() throws MelosysException {
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
    void finnUtildelteOppgaverEtterFrist_mottarBehandlingsOppgaveUtenSaksreferanse_returnererGyldigeOppgaver() throws Exception {
        OppgaveDto jfrOppgave = new OppgaveDto();
        jfrOppgave.setOppgavetype("JFR");
        OppgaveDto behOppgave = new OppgaveDto();
        behOppgave.setSaksreferanse("MEL-123");
        OppgaveDto ikkeGyldigOppgave = new OppgaveDto();

        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class)))
            .thenReturn(List.of(jfrOppgave, behOppgave, ikkeGyldigOppgave));

        List<Oppgave> oppgaver = oppgaveFasadeImpl.finnUtildelteOppgaverEtterFrist("abbehandlingstema1234");

        assertThat(oppgaver.size()).isEqualTo(2);
        assertThat(oppgaver.get(0).getOppgavetype()).isEqualTo(Oppgavetyper.JFR);
        assertThat(oppgaver.get(1).getSaksnummer()).isEqualTo("MEL-123");
    }

    private Oppgave lagOppgave() {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setAktivDato(LocalDate.now());
        oppgaveBuilder.setAktørId("aktoer123");
        oppgaveBuilder.setBehandlingstype("aebehandlingstype1234");
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

        return oppgaveBuilder.build();
    }
}
