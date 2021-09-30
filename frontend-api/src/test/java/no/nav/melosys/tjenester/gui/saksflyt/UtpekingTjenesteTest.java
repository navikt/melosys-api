package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingAvvisDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UtpekingTjenesteTest extends JsonSchemaTestParent {

    private static final String UTPEKING_AVVIS_POST_SCHEMA = "saksflyt-utpeking-avvis-post-schema.json";

    @Mock
    private UtpekingService utpekingService;
    @Mock
    private Aksesskontroll aksesskontroll;
    private UtpekingTjeneste utpekingTjeneste;

    @BeforeEach
    public void settOpp() {
        utpekingTjeneste = new UtpekingTjeneste(utpekingService, aksesskontroll);
    }

    @Test
    public void avvisUtpeking() throws IOException {

        UtpekingAvvisDto utpekingAvvisDto = new UtpekingAvvisDto();
        utpekingAvvisDto.setFritekst("test");
        utpekingAvvisDto.setNyttLovvalgsland("DK");
        utpekingAvvisDto.setBegrunnelseUtenlandskMyndighet("test");
        utpekingAvvisDto.setVilSendeAnmodningOmMerInformasjon(false);

        valider(utpekingAvvisDto, UTPEKING_AVVIS_POST_SCHEMA);

        utpekingTjeneste.avvisUtpeking(1L, utpekingAvvisDto);

        verify(utpekingService).avvisUtpeking(anyLong(), any(UtpekingAvvis.class));
    }
}
