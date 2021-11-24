package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.Folkeregisterpersonstatus;
import no.nav.melosys.tjenester.gui.graphql.dto.FolkeregisterpersonstatusDto;

public final class FolkeregisterpersonstatusTilDtoKonverter {
    private FolkeregisterpersonstatusTilDtoKonverter() {
    }

    public static FolkeregisterpersonstatusDto tilDto(Folkeregisterpersonstatus folkeregisterpersonstatus) {
        if (folkeregisterpersonstatus == null) {
            return null;
        }
        return new FolkeregisterpersonstatusDto(
            folkeregisterpersonstatus.personstatus().getKode(),
            folkeregisterpersonstatus.hentGjeldendeTekst(),
            folkeregisterpersonstatus.registreringsdato(),
            MasterTilDtoKonverter.tilDto(folkeregisterpersonstatus.master()),
            folkeregisterpersonstatus.kilde(),
            folkeregisterpersonstatus.erHistorisk()
        );
    }
}
