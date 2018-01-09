package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadDto {

    private long behandlingId;

    private SoeknadDokument soknadDokument;

    public SoeknadDto(long behandlingID, SoeknadDokument soeknad) {
        this.behandlingId = behandlingID;
        this.soknadDokument = soeknad;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public SoeknadDokument getSoknadDokument() {
        return soknadDokument;
    }

    public void setSoknadDokument(SoeknadDokument soknadDokument) {
        this.soknadDokument = soknadDokument;
    }
}
