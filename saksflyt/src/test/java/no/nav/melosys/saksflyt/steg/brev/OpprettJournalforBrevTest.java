package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private PersondataFasade mockPersondataFasade;

    private OpprettJournalforBrev opprettJournalforBrev;

    @BeforeEach
    void init() {
        opprettJournalforBrev = new OpprettJournalforBrev(mockBehandlingService, mockDokgenService,
            mockJoarkFasade, mockPersondataFasade, mockEregFasade);
    }

    @Test
    void utførFeilerVedManglendeBehandling() {
        assertThatThrownBy(() -> opprettJournalforBrev.utfør(new Prosessinstans()))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Prosessinstans mangler behandling");
    }

    @Test
    void utførFeilerVedManglendeMottaker() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling());

        assertThatThrownBy(() -> opprettJournalforBrev.utfør(prosessinstans))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Mangler mottaker");
    }

    @Test
    void utførOpprettJournalforBrevTilBruker() throws Exception {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        mottaker.setAktørId("1234");

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker.getRolle());
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, mottaker.getAktørId());

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(DokgenBrevbestilling.class));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforBrevTilRepresentant() throws Exception {
        Behandling behandling = TestdataFactory.lagBehandling();

        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockEregFasade.hentOrganisasjonNavn(any())).thenReturn("Advokatene AS");

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setOrgnr("987654321");

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker.getRolle());
        prosessinstans.setData(ProsessDataKey.ORGNR, mottaker.getOrgnr());

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(DokgenBrevbestilling.class));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

}