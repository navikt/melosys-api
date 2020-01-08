package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.service.kontroll.KontrollData;

class VedtakKontrollData extends KontrollData {

    private Lovvalgsperiode lovvalgsperiode;

    VedtakKontrollData(MedlemskapDokument medlemskapDokument, Lovvalgsperiode lovvalgsperiode) {
        super(medlemskapDokument);
        this.lovvalgsperiode = lovvalgsperiode;
    }

    Lovvalgsperiode getLovvalgsperiode() {
        return lovvalgsperiode;
    }
}
