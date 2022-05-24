package no.nav.melosys.tjenester.gui;

import java.util.Collections;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.kontroll.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class KontrollTjenesteTest extends JsonSchemaTestParent {

    private final long BEHANDLING_ID = 4;

    @Mock
    private FerdigbehandlingKontrollService mockFerdigbehandlingKontrollService;
    @Mock
    private Aksesskontroll mockAksesskontroll;

    private KontrollTjeneste kontrollTjeneste;

    @BeforeEach
    public void setUp() {
        kontrollTjeneste = new KontrollTjeneste(mockFerdigbehandlingKontrollService, mockAksesskontroll);
    }

    @Test
    void kontrollerFerdigbehandling_ingenFeilmeldinger_ingentingSkjer() throws ValideringException {
        kontrollTjeneste.kontrollerFerdigbehandling(BEHANDLING_ID, true, lagFattVedtakDto());
    }

    @Test
    void kontrollerFerdigbehandling_feilmeldinger_kasterExceptions() throws ValideringException {
        doThrow(new ValideringException("melding", Collections.emptyList()))
            .when(mockFerdigbehandlingKontrollService).kontroller(BEHANDLING_ID, true, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);

        assertThatThrownBy(() -> kontrollTjeneste.kontrollerFerdigbehandling(BEHANDLING_ID, true, lagFattVedtakDto()))
            .isInstanceOf(ValideringException.class)
            .hasMessage("melding");
    }

    private FattVedtakDto lagFattVedtakDto() {
        var fattVedtak = new FattVedtakDto();
        fattVedtak.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        fattVedtak.setBehandlingsresultatTypeKode(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
        return fattVedtak;
    }
}

