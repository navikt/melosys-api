package no.nav.melosys.service.dokument.brev.mapper;

public final class MapperUtils {

    private MapperUtils() {
    }

    static String fjernNamespaceFra(String xml) {
        return xml.replaceAll("(<\\?[^<]*\\?>)?", "") /* remove preamble */
            .replaceAll(" xmlns.*?(\"|\').*?(\"|\')", "") /* remove xmlns declaration */
            .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") /* remove opening tag prefix */
            .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
    }
}
