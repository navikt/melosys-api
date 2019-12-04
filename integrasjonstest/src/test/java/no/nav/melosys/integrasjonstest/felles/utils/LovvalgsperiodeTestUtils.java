package no.nav.melosys.integrasjonstest.felles.utils;

import java.time.LocalDate;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
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
        return LovvalgsperiodeDto.av(lovvalgsperiode);
    }
}
