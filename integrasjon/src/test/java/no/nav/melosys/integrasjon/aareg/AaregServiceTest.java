package no.nav.melosys.integrasjon.aareg;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;


@RunWith(MockitoJUnitRunner.class)
public class AaregServiceTest {

    @Mock
    ArbeidsforholdConsumer arbeidsforholdConsumer;

    private AaregService aaregService;

    @Before
    public void setUp() {
        aaregService = new AaregService(arbeidsforholdConsumer);
    }

    @Test
    public void finnArbeidsforholdPrArbeidstaker() throws Exception {
        //TODO
    }


}