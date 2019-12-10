package no.nav.melosys.integrasjonstest.felles.utils;

import java.time.LocalDate;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;

public final class LovvalgsperiodeTestUtils {
    private LovvalgsperiodeTestUtils() {}

    public static LovvalgsperiodeDto lagLovvalgsperiodeDto(LovvalgBestemmelse lovvalgsbestemmelse, Landkoder landkode, InnvilgelsesResultat innvilgelsesResultat) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(landkode);
        lovvalgsperiode.setBestemmelse(lovvalgsbestemmelse);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        lovvalgsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        if (innvilgelsesResultat == InnvilgelsesResultat.INNVILGET) {
            lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
            lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        }
        if (lovvalgsbestemmelse == Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A) {
            lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        }

        return LovvalgsperiodeDto.av(lovvalgsperiode);
    }
}
