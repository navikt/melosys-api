package no.nav.melosys.saksflyt.steg.ul;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.eessi.BucType.LA_BUC_02;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_DISTRIBUER_JOURNALPOST;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_OPPDATER_BEHANDLINGSRESULTAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UtpekAnnetLandSendUtlandTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    private UtpekAnnetLandSendUtland utpekAnnetLandSendUtland;

    @Before
    public void settOpp() throws MelosysException {
        utpekAnnetLandSendUtland = spy(new UtpekAnnetLandSendUtland(behandlingsresultatService, eessiService,
            joarkFasade, tpsFasade, utenlandskMyndighetService));

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.landkode = Landkoder.SE;
        utenlandskMyndighet.navn = "Sverige";

        when(utenlandskMyndighetService.hentUtenlandskMyndighet(Landkoder.SE)).thenReturn(utenlandskMyndighet);
        when(joarkFasade.opprettJournalpost(any(OpprettJournalpost.class), anyBoolean())).thenReturn("journalpostId");
    }

    @Test
    public void utfør_sendSed() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of("SE:001", "SE:002"));

        utpekAnnetLandSendUtland.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(123L, List.of("SE:001", "SE:002"), LA_BUC_02, null, null);

        assertThat(prosessinstans.getSteg()).isEqualTo(UL_OPPDATER_BEHANDLINGSRESULTAT);
    }

    @Test
    public void utfør_sendBrev() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.UTPEKT_LAND, Landkoder.SE);

        utpekAnnetLandSendUtland.utfør(prosessinstans);

        verify(utpekAnnetLandSendUtland).sendBrev(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(UL_DISTRIBUER_JOURNALPOST);
    }

    private Prosessinstans lagProsessinstans() {
        Aktoer bruker = new Aktoer();
        bruker.setAktørId("000");
        bruker.setRolle(Aktoersroller.BRUKER);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(1234L);
        fagsak.setAktører(Set.of(bruker));

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}
