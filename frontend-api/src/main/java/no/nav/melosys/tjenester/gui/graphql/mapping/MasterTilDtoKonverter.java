package no.nav.melosys.tjenester.gui.graphql.mapping;

public final class MasterTilDtoKonverter {
    static final String PDL = "PDL";
    static final String TPS = "TPS";
    static final String NAV_PDL = "NAV (PDL)";
    static final String NAV_TPS = "NAV (TPS)";

    private MasterTilDtoKonverter() {
    }

    public static String tilDto(String master) {
        if (master == null) {
            return "";
        }
        return switch (master) {
            case PDL -> NAV_PDL;
            case TPS -> NAV_TPS;
            default -> master;
        };
    }
}
