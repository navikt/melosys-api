package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import org.jetbrains.annotations.NotNull;

public class PensjonEllerTrygd extends Inntekt {

    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/PensjonEllerTrygdebeskrivelse

    @NotNull
    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(@NotNull String value) {
        this.beskrivelse = value;
    }
}
