package no.nav.melosys.service.dokument;

import java.time.Instant;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalResolver;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singleton;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenServiceTest {

    @Mock
    private DokgenConsumer mockDokgenConsumer;

    @Mock
    private JoarkFasade mockJoarkFasade;

    @Mock
    private KodeverkService mockKodeverkService;

    private final FakeUnleash unleash = new FakeUnleash();

    private DokgenService dokgenService;

    private final byte[] expectedPdf = "pdf".getBytes();

    @BeforeEach
    void init() {
        dokgenService = new DokgenService(mockDokgenConsumer, new DokgenMalResolver(unleash), mockJoarkFasade, new DokgenMalMapper(mockKodeverkService));
    }

    @Test
    void produserBrevFeilerUtilgjengeligMal() {
        assertThrows(FunksjonellException.class, () -> dokgenService.produserBrev(ATTEST_A1, lagBehandling()));
    }

    @Test
    void produserBrevOk() throws Exception {
        when(mockKodeverkService.dekod(any(), any(), any())).thenReturn("Andeby");
        when(mockJoarkFasade.hentInstantMottaksDatoForJournalpost(any())).thenReturn(Instant.now());
        when(mockDokgenConsumer.lagPdf(anyString(), any())).thenReturn(expectedPdf);

        byte[] pdfResponse = dokgenService.produserBrev(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, lagBehandling());

        assertNotNull(pdfResponse);
        assertEquals(expectedPdf, pdfResponse);
    }

    @Test
    void erTilgjengeligDokgenmal() {
        unleash.enableAll();

        assertTrue(dokgenService.erTilgjengeligDokgenmal(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD));
        assertFalse(dokgenService.erTilgjengeligDokgenmal(ATTEST_A1));
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setRegistrertDato(Instant.now());
        return fagsak;
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