package no.nav.melosys.domain.dokument.inntekt.inntektstype;


import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import org.jetbrains.annotations.NotNull;

public class Loennsinntekt extends Inntekt {

    protected String beskrivelse; //"http://nav.no/kodeverk/Kodeverk/Loennsbeskrivelse"s

    @JsonView(DokumentView.Database.class)
    protected Integer antall;

    @Override
    @NotNull
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public void setBeskrivelse(@NotNull String value) {
        this.beskrivelse = value;
    }

    public Integer getAntall() {
        return antall;
    }

    public void setAntall(Integer value) {
        this.antall = value;
    }

}
