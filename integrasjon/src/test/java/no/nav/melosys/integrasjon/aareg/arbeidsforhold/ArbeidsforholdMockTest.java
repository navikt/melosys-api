package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse;
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

    @Test
    public void hentArbeidsforholdHistorikk() throws Exception {
        ArbeidsforholdMock arbeidsforholdMock = new ArbeidsforholdMock();

        final Long arbeidsforholdsID = 12608035L;

        HentArbeidsforholdHistorikkRequest request = new HentArbeidsforholdHistorikkRequest();
        request.setArbeidsforholdId(arbeidsforholdsID);

        HentArbeidsforholdHistorikkResponse response = arbeidsforholdMock.hentArbeidsforholdHistorikk(request);
        assertThat(response.getArbeidsforhold()).isNotNull();
    }

}