package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettJournalforBrevTest {

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private DokgenService dokgenService;

    @Mock
    private JoarkFasade joarkFasade;

    @InjectMocks
    private OpprettJournalforBrev opprettJournalforBrev;

    @Test
    void utførFeilerMedManglendeBehandling() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThrows(FunksjonellException.class, () -> opprettJournalforBrev.utfør(prosessinstans));
    }

    @Test
    void utførOpprettJournalforBrev() throws Exception {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(dokgenService.produserBrev(any(), any())).thenReturn("pdf".getBytes());
        when(joarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");

        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = lagBehandling();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.PRODUSERBART_BREV, Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(behandlingService).hentBehandling(anyLong());
        verify(dokgenService).produserBrev(any(), any());
        verify(joarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        return behandling;
    }

    private Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99887766554";
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

}