package no.nav.melosys.service.persondata.adresse;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class OppholdsadresseOversetter {
    private OppholdsadresseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static no.nav.melosys.domain.person.adresse.Oppholdsadresse oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.adresse.Oppholdsadresse oppholdsadressePDL,
        KodeverkService kodeverkService) {
        StrukturertAdresse strukturertAdresse = null;

        if (oppholdsadressePDL.vegadresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(oppholdsadressePDL.vegadresse(), kodeverkService);
        } else if (oppholdsadressePDL.utenlandskAdresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(oppholdsadressePDL.utenlandskAdresse());
        } else if (oppholdsadressePDL.matrikkeladresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(oppholdsadressePDL.matrikkeladresse(),
                kodeverkService);
        }

        return new Oppholdsadresse(strukturertAdresse,
            null,
            oppholdsadressePDL.coAdressenavn(),
            oppholdsadressePDL.gyldigFraOgMed(),
            oppholdsadressePDL.gyldigTilOgMed(),
            oppholdsadressePDL.metadata().master(),
            oppholdsadressePDL.hentKilde(),
            oppholdsadressePDL.metadata().historisk()
        );
    }
}
