package no.nav.melosys.regler.api.lovvalg.rep;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Artikkel {

    ART_11_1("Artikkel 11.1"),
    ART_11_2("Artikkel 11.2"),
    ART_11_3A("Artikkel 11.3a"),
    ART_11_3B("Artikkel 11.3b"),
    ART_11_3C("Artikkel 11.3c"),
    ART_11_3D("Artikkel 11.3d"),
    ART_11_3E("Artikkel 11.3e"),
    ART_12_1("Artikkel 12.1"),
    ART_12_2("Artikkel 12.2"),
    ART_13_1A("Artikkel 13.1a"),
    ART_13_1B1("Artikkel 13.1b1"),
    ART_13_1B2("Artikkel 13.1b2"),
    ART_13_1B3("Artikkel 13.1b3"),
    ART_13_1B4("Artikkel 13.1b4"),
    ART_13_2A("Artikkel 13.2a"),
    ART_13_2B("Artikkel 13.2b"),
    ART_16_1("Artikkel 16.1"),
    ART_16_2("Artikkel 16.2");

    @JsonValue
    public final String beskrivelse;
    
    private Artikkel(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

}
