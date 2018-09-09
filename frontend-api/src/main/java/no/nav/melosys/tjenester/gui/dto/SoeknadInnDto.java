package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadInnDto {

    private SoeknadDokument soeknadDokument;

    public SoeknadDokument getSoknadDokument() {
        return soeknadDokument;
    }

    public void setSoknadDokument(SoeknadDokument soeknadDokument) {
        this.soeknadDokument = soeknadDokument;
    }
}


