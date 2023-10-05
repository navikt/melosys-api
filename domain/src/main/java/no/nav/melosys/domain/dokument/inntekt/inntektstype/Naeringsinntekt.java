package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import org.jetbrains.annotations.NotNull;

public class Naeringsinntekt extends Inntekt {

    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/Naeringsinntektsbeskrivelse

    @NotNull
    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(@NotNull String value) {
        this.beskrivelse = value;
    }
}
