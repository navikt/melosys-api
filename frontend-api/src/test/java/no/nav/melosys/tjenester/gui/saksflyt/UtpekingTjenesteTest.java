package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;

import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingAvvisDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UtpekingTjenesteTest extends JsonSchemaTestParent {

    private static final String UTPEKING_AVVIS_POST_SCHEMA = "saksflyt-utpeking-avvis-post-schema.json";

    @Mock
    private UtpekingService utpekingService;
    @Mock
    private TilgangService tilgangService;
    private UtpekingTjeneste utpekingTjeneste;

    @Before
    public void settOpp() {
        utpekingTjeneste = new UtpekingTjeneste(utpekingService, tilgangService);
    }

    @Test
    public void avvisUtpeking() throws IOException, IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

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