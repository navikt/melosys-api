package no.nav.melosys.tjenester.gui.graphql.mapping;

public final class MasterTilDtoKonverter {
    final static String PDL = "PDL";
    final static String NAV_PDL = "NAV (PDL)";

    private MasterTilDtoKonverter() {
    }

    public static String tilDto(String master) {
        if (PDL.equals(master)) return NAV_PDL;

        return master;
    }
}
