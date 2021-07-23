package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.SemistrukturertAdresseformatDto;

public final class SemistrukturertAdresseTilDtoKonverter {
    private SemistrukturertAdresseTilDtoKonverter() {
    }

    public static SemistrukturertAdresseformatDto tilDto(SemistrukturertAdresse semistrukturertAdresse,
                                                         KodeverkService kodeverkService) {
        if (semistrukturertAdresse == null) {
            return null;
        }
        return new SemistrukturertAdresseformatDto(
            semistrukturertAdresse.adresselinje1(),
            semistrukturertAdresse.adresselinje2(),
            semistrukturertAdresse.adresselinje3(),
            semistrukturertAdresse.adresselinje4(),
            semistrukturertAdresse.postnr(),
            semistrukturertAdresse.poststed(),
            kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, semistrukturertAdresse.landkode())
        );
    }
}
