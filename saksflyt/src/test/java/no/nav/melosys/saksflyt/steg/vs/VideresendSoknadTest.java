package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideresendSoknadTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    @Mock
    private JoarkFasade joarkFasade;

    private VideresendSoknad videresendSoknad;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        videresendSoknad = new VideresendSoknad(eessiService, behandlingsresultatService, landvelgerService, tpsFasade, utenlandskMyndighetRepository, joarkFasade);
    }

    @Test
    public void utfør_journalpostIDFinnesIkke_forventFunksjonellException() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setInitierendeDokumentId("1");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("JournalpostID til behandling " + behandling.getId() + " finnes ikke!");
        videresendSoknad.utfør(prosessinstans);
    }

    @Test
    public void utfør_dokumentIDFinnesIkke_forventFunksjonellException() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setInitierendeJournalpostId("1");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("DokumentID til behandling " + behandling.getId() + " finnes ikke!");
        videresendSoknad.utfør(prosessinstans);
    }

    @Test
    public void utfør_skalSendesUtlandErEessiKlar_senderSedIBuc3() throws MelosysException {
        final long behandlingID = 1L;
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setInitierendeJournalpostId("1");
        behandling.setInitierendeDokumentId("1");

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);

        byte[] vedlegg = new byte[10];

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.VS_SEND_SOKNAD);
        prosessinstans.setBehandling(behandling);

        when(joarkFasade.hentDokument(eq(behandling.getInitierendeJournalpostId()), eq(behandling.getInitierendeDokumentId())))
            .thenReturn(vedlegg);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong()))
            .thenReturn(Collections.singleton(Landkoder.FR));
        when(eessiService.hentEessiMottakerinstitusjoner(eq(BucType.LA_BUC_03.name())))
            .thenReturn(Collections.singletonList(new Institusjon("2", "frankrike", "FR")));

        videresendSoknad.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(eq(behandlingID), eq(BucType.LA_BUC_03), eq(vedlegg));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_STATUS_BEH_AVSL);
    }

    @Test
    public void utfør_skalSendesUtlandErIkkeEessiKlar_senderA008SomBrev() throws MelosysException {
        Prosessinstans prosessinstans = opprettProsessinstans();
        Behandling behandling = prosessinstans.getBehandling();

        byte[] vedlegg = new byte[10];
        when(joarkFasade.hentDokument(eq(behandling.getInitierendeJournalpostId()), eq(behandling.getInitierendeDokumentId())))
            .thenReturn(vedlegg);

        ArkivDokument hoveddokument = new ArkivDokument();
        hoveddokument.setTittel("Tittel");
        Journalpost journalpost = new Journalpost("1");
        journalpost.setHoveddokument(hoveddokument);
        when(joarkFasade.hentJournalpost(anyString())).thenReturn(journalpost);

        when(joarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("2");

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Collections.singleton(Landkoder.SE));
        when(eessiService.hentEessiMottakerinstitusjoner(eq(BucType.LA_BUC_03.name())))
            .thenReturn(Collections.singletonList(new Institusjon("2", "frankrike", "FR")));

        videresendSoknad.utfør(prosessinstans);

        verify(joarkFasade).opprettJournalpost(any(), anyBoolean());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.VS_DISTRIBUER_JOURNALPOST);
    }

    private static Prosessinstans opprettProsessinstans() {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setAktørId("123");
        myndighet.setInstitusjonId("SE:id");

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("321");

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setGsakSaksnummer(1111L);
        fagsak.setAktører(Set.of(myndighet, bruker));

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setInitierendeJournalpostId("1");
        behandling.setInitierendeDokumentId("1");
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.VS_SEND_SOKNAD);
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}