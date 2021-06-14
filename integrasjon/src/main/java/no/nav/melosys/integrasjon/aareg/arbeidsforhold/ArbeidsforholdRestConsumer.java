package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

public interface ArbeidsforholdRestConsumer {
    ArbeidsfoholdResponse[] finnArbeidsforholdPrArbeidstaker(String fnr, ArbeidsfoholdQuery arbeidsfoholdQuery);
}
