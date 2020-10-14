package no.nav.melosys.integrasjon.tps.mapper;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.UstrukturertAdresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StedsadresseNorge;

class AdresseMapper {

    private AdresseMapper() {
        throw new IllegalStateException("Utility");
    }

    static Bostedsadresse mapTilBostedsadresse(no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse bostedsadresse) {
        Bostedsadresse b = new Bostedsadresse();
        if (bostedsadresse == null || bostedsadresse.getStrukturertAdresse() == null) {
            return b;
        }
        if (bostedsadresse.getStrukturertAdresse() instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) {
            Gateadresse gateadresse = (Gateadresse) bostedsadresse.getStrukturertAdresse();
            b.getGateadresse().setGatenavn(gateadresse.getGatenavn());
            b.getGateadresse().setGatenummer(gateadresse.getGatenummer());
            b.getGateadresse().setHusnummer(gateadresse.getHusnummer());
            b.getGateadresse().setHusbokstav(gateadresse.getHusbokstav());
        }
        if (bostedsadresse.getStrukturertAdresse() instanceof StedsadresseNorge) {
            StedsadresseNorge stedsadresseNorge = (StedsadresseNorge) bostedsadresse.getStrukturertAdresse();
            b.setPostnr(stedsadresseNorge.getPoststed().getValue());
        }
        b.setLand(Land.av(bostedsadresse.getStrukturertAdresse().getLandkode().getValue()));
        return b;
    }

    static UstrukturertAdresse mapTilPostadresse(Postadresse postadresse) {
        UstrukturertAdresse ua = new UstrukturertAdresse();
        if (postadresse == null || postadresse.getUstrukturertAdresse() == null) {
            return ua;
        }
        ua.adresselinje1 = postadresse.getUstrukturertAdresse().getAdresselinje1();
        ua.adresselinje2 = postadresse.getUstrukturertAdresse().getAdresselinje2();
        ua.adresselinje3 = postadresse.getUstrukturertAdresse().getAdresselinje3();
        ua.adresselinje4 = postadresse.getUstrukturertAdresse().getAdresselinje4();
        ua.land = Land.av(postadresse.getUstrukturertAdresse().getLandkode().getValue());
        return ua;
    }
}
