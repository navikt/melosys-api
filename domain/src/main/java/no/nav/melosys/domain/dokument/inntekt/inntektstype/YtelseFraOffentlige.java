package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import org.jetbrains.annotations.NotNull;

public class YtelseFraOffentlige extends Inntekt {

    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/YtelseFraOffentligeBeskrivelse

    @Override
    @NotNull
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public void setBeskrivelse(@NotNull String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }
}
