package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

public interface ArbeidsforholdConsumer {

    FinnArbeidsforholdPrArbeidstakerResponse finnArbeidsforholdPrArbeidstaker(FinnArbeidsforholdPrArbeidstakerRequest request)
            throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput;
}
