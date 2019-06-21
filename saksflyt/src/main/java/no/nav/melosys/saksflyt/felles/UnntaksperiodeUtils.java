package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public final class UnntaksperiodeUtils {

    private UnntaksperiodeUtils() {}

    public static Lovvalgsperiode opprettLovvalgsperiode(SedDokument sedDokument) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(sedDokument.getLovvalgBestemmelse());
        lovvalgsperiode.setFom(sedDokument.getLovvalgsperiode().getFom());
        lovvalgsperiode.setTom(sedDokument.getLovvalgsperiode().getTom());
        lovvalgsperiode.setLovvalgsland(sedDokument.getLovvalgslandKode());
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
        lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);

        return lovvalgsperiode;
    }

}
