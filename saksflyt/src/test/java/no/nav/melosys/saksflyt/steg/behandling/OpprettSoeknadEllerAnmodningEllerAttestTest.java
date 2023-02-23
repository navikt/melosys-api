package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpprettSoeknadEllerAnmodningEllerAttestTest {

    @Mock
    private MottatteOpplysningerService mottatteOpplysningerService;

    private OpprettSoeknadEllerAnmodningEllerAttest opprettSoeknadEllerAnmodningEllerAttest;

    @BeforeEach
    public void setUp() {
        opprettSoeknadEllerAnmodningEllerAttest = new OpprettSoeknadEllerAnmodningEllerAttest(mottatteOpplysningerService);
    }

    @Test
    void utfør_kallerOpprettSøknadEllerAnmodningEllerAttest() {
        Prosessinstans prosessinstans = new Prosessinstans();

        opprettSoeknadEllerAnmodningEllerAttest.utfør(prosessinstans);

        verify(mottatteOpplysningerService).opprettSøknadEllerAnmodningEllerAttest(prosessinstans);
    }
}
