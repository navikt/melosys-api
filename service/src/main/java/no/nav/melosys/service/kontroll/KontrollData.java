package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;

public abstract class KontrollData {

    private MedlemskapDokument medlemskapDokument;

    protected KontrollData(MedlemskapDokument medlemskapDokument) {
        this.medlemskapDokument = medlemskapDokument;
    }

    public MedlemskapDokument getMedlemskapDokument() {
        return medlemskapDokument;
    }

    public void setMedlemskapDokument(MedlemskapDokument medlemskapDokument) {
        this.medlemskapDokument = medlemskapDokument;
    }
}
