package no.nav.melosys.saksflyt.steg.vs;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideresendSoknadTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private JoarkFasade joarkFasade;

    private VideresendSoknad videresendSoknad;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        videresendSoknad = new VideresendSoknad(eessiService, brevBestiller, behandlingsresultatService, landvelgerService, joarkFasade);
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
    }
}