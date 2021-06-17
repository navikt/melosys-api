package no.nav.melosys.service.persondata.adresse;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class BostedsadresseOversetter {
    private BostedsadresseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static no.nav.melosys.domain.person.adresse.Bostedsadresse oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse bostedsadressePDL,
        KodeverkService kodeverkService) {
        StrukturertAdresse strukturertAdresse = null;

        if (bostedsadressePDL.vegadresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(bostedsadressePDL.vegadresse(),
                kodeverkService);
        } else if (bostedsadressePDL.utenlandskAdresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(bostedsadressePDL.utenlandskAdresse());
        } else if (bostedsadressePDL.matrikkeladresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(bostedsadressePDL.matrikkeladresse(),
                kodeverkService);
        }  // Ukjent bosted

        return new Bostedsadresse(strukturertAdresse,
            bostedsadressePDL.coAdressenavn(),
            bostedsadressePDL.gyldigFraOgMed(),
            bostedsadressePDL.gyldigTilOgMed(),
            bostedsadressePDL.metadata().master(),
            bostedsadressePDL.hentKilde(),
            bostedsadressePDL.metadata().historisk()
            );
    }
}
