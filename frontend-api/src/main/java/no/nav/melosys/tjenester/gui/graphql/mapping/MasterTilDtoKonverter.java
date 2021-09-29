package no.nav.melosys.tjenester.gui.graphql.mapping;

public final class MasterTilDtoKonverter {
    private MasterTilDtoKonverter() {
    }

    public static String tilDto(String master) {
        final String PDL = "PDL";
        final String NAV_PDL = "NAV (PDL)";

        if (PDL.equals(master)) return NAV_PDL;

        return master;
    }
}
