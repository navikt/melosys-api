package no.nav.melosys.service.persondata.mapping.adresse;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.util.IsoLandkodeKonverterer;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.PostadresseIFrittFormat;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Postboksadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.UtenlandskAdresseIFrittFormat;
import no.nav.melosys.service.kodeverk.KodeverkService;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public final class KontaktadresseOversetter {
    private KontaktadresseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static no.nav.melosys.domain.person.adresse.Kontaktadresse oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.adresse.Kontaktadresse kontaktadressePDL,
        KodeverkService kodeverkService) {
        StrukturertAdresse strukturertAdresse = null;
        SemistrukturertAdresse semistrukturertAdresse = null;
        String coAdressenavnFraPostboks = null;

        if (kontaktadressePDL.vegadresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(kontaktadressePDL.vegadresse(), kodeverkService);
        } else if (kontaktadressePDL.utenlandskAdresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(kontaktadressePDL.utenlandskAdresse());
        } else if (kontaktadressePDL.postadresseIFrittFormat() != null) {
            semistrukturertAdresse = lagPostadresse(kontaktadressePDL.postadresseIFrittFormat(), kodeverkService);
        } else if (kontaktadressePDL.utenlandskAdresseIFrittFormat() != null) {
            semistrukturertAdresse = lagPostadresse(kontaktadressePDL.utenlandskAdresseIFrittFormat());
        } else if (kontaktadressePDL.postboksadresse() != null) {
            strukturertAdresse = lagStrukturertAdresse(kontaktadressePDL.postboksadresse(), kodeverkService);
            coAdressenavnFraPostboks = kontaktadressePDL.postboksadresse().postbokseier();
        }

        return new Kontaktadresse(strukturertAdresse,
            semistrukturertAdresse,
            coAdressenavnFraPostboks != null ? coAdressenavnFraPostboks : kontaktadressePDL.coAdressenavn(),
            KonverteringsUtils.localDateTimeToLocalDate(kontaktadressePDL.gyldigFraOgMed()),
            KonverteringsUtils.localDateTimeToLocalDate(kontaktadressePDL.gyldigTilOgMed()),
            kontaktadressePDL.metadata().master(),
            kontaktadressePDL.hentKilde(),
            kontaktadressePDL.hentDatoSistRegistrert(),
            kontaktadressePDL.metadata().historisk()
        );
    }

    private static StrukturertAdresse lagStrukturertAdresse(Postboksadresse postboksadresse,
                                                            KodeverkService kodeverkService) {
        return new StrukturertAdresse(null, null,
            null, postboksadresse.postboks(),
            postboksadresse.postnummer(),
            kodeverkService.dekod(POSTNUMMER, postboksadresse.postnummer()),
            null,
            Landkoder.NO.getKode()
        );
    }

    private static SemistrukturertAdresse lagPostadresse(PostadresseIFrittFormat postadresseIFrittFormat,
                                                         KodeverkService kodeverkService) {
        return new SemistrukturertAdresse(
            postadresseIFrittFormat.adresselinje1(),
            postadresseIFrittFormat.adresselinje2(),
            postadresseIFrittFormat.adresselinje3(),
            null,
            postadresseIFrittFormat.postnummer(),
            kodeverkService.dekod(POSTNUMMER, postadresseIFrittFormat.postnummer()),
            Landkoder.NO.getKode()
        );
    }

    private static SemistrukturertAdresse lagPostadresse(UtenlandskAdresseIFrittFormat utenlandskAdresseIFrittFormat) {
        return new SemistrukturertAdresse(
            utenlandskAdresseIFrittFormat.adresselinje1(),
            utenlandskAdresseIFrittFormat.adresselinje2(),
            utenlandskAdresseIFrittFormat.adresselinje3(),
            null,
            utenlandskAdresseIFrittFormat.postkode(),
            utenlandskAdresseIFrittFormat.byEllerStedsnavn(),
            IsoLandkodeKonverterer.tilIso2(utenlandskAdresseIFrittFormat.landkode()));
    }
}


