package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadDto {

    private long behandlingID;

    private SoeknadDokument soeknadDokument;

    public SoeknadDto(long behandlingID, SoeknadDokument soeknad) {
        this.behandlingID = behandlingID;
        this.soeknadDokument = soeknad;
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


}
