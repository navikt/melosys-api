package no.nav.melosys.saksflyt.steg.ul;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.eessi.BucType.LA_BUC_02;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.UL_DISTRIBUER_JOURNALPOST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UtpekAnnetLandSendSedTest {

    @Mock
    BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    LandvelgerService landvelgerService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    private UtpekAnnetLandSendSed utpekAnnetLandSendSed;

    private List<Landkoder> trygdemyndighetsland = List.of(Landkoder.SE);

    @Before
    public void settOpp() throws MelosysException {
        utpekAnnetLandSendSed = spy(new UtpekAnnetLandSendSed(behandlingsresultatService, eessiService, fagsakService,
            joarkFasade, landvelgerService, tpsFasade, utenlandskMyndighetService, prosessinstansRepository));

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.landkode = Landkoder.SE;
        utenlandskMyndighet.navn = "Sverige";

        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(trygdemyndighetsland);
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(Landkoder.SE)).thenReturn(utenlandskMyndighet);
        when(joarkFasade.opprettJournalpost(any(OpprettJournalpost.class), anyBoolean())).thenReturn("journalpostId");
    }

    @Test
    public void utfør_sendSed() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of("SE:001", "SE:002"));

        when(eessiService.landErEessiReady(anyString(), eq("SE"))).thenReturn(true);

        utpekAnnetLandSendSed.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(123L, List.of("SE:001", "SE:002"), LA_BUC_02, null);

        assertThat(prosessinstans.getSteg()).isEqualTo(FERDIG);
    }

    @Test
    public void utfør_sendBrev() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of("SE:001", "SE:002"));
        prosessinstans.setData(ProsessDataKey.UTPEKT_LAND, Landkoder.SE);

        when(eessiService.landErEessiReady(anyString(), eq("SE"))).thenReturn(false);

        utpekAnnetLandSendSed.utfør(prosessinstans);

        verify(utpekAnnetLandSendSed).sendBrev(prosessinstans);

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
