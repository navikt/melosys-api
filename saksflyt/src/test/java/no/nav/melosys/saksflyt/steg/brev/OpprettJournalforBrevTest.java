package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettJournalforBrevTest {

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private DokgenService mockDokgenService;

    @Mock
    private JoarkFasade mockJoarkFasade;

    @Mock
    private EregFasade mockEregFasade;

    @Mock
    private TpsFasade mockTpsFasade;

    private OpprettJournalforBrev opprettJournalforBrev;

    @BeforeEach
    void init() {
        opprettJournalforBrev = new OpprettJournalforBrev(mockBehandlingService, mockDokgenService,
            mockJoarkFasade, mockTpsFasade, mockEregFasade);
    }

    @Test
    void utførFeilerVedManglendeBehandling() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThrows(FunksjonellException.class, () -> opprettJournalforBrev.utfør(prosessinstans));
    }

    @Test
    void utførFeilerVedManglendeMottaker() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        assertThrows(FunksjonellException.class, () -> opprettJournalforBrev.utfør(prosessinstans));
    }

    @Test
    void utførOpprettJournalforBrevTilBruker() throws Exception {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.PRODUSERBART_BREV, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);

        prosessinstans.setData(ProsessDataKey.MOTTAKER, Aktoersroller.BRUKER);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "1234");

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(eq(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD), eq(behandling.getId()), any(), any());
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforBrevTilRepresentant() throws Exception {
        Behandling behandling = TestdataFactory.lagBehandling();

        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockEregFasade.hentOrganisasjonNavn(any())).thenReturn("Advokatene AS");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.PRODUSERBART_BREV, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, Aktoersroller.REPRESENTANT);
        prosessinstans.setData(ProsessDataKey.ORGNR, "12345");

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(eq(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD), eq(behandling.getId()), any(), any());
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }


}