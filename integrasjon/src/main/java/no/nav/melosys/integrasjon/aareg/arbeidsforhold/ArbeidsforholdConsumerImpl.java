package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

public class ArbeidsforholdConsumerImpl implements ArbeidsforholdConsumer {
    private ArbeidsforholdV3 port;

    public ArbeidsforholdConsumerImpl(ArbeidsforholdV3 port) {
        this.port = port;
    }

    @Override
    public FinnArbeidsforholdPrArbeidstakerResponse finnArbeidsforholdPrArbeidstaker(FinnArbeidsforholdPrArbeidstakerRequest request)
            throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput {
        return port.finnArbeidsforholdPrArbeidstaker(request);
    }

}
