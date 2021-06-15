package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.aareg.arbeidsforhold.model.Arbeidsforhold;

public class ArbeidsfoholdResponse {

    private final Arbeidsforhold[] arbeidsforholdResponse;

    public ArbeidsfoholdResponse(Arbeidsforhold[] arbeidsforholdResponse) {
        this.arbeidsforholdResponse = arbeidsforholdResponse;
    }

    public Arbeidsforhold[] getArbeidsforholdResponse() {
        return arbeidsforholdResponse;
    }

    public String getJsonDocument() {
        TODO: // serialize to json or find out if we want to convert to xml doc?
        return  "json or xml?";
    }
}
