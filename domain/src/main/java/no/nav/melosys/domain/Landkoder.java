package no.nav.melosys.domain;

import javax.persistence.Converter;

/**
 * ISO2 landkoder som vises i frontend.
 */
public enum Landkoder implements InterntKodeverkTabell<Landkoder> {

    BE("BE", "Belgia"),
    BG("BG", "Bulgaria"),
    DK("DK", "Danmark"),
    EE("EE", "Estland"),
    FI("FI", "Finland"),
    FR("FR", "Frankrike"),
    GR("GR", "Hellas"),
    IE("IE", "Irland"),
    IS("IS", "Island"),
    IT("IT", "Italia"),
    HR("HR", "Kroatia"),
    CY("CY", "Kypros"),
    LV("LV", "Latvia"),
    LI("LI", "Liechtenstein"),
    LT("LT", "Litauen"),
    LU("LU", "Luxembourg"),
    MT("MT", "Malta"),
    NL("NL", "Nederland"),
    NO("NO", "Norge"),
    PL("PL", "Polen"),
    PT("PT", "Portugal"),
    RO("RO", "Romania"),
    SK("SK", "Slovakia"),
    SI("SI", "Slovenia"),
    ES("ES", "Spania"),
    GB("GB", "Storbritannia"),
    CH("CH", "Sveits"),
    SE("SE", "Sverige"),
    CZ("CZ", "Tsjekkia"),
    DE("DE", "Tyskland"),
    HU("HU", "Ungarn"),
    AT("AT", "Østerrike");

    private String kode;
    private String beskrivelse;

    Landkoder(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Converter
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Landkoder> {
        @Override
        protected Landkoder[] getLovligeVerdier() {
            return Landkoder.values();
        }
    }

}
