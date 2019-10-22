package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JournalfoeringServiceTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private EessiService eessiService;

    private JournalfoeringService journalfoeringService;
    private JournalfoeringOpprettDto opprettDto;
    private JournalfoeringTilordneDto tilordneDto;
    private Journalpost journalpost;

    @Before
    public void setup() throws SikkerhetsbegrensningException, IntegrasjonException {

        journalpost = new Journalpost("123");
        journalpost.setHoveddokument(new ArkivDokument());
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        this.journalfoeringService = new JournalfoeringService(joarkFasade, oppgaveService, prosessinstansService, eessiService);
        JournalfoeringOpprettDto opprettDto = new JournalfoeringOpprettDto();
        opprettDto.setJournalpostID("setJournalpostID");
        opprettDto.setDokumentID("setDokumentID");
        opprettDto.setOppgaveID("setOppgaveID");
        opprettDto.setAvsenderNavn("setAvsenderNavn");
        opprettDto.setAvsenderID("setAvsenderID");
        opprettDto.setBrukerID("setBrukerID");
        opprettDto.setHoveddokumentTittel("setDokumenttittel");
        opprettDto.setArbeidsgiverID("123456789");
        opprettDto.setBehandlingstypeKode(Behandlingstyper.SOEKNAD.getKode());
        this.opprettDto = opprettDto;

        JournalfoeringTilordneDto tilordneDto = new JournalfoeringTilordneDto();
        tilordneDto.setJournalpostID("setJournalpostID");
        tilordneDto.setDokumentID("setDokumentID");
        tilordneDto.setOppgaveID("setOppgaveID");
        tilordneDto.setAvsenderNavn("setAvsenderNavn");
        tilordneDto.setAvsenderID("setAvsenderID");
        tilordneDto.setBrukerID("setBrukerID");
        tilordneDto.setHoveddokumentTittel("setDokumenttittel");
        this.tilordneDto = tilordneDto;
    }

    @Test
    public void opprettSakOgJournalfør() throws MelosysException {
        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(LocalDate.MIN);
        periode.setTom(LocalDate.MAX);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Arrays.asList("DK"));
        opprettDto.setFagsak(fagsakDto);
        journalfoeringService.opprettOgJournalfør(opprettDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());

    }

    @Test(expected = FunksjonellException.class)
    public void opprettSakOgJournalfør_fomEtterTom_feiler() throws MelosysException {
        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(LocalDate.MAX);
        periode.setTom(LocalDate.MIN);
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Arrays.asList("DK"));
        opprettDto.setFagsak(fagsakDto);
        journalfoeringService.opprettOgJournalfør(opprettDto);

    }

    @Test(expected = FunksjonellException.class)
    public void opprettSakOgJournalfør_oppgaveID_mangler() throws MelosysException {
        opprettDto.setOppgaveID(null);
        journalfoeringService.opprettOgJournalfør(opprettDto);
    }

    @Test
    public void opprettOgJournalfør_erSed_prosessinstansOpprettet() throws MelosysException {
        opprettDto.setBehandlingstypeKode(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.getKode());
        journalpost.setMottaksKanal("EESSI");
        journalpost.getHoveddokument().setNavSkjemaID("A009");
        when(eessiService.støtterAutomatiskBehandling(anyString())).thenReturn(Boolean.TRUE);

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansSedMottak(anyString(), anyString());
    }

    @Test
    public void opprettOgJournalfør_erAnmodningUnntakHovedregel_prosessinstansOpprettet() throws MelosysException {
        AnmodningOmUnntakDto anmodningOmUnntakDto = new AnmodningOmUnntakDto();
        anmodningOmUnntakDto.setLovvalgsbestemmelse(FO_883_2004_ART16_1.getKode());
        anmodningOmUnntakDto.setUnntakFraLovvalgsbestemmelse(FO_883_2004_ART12_1.getKode());
        anmodningOmUnntakDto.setUnntakFraLovvalgsland("DE");
        opprettDto.setAnmodningOmUnntak(anmodningOmUnntakDto);

        FagsakDto fagsakDto = new FagsakDto();
        PeriodeDto periode = new PeriodeDto();
        periode.setFom(LocalDate.now());
        periode.setTom(LocalDate.now().plusYears(1));
        fagsakDto.setSoknadsperiode(periode);
        fagsakDto.setLand(Collections.singletonList("NO"));
        opprettDto.setFagsak(fagsakDto);
        opprettDto.setBehandlingstypeKode("ANMODNING_OM_UNNTAK_HOVEDREGEL");

        journalfoeringService.opprettOgJournalfør(opprettDto);

        ArgumentCaptor<Prosessinstans> captor = ArgumentCaptor.forClass(Prosessinstans.class);
        verify(prosessinstansService).lagre(captor.capture());

        Prosessinstans prosessinstans = captor.getValue();
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.JFR_AOU_BREV);
    }

    @Test
    public void opprettOgJournalfør_erVurderTrygdetid_prosessinstansOpprettet() throws MelosysException {
        opprettDto.setBehandlingstypeKode(Behandlingstyper.VURDER_TRYGDETID.getKode());
        journalpost.setMottaksKanal("EESSI");

        journalfoeringService.opprettOgJournalfør(opprettDto);
        verify(prosessinstansService).opprettProsessinstansGenerellSedBehandling(opprettDto);
    }

    @Test
    public void tilordneSakOgJournalfør() throws FunksjonellException, TekniskException {
        tilordneDto.setSaksnummer("MEL-0123");
        journalfoeringService.tilordneSakOgJournalfør(tilordneDto);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
        verify(oppgaveService).ferdigstillOppgave(anyString());

    }

    @Test(expected = FunksjonellException.class)
    public void tilordneSakOgJournalfør_saksnr_mangler() throws FunksjonellException, TekniskException {
        tilordneDto.setSaksnummer("");
        journalfoeringService.tilordneSakOgJournalfør(tilordneDto);
    }

    @Test
    public void valider() throws FunksjonellException {
        journalfoeringService.valider(opprettDto);
    }

    @Test(expected = FunksjonellException.class)
    public void valider_brukerID_mangler() throws FunksjonellException {
        opprettDto.setBrukerID(null);
        journalfoeringService.valider(opprettDto);
    }
}