package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KontaktopplysningTjenesteTest {

    private KontaktopplysningTjeneste kontaktopplysningTjeneste;

    @Mock
    private KontaktopplysningService kontaktopplysningService;

    private static final String SAK_NUMMER = "MEL-1";
    private static final String ORG_NUMMER = "999";

    @Before
    public void setUp() {
        kontaktopplysningTjeneste = new KontaktopplysningTjeneste(kontaktopplysningService);
    }

    @Test
    public void hentKontaktopplysning_kallerPåService_objektFinnes() {
        doReturn(Optional.of(Kontaktopplysning.class)).when(kontaktopplysningService).hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        Response response = kontaktopplysningTjeneste.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void hentKontaktopplysning_kallerPåService_objektFinnesIkke() {
        Response response = kontaktopplysningTjeneste.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        verify(kontaktopplysningService).hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void lagKontaktopplysning_kallerPåService() {
        KontaktInfoDto kontaktInfoDto = new KontaktInfoDto();
        kontaktInfoDto.setKontaktnavn("kontaktnavn");
        kontaktInfoDto.setKontaktorgnr("kontaktorgnr");
        Response response = kontaktopplysningTjeneste.lagKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktInfoDto);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(SAK_NUMMER, ORG_NUMMER, kontaktInfoDto.getKontaktorgnr(), kontaktInfoDto.getKontaktnavn());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void slettKontaktopplysning_kallerPåService() throws FunksjonellException {
        Response response = kontaktopplysningTjeneste.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        verify(kontaktopplysningService).slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
