package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.kontroll.MedlemskapKontroller;
import no.nav.melosys.service.kontroll.PeriodeKontroller;

class VedtakKontroller {

    private VedtakKontroller() {}

    static Kontroll_begrunnelser overlappendeMedlemsperiode(VedtakKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();
        
        return MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(), medlemskapDokument)
            ? Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Kontroll_begrunnelser periodeOver24Mnd(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            && PeriodeKontroller.periodeOver24Mnd(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? Kontroll_begrunnelser.PERIODEN_OVER_24_MD : null;
    }
}
