package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;

public class UtledMedlemskapsperiodeRequest {
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;

    @JsonCreator
    public UtledMedlemskapsperiodeRequest(@JsonProperty("bestemmelse") Folketrygdloven_kap2_bestemmelser bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }
}
