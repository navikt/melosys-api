package no.nav.melosys.domain.util;

public enum Land_ISO2 {

    AU("Australia"),
    BA("Bosnia-Hercegovina"),
    CA("Canada"),
    CL("Chile"),
    FR("Frankrike"),
    GR("Hellas"),
    IN("India"),
    IL("Israel"),
    IM("Isle of Man"),
    IT("Italia"),
    JE("Jersey"),
    HR("Kroatia"),
    LU("Luxembourg"),
    ME("Montenegro"),
    NL("Nederland"),
    PT("Portugal"),
    CA_QC("Quebec"),
    RS("Serbia"),
    SI("Slovenia"),
    GB("Storbritannia"),
    CH("Sveits"),
    TR("Tyrkia"),
    HU("Ungarn"),
    US("USA"),
    AT("Østerrike"),
    BE("Belgia"),
    BG("Bulgaria"),
    DK("Danmark"),
    EE("Estland"),
    FI("Finland"),
    FO("Færøyene"),
    GL("Grønland"),
    IE("Irland"),
    IS("Island"),
    CY("Kypros"),
    LV("Latvia"),
    LI("Liechtenstein"),
    LT("Litauen"),
    MT("Malta"),
    NO("Norge"),
    PL("Polen"),
    RO("Romania"),
    SK("Slovakia"),
    ES("Spania"),
    SJ("Svalbard og Jan Mayen"),
    SE("Sverige"),
    CZ("Tsjekkia"),
    DE("Tyskland"),
    AX("Åland");


    private final String beskrivelse;

    private Land_ISO2(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return this.name();
    }

    public String getBeskrivelse() {
        return this.beskrivelse;
    }
}
