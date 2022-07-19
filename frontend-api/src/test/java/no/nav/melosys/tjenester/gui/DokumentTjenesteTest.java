package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.brev.BrevbestillingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DokumentTjenesteTest {
    private static final Logger log = LoggerFactory.getLogger(DokumentTjenesteTest.class);

    private DokumentTjeneste dokumentTjeneste;

    @Mock
    private DokumentServiceFasade dokumentServiceFasade;
    @Mock
    private DokumentHentingService dokumentHentingService;
    @Mock
    private EessiService eessiService;
    @Mock
    private Aksesskontroll aksesskontroll;

    @BeforeEach
    public void setUp() {
        dokumentTjeneste = new DokumentTjeneste(dokumentServiceFasade, dokumentHentingService, eessiService, aksesskontroll);
    }

    @Test
    void produserDokument() {
        BrevbestillingDto brevBestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        dokumentTjeneste.produserDokument(1L,
            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, brevBestillingDto);

        verify(dokumentServiceFasade).produserUtkast(anyLong(), any());
        verify(dokumentServiceFasade).produserDokument(anyLong(), any());
    }

    @Test
    void produserDokumentFeilerMedManglendeMottaker() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dokumentTjeneste.produserDokument(1L,
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, new BrevbestillingDto()))
            .withMessageContaining("Mottaker trengs for å bestille");
    }

}
