package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singleton;
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
    private KontaktopplysningService mockKontaktopplysningService;

    private OpprettJournalforBrev opprettJournalforBrev;

    @BeforeEach
    void init() {
        opprettJournalforBrev = new OpprettJournalforBrev(mockBehandlingService, mockDokgenService, mockJoarkFasade, mockEregFasade, mockKontaktopplysningService);
    }

    @Test
    void utførFeilerMedManglendeBehandling() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThrows(FunksjonellException.class, () -> opprettJournalforBrev.utfør(prosessinstans));
    }

    @Test
    void utførOpprettJournalforBrev() throws Exception {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockDokgenService.produserBrev(any())).thenReturn("pdf".getBytes());
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.PRODUSERBART_BREV, Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any());
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }
}