package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpprettMottatteOpplysningerTest {

    @Mock
    private MottatteOpplysningerService mottatteOpplysningerService;

    private OpprettMottatteOpplysninger opprettMottatteOpplysninger;

    @BeforeEach
    public void setUp() {
        opprettMottatteOpplysninger = new OpprettMottatteOpplysninger(mottatteOpplysningerService);
    }

    @Test
    void utfør_kallerOpprettSøknadEllerAnmodningEllerAttest() {
        Prosessinstans prosessinstans = new Prosessinstans();

        opprettMottatteOpplysninger.utfør(prosessinstans);

        verify(mottatteOpplysningerService).opprettSøknadEllerAnmodningEllerAttest(prosessinstans);
    }
}
