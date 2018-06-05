package no.nav.melosys.domain;

import javax.persistence.Converter;

/**
 * Landkoder som vises i frontend.
 */
public enum Landkoder implements KodeverkTabell<Landkoder> {

    BE("BE", "Belgia"),
    BG("BG", "Bulgaria"),
    CZ("CZ", "Tsjekkia"),
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
    SE("SE", "Sverige"),
    DE("DE", "Tyskland"),
    HU("HU", "Ungarn"),
    AT("AT", "Østerrike");

    private String kode;
    private String beskrivelse;

    private Landkoder(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<Landkoder> {
        @Override
        protected Landkoder[] getLovligeVerdier() {
            return Landkoder.values();
        }
    }

}
