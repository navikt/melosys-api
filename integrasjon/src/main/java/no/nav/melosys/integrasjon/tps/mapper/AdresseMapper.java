package no.nav.melosys.integrasjon.tps.mapper;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gyldighetsperiode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StedsadresseNorge;

class AdresseMapper {

    private AdresseMapper() {
        throw new IllegalStateException("Utility");
    }

    static Bostedsadresse mapTilBostedsadresse(no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse bostedsadresse) {
        Bostedsadresse b = new Bostedsadresse();
        if (bostedsadresse != null && bostedsadresse.getStrukturertAdresse() != null) {
            if (bostedsadresse.getStrukturertAdresse() instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) {
                b.setGateadresse(mapTilGateadresse((no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) bostedsadresse.getStrukturertAdresse()));
            }
            if (bostedsadresse.getStrukturertAdresse() instanceof StedsadresseNorge) {
                StedsadresseNorge stedsadresseNorge = (StedsadresseNorge) bostedsadresse.getStrukturertAdresse();
                b.setPostnr(stedsadresseNorge.getPoststed().getValue());
            }
            b.setLand(Land.av(bostedsadresse.getStrukturertAdresse().getLandkode().getValue()));
        }
        return b;
    }

    static UstrukturertAdresse mapTilPostadresse(Postadresse postadresse) {
        UstrukturertAdresse ua = new UstrukturertAdresse();
        if (postadresse != null && postadresse.getUstrukturertAdresse() != null) {
            ua.adresselinje1 = postadresse.getUstrukturertAdresse().getAdresselinje1();
            ua.adresselinje2 = postadresse.getUstrukturertAdresse().getAdresselinje2();
            ua.adresselinje3 = postadresse.getUstrukturertAdresse().getAdresselinje3();
            ua.adresselinje4 = postadresse.getUstrukturertAdresse().getAdresselinje4();
            ua.land = Land.av(postadresse.getUstrukturertAdresse().getLandkode().getValue());
        }
        return ua;
    }

    static MidlertidigPostadresse mapTilMidlertidigPostadresse(no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresse midlertidigPostadresse) {
        MidlertidigPostadresse mp = new MidlertidigPostadresse();
        if (midlertidigPostadresse != null) {
            if (midlertidigPostadresse instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge) {
                mp = mapTilMidlertidigPostadresseNorge((no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge) midlertidigPostadresse);
            } else if (midlertidigPostadresse instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseUtland) {
                mp = mapTilMidlertidigPostadresseUtland((no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseUtland) midlertidigPostadresse);
            }
            mp.endringstidspunkt = KonverteringsUtils.xmlGregorianCalendarToLocalDateTime(midlertidigPostadresse.getEndringstidspunkt());
            mp.postleveringsPeriode = mapTilPeriode(midlertidigPostadresse.getPostleveringsPeriode());
        }
        return mp;
    }

    private static MidlertidigPostadresseNorge mapTilMidlertidigPostadresseNorge(no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge midlertidigPostadresseNorge) {
        MidlertidigPostadresseNorge mpn = new MidlertidigPostadresseNorge();
        if (midlertidigPostadresseNorge.getStrukturertAdresse() != null) {
            if (midlertidigPostadresseNorge.getStrukturertAdresse() instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) {
                mpn.gateadresse = mapTilGateadresse((no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) midlertidigPostadresseNorge.getStrukturertAdresse());
            }
            if (midlertidigPostadresseNorge.getStrukturertAdresse() instanceof StedsadresseNorge) {
                StedsadresseNorge stedsadresseNorge = (StedsadresseNorge) midlertidigPostadresseNorge.getStrukturertAdresse();
                mpn.poststed = stedsadresseNorge.getPoststed().getValue();
            }
            mpn.land = Land.av(midlertidigPostadresseNorge.getStrukturertAdresse().getLandkode().getValue());
        }
        return mpn;
    }

    private static MidlertidigPostadresseUtland mapTilMidlertidigPostadresseUtland(no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseUtland midlertidigPostadresseUtland) {
        MidlertidigPostadresseUtland mpu = new MidlertidigPostadresseUtland();
        if (midlertidigPostadresseUtland.getUstrukturertAdresse() != null) {
            mpu.adresselinje1 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje1();
            mpu.adresselinje2 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje2();
            mpu.adresselinje3 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje3();
            mpu.adresselinje4 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje4();
            mpu.land = Land.av(midlertidigPostadresseUtland.getUstrukturertAdresse().getLandkode().getValue());
        }
        return mpu;
    }

    private static Gateadresse mapTilGateadresse(no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse gateadresse) {
        Gateadresse g = new Gateadresse();
        if (gateadresse != null) {
            g.setGatenavn(gateadresse.getGatenavn());
            g.setGatenummer(gateadresse.getGatenummer());
            g.setHusnummer(gateadresse.getHusnummer());
            g.setHusbokstav(gateadresse.getHusbokstav());
        }
        return g;
    }

    private static Periode mapTilPeriode(Gyldighetsperiode postleveringsPeriode) {
        return new Periode(
            KonverteringsUtils.xmlGregorianCalendarToLocalDate(postleveringsPeriode.getFom()),
            KonverteringsUtils.xmlGregorianCalendarToLocalDate(postleveringsPeriode.getTom())
        );
    }
}
