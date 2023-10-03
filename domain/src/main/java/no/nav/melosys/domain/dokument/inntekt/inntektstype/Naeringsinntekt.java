package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import org.jetbrains.annotations.NotNull;

public class Naeringsinntekt extends Inntekt {

    @NotNull
    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/Naeringsinntektsbeskrivelse

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String value) {
        this.beskrivelse = value;
    }
}
