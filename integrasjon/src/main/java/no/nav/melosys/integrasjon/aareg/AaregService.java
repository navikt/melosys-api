package no.nav.melosys.integrasjon.aareg;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;

@Service
public class AaregService implements AaregFasade {

    private static final Logger log = LoggerFactory.getLogger(AaregService.class);

    private ArbeidsforholdConsumer arbeidsforholdConsumer;

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
    }

    @Override
    public List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String ident) throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput {
        return finnArbeidsforholdPrArbeidstaker(ident, REGELVERK_A_ORDNINGEN);
    }
    
    @Override
    public List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String ident, String regelverk) throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();

        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);
        request.setIdent(norskIdent);
        Regelverker regelverker = new Regelverker();

        regelverker.setKodeverksRef(regelverk);
        request.setRapportertSomRegelverk(regelverker);

        // Kall til Aa-registret
        return arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(request).getArbeidsforhold();
    }

}
