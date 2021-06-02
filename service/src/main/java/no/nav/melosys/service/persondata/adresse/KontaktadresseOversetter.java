package no.nav.melosys.service.persondata.adresse;

import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.*;
import no.nav.melosys.service.kodeverk.KodeverkService;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public class KontaktadresseOversetter {
    private KontaktadresseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static no.nav.melosys.domain.person.adresse.Kontaktadresse oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.adresse.Kontaktadresse kontaktadressePDL,
        KodeverkService kodeverkService) {
        StrukturertAdresse strukturertAdresse = null;
        UstrukturertAdresse ustrukturertAdresse = null;

        if (kontaktadressePDL.vegadresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(kontaktadressePDL.vegadresse(), kodeverkService);
        } else if (kontaktadressePDL.utenlandskAdresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(kontaktadressePDL.utenlandskAdresse());
        } else if (kontaktadressePDL.postadresseIFrittFormat() != null) {
            ustrukturertAdresse = lagUstrukturertAdresse(kontaktadressePDL.postadresseIFrittFormat(), kodeverkService);
        } else if (kontaktadressePDL.utenlandskAdresseIFrittFormat() != null) {
            ustrukturertAdresse = lagUstrukturertAdresse(kontaktadressePDL.utenlandskAdresseIFrittFormat());
        } else if (kontaktadressePDL.postboksadresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(kontaktadressePDL.postboksadresse(), kodeverkService);
        }

        return new Kontaktadresse(strukturertAdresse,
            ustrukturertAdresse,
            kontaktadressePDL.coAdressenavn(),
            kontaktadressePDL.gyldigFraOgMed(),
            kontaktadressePDL.gyldigTilOgMed(),
            kontaktadressePDL.metadata().master(),
            kontaktadressePDL.hentKilde(),
            kontaktadressePDL.metadata().historisk()
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

    private static StrukturertAdresse lagStrukturertAdresse(Postboksadresse postboksadresse,
                                                            KodeverkService kodeverkService) {
        return new StrukturertAdresse(
            null,
            null,
            null,
            postboksadresse.postboks(),
            postboksadresse.postnummer(),
            kodeverkService.dekod(POSTNUMMER, postboksadresse.postnummer(), LocalDate.now()),
            null,
            Landkoder.NO.getKode()
        );
    }

    private static UstrukturertAdresse lagUstrukturertAdresse(PostadresseIFrittFormat postadresseIFrittFormat,
                                                              KodeverkService kodeverkService) {
        return new UstrukturertAdresse(
            postadresseIFrittFormat.adresselinje1(),
            postadresseIFrittFormat.adresselinje2(),
            postadresseIFrittFormat.adresselinje3(),
            postadresseIFrittFormat.postnummer() + " " + kodeverkService.dekod(POSTNUMMER, postadresseIFrittFormat.postnummer(), LocalDate.now()),
            Landkoder.NO.getKode()
        );
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

    private static UstrukturertAdresse lagUstrukturertAdresse(
        UtenlandskAdresseIFrittFormat utenlandskAdresseIFrittFormat) {
        return new UstrukturertAdresse(
            utenlandskAdresseIFrittFormat.adresselinje1(),
            utenlandskAdresseIFrittFormat.adresselinje2(),
            utenlandskAdresseIFrittFormat.adresselinje3(),
            utenlandskAdresseIFrittFormat.postkode() + " " + utenlandskAdresseIFrittFormat.byEllerStedsnavn(),
            LandkoderUtils.tilIso2(utenlandskAdresseIFrittFormat.landkode()));
    }
}


