package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class SoeknadInnDto {

    private SoeknadDokument soknadDokument;

    public SoeknadDokument getSoknadDokument() {
        return soknadDokument;
    }

    public void setSoknadDokument(SoeknadDokument soknadDokument) {
        this.soknadDokument = soknadDokument;
    }
}


