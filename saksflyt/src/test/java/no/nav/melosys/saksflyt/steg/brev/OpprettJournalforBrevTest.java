package no.nav.melosys.saksflyt.steg.brev;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.junit.jupiter.api.Assertions.*;
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

    @Captor
    private ArgumentCaptor<DokgenBrevbestilling> brevbestillingCaptor;

    private OpprettJournalforBrev opprettJournalforBrev;

    @BeforeEach
    void init() {
        opprettJournalforBrev = new OpprettJournalforBrev(mockBehandlingService, mockDokgenService, mockJoarkFasade, mockEregFasade, mockKontaktopplysningService);
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
        when(mockDokgenService.produserBrev(any())).thenReturn("pdf".getBytes());
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.PRODUSERBART_BREV, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        Aktoer value = new Aktoer();
        value.setRolle(Aktoersroller.BRUKER);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, value);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(brevbestillingCaptor.capture());
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());

        DokgenBrevbestilling brevbestilling = brevbestillingCaptor.getValue();
        assertEquals(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, brevbestilling.getProduserbartdokument());
        assertEquals(behandling, brevbestilling.getBehandling());
        assertNull(brevbestilling.getOrg());
        assertNull(brevbestilling.getKontaktopplysning());
    }

    @Test
    void utførOpprettJournalforBrevTilRepresentant() throws Exception {
        Behandling behandling = TestdataFactory.lagBehandling();
        OrganisasjonDokument organisasjonDokument = TestdataFactory.lagOrg();
        Kontaktopplysning kontaktopplysning = TestdataFactory.lagKontaktOpplysning();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(organisasjonDokument);

        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockDokgenService.produserBrev(any())).thenReturn("pdf".getBytes());
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(saksopplysning);
        when(mockKontaktopplysningService.hentKontaktopplysning(any(), any())).thenReturn(Optional.of(kontaktopplysning));

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.PRODUSERBART_BREV, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        Aktoer value = new Aktoer();
        value.setRolle(Aktoersroller.REPRESENTANT);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, value);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(brevbestillingCaptor.capture());
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());

        DokgenBrevbestilling brevbestilling = brevbestillingCaptor.getValue();
        assertEquals(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, brevbestilling.getProduserbartdokument());
        assertEquals(behandling, brevbestilling.getBehandling());
        assertEquals(organisasjonDokument, brevbestilling.getOrg());
        assertEquals(kontaktopplysning, brevbestilling.getKontaktopplysning());
    }


}