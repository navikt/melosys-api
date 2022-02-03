package no.nav.melosys.tjenester.gui;

import java.util.Optional;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KontaktopplysningTjenesteTest {

    private KontaktopplysningTjeneste kontaktopplysningTjeneste;

    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private static final String SAK_NUMMER = "MEL-1";
    private static final String ORG_NUMMER = "999";
    private static final KontaktInfoDto KONTAKT_INFO = new KontaktInfoDto("kontaktnavn", "kontaktorgnr", "kontakttelefon");

    @BeforeEach
    public void setUp() {
        kontaktopplysningTjeneste = new KontaktopplysningTjeneste(kontaktopplysningService, aksesskontroll);
    }

    @Test
    void hentKontaktopplysning_kallerPåService_objektFinnes() {
        doReturn(Optional.of(new Kontaktopplysning())).when(kontaktopplysningService).hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        ResponseEntity<?> response = kontaktopplysningTjeneste.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void hentKontaktopplysning_kallerPåService_objektFinnesIkke() {
        ResponseEntity<?> response = kontaktopplysningTjeneste.hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        verify(kontaktopplysningService).hentKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void lagKontaktopplysning_kallerPåService() {
        ResponseEntity<?> response = kontaktopplysningTjeneste.lagKontaktopplysning(SAK_NUMMER, ORG_NUMMER, KONTAKT_INFO);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(
            SAK_NUMMER, ORG_NUMMER, KONTAKT_INFO.kontaktorgnr(), KONTAKT_INFO.kontaktnavn(), KONTAKT_INFO.kontakttelefon());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void slettKontaktopplysning_kallerPåService() {
        ResponseEntity<?> response = kontaktopplysningTjeneste.slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);

        verify(kontaktopplysningService).slettKontaktopplysning(SAK_NUMMER, ORG_NUMMER);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
