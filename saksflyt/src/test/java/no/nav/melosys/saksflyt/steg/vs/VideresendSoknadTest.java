package no.nav.melosys.saksflyt.steg.vs;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private FagsakService fagsakService;

    private VideresendSoknad videresendSoknad;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @Before
    public void setup() throws IkkeFunnetException {
        videresendSoknad = new VideresendSoknad(eessiService, behandlingsresultatService, landvelgerService, tpsFasade, utenlandskMyndighetService, joarkFasade, fagsakService);
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
        Prosessinstans prosessinstans = opprettProsessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of("SE:123"));

        Behandling behandling = prosessinstans.getBehandling();
        Long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        byte[] vedlegg = new byte[10];

        when(joarkFasade.hentDokument(eq(behandling.getInitierendeJournalpostId()), eq(behandling.getInitierendeDokumentId())))
            .thenReturn(vedlegg);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        videresendSoknad.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(eq(behandlingID), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_03), eq(vedlegg));
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
        behandlingsresultat.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(utenlandskMyndighetService.hentUtenlandskMyndighet(any())).thenReturn(new UtenlandskMyndighet());
        when(fagsakService.hentFagsak(any())).thenReturn(lagFagsak());

        videresendSoknad.utfør(prosessinstans);

        verify(joarkFasade).opprettJournalpost(any(), anyBoolean());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.VS_DISTRIBUER_JOURNALPOST);
    }

    private static Prosessinstans opprettProsessinstans() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setInitierendeJournalpostId("1");
        behandling.setInitierendeDokumentId("1");
        behandling.setFagsak(lagFagsak());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.VS_SEND_SOKNAD);
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }

    private static Fagsak lagFagsak() {
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

        return fagsak;
    }
}