package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;

public class PensjonEllerTrygd extends Inntekt {

    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/PensjonEllerTrygdebeskrivelse

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String value) {
        this.beskrivelse = value;
    }
}
