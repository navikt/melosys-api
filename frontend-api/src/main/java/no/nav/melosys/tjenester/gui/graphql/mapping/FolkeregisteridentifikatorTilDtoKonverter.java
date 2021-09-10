package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.Folkeregisteridentifikator;

public final class FolkeregisteridentifikatorTilDtoKonverter {
    private FolkeregisteridentifikatorTilDtoKonverter() {
    }

    public static String tilDto(Folkeregisteridentifikator folkeregisteridentifikator) {
        if (folkeregisteridentifikator == null) {
            return null;
        }
        return folkeregisteridentifikator.identifikasjonsnummer();
    }
}
