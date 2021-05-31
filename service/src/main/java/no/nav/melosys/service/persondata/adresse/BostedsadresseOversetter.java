package no.nav.melosys.service.persondata.adresse;

import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Matrikkeladresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Vegadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public class BostedsadresseOversetter {
    private BostedsadresseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static no.nav.melosys.domain.person.adresse.Bostedsadresse oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse bostedsadressePDL,
        KodeverkService kodeverkService) {
        StrukturertAdresse strukturertAdresse = null;

        if (bostedsadressePDL.vegadresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(bostedsadressePDL.vegadresse(), kodeverkService);
        } else if (bostedsadressePDL.utenlandskAdresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(bostedsadressePDL.utenlandskAdresse());
        } else if (bostedsadressePDL.matrikkeladresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(bostedsadressePDL.matrikkeladresse(), kodeverkService);
        }  // Ukjent bosted

        return new Bostedsadresse(strukturertAdresse,
            null,
            bostedsadressePDL.coAdressenavn(),
            bostedsadressePDL.gyldigFraOgMed(),
            bostedsadressePDL.gyldigTilOgMed(),
            bostedsadressePDL.metadata().master(),
            bostedsadressePDL.hentKilde(),
            bostedsadressePDL.metadata().historisk()
            );
    }

    private static StrukturertAdresse lagStrukturertAdresse(Vegadresse vegadresse, KodeverkService kodeverkService) {
        return new StrukturertAdresse(
            vegadresse.adressenavn(),
            vegadresse.husnummer() + leggTilHusBokstav(vegadresse),
            vegadresse.tilleggsnavn(),
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

    private static StrukturertAdresse lagStrukturertAdresse(UtenlandskAdresse utenlandskAdresse) {
        return new StrukturertAdresse(
            utenlandskAdresse.adressenavnNummer(),
            utenlandskAdresse.bygningEtasjeLeilighet(),
            null,
            utenlandskAdresse.postboksNummerNavn(),
            utenlandskAdresse.postkode(),
            utenlandskAdresse.bySted(),
            utenlandskAdresse.regionDistriktOmraade(),
            LandkoderUtils.tilIso2(utenlandskAdresse.landkode())
        );
    }

    private static StrukturertAdresse lagStrukturertAdresse(Matrikkeladresse matrikkeladresse,
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
