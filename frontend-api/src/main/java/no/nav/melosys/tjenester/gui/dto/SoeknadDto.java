package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadDto {

    private long behandlingID;

    private SoeknadDokument soknadDokument;

    public SoeknadDto(long behandlingID, SoeknadDokument soeknad) {
        this.behandlingID = behandlingID;
        this.soknadDokument = soeknad;
    }

    public long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public SoeknadDokument getSoknadDokument() {
        return soknadDokument;
    }

    public void setSoknadDokument(SoeknadDokument soknadDokument) {
        this.soknadDokument = soknadDokument;
    }
}
