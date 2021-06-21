package no.nav.melosys.service.persondata.adresse;

import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Matrikkeladresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

class PdlAdresseformatOversetter {
    private PdlAdresseformatOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    static StrukturertAdresse lagStrukturertAdresse(Vegadresse vegadresse, KodeverkService kodeverkService) {
        return new StrukturertAdresse(
            vegadresse.tilleggsnavn(),
            vegadresse.adressenavn(),
            vegadresse.husnummer() + leggTilHusBokstav(vegadresse),
            null,
            vegadresse.postnummer(),
            kodeverkService.dekod(POSTNUMMER, vegadresse.postnummer(), LocalDate.now()),
            null,
            Landkoder.NO.getKode()
        );
    }

    private static String leggTilHusBokstav(Vegadresse vegadresse) {
        return vegadresse.husbokstav() != null ? " " + vegadresse.husbokstav() : "";
    }

    static StrukturertAdresse lagStrukturertAdresse(UtenlandskAdresse utenlandskAdresse) {
        return new StrukturertAdresse(null,
            utenlandskAdresse.adressenavnNummer(),
            utenlandskAdresse.bygningEtasjeLeilighet(),
            utenlandskAdresse.postboksNummerNavn(),
            utenlandskAdresse.postkode(),
            utenlandskAdresse.bySted(),
            utenlandskAdresse.regionDistriktOmraade(),
            LandkoderUtils.tilIso2(utenlandskAdresse.landkode())
        );
    }

    static StrukturertAdresse lagStrukturertAdresse(Matrikkeladresse matrikkeladresse,
                                                            KodeverkService kodeverkService) {
        return new StrukturertAdresse(
            matrikkeladresse.tilleggsnavn(),
            null,
            matrikkeladresse.postnummer(),
            kodeverkService.dekod(POSTNUMMER, matrikkeladresse.postnummer(), LocalDate.now()),
            null,
            Landkoder.NO.getKode()
        );
    }
}
