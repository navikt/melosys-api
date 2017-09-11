package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

public class ArbeidsforholdMockTest {

    @Test
    public void finnArbeidsforholdPrArbeidstaker() throws Exception {
        ArbeidsforholdMock arbeidsforholdMock = new ArbeidsforholdMock();

        List<String> støttet = Arrays.asList("88888888884", "88888888885", "88888888886", "99999999999", "FJERNET");

        for (String ident : støttet) {
            FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
            NorskIdent norskIdent = new NorskIdent();
            norskIdent.setIdent(ident);
            request.setIdent(norskIdent);

            FinnArbeidsforholdPrArbeidstakerResponse response = arbeidsforholdMock.finnArbeidsforholdPrArbeidstaker(request);
            assertThat(response.getArbeidsforhold()).isNotEmpty();
        }

    }

}