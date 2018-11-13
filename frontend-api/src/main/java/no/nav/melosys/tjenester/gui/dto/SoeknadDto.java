package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadDto {

    private long behandlingID;

    private SoeknadDokument soeknadDokument;

    private SoeknadTilleggsDataDto tilleggsData;

    public SoeknadDto(long behandlingID,
                      SoeknadDokument soeknad,
                      SoeknadTilleggsDataDto tilleggsData) {
        this.behandlingID = behandlingID;
        this.soeknadDokument = soeknad;
        this.tilleggsData = tilleggsData;
    }

    @JsonCreator
    public SoeknadDto(@JsonProperty("behandlingID") long behandlingID,
                      @JsonProperty("soeknadDokument") SoeknadDokument soeknad) {
        this(behandlingID, soeknad, new SoeknadTilleggsDataDto());
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

    public SoeknadTilleggsDataDto getTilleggsData() {
        return tilleggsData;
    }

    public void setTilleggsData(SoeknadTilleggsDataDto soeknadTilleggsDataDto) {
        this.tilleggsData = soeknadTilleggsDataDto;
    }
}
