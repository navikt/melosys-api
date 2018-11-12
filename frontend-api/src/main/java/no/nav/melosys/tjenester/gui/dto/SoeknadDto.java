package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadDto {

    private long behandlingID;

    private SoeknadDokument soeknadDokument;

    private SoeknadTilleggDataDto tilleggsData;

    @JsonCreator
    public SoeknadDto(@JsonProperty("behandlingID") long behandlingID,
                      @JsonProperty("soeknadDokument") SoeknadDokument soeknad,
                      @JsonProperty("tilleggsData") SoeknadTilleggDataDto tilleggData) {
        this.behandlingID = behandlingID;
        this.soeknadDokument = soeknad;
        this.tilleggsData = tilleggData;
    }

    public long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public SoeknadDokument getSoeknadDokument() {
        return soeknadDokument;
    }

    public void setSoeknadDokument(SoeknadDokument soeknadDokument) {
        this.soeknadDokument = soeknadDokument;
    }

    public SoeknadTilleggDataDto getTilleggsData() {
        return tilleggsData;
    }

    public void setTilleggsData(SoeknadTilleggDataDto soeknadTilleggDataDto) {
        this.tilleggsData = soeknadTilleggDataDto;
    }
}
