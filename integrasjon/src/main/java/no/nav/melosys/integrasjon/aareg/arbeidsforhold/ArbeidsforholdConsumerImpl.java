package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.*;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse;

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

    @Override
    public HentArbeidsforholdHistorikkResponse hentArbeidsforholdHistorikk(HentArbeidsforholdHistorikkRequest request)
            throws HentArbeidsforholdHistorikkSikkerhetsbegrensning, HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet {
        return port.hentArbeidsforholdHistorikk(request);
    }
}
