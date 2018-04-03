package no.nav.melosys.domain.gsak;

/**
 * Denne enumen er en hardkoding av kodeverket PrioritetT:
 * https://kodeverkviewer.adeo.no/kodeverk/xml/prioritetT.xml
 */
public enum PrioritetType {
    HOY_MED("Høy"),
    LAV_MED("Lav"),
    NORM_MED("Normal"),
    HOY_UFM("Høy"),
    LAV_UFM("Lav"),
    NORM_UFM("Normal");

    private String navn;

    PrioritetType(String navn) {
        this.navn = navn;
    }
}
