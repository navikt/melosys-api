package no.nav.melosys.integrasjon.tps.mapper;

import java.util.Optional;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.person.UstrukturertAdresse;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;

class AdresseMapper {

    private static final String BOSTEDSADRESSE = "BOSTEDSADRESSE";
    private static final String POSTADRESSE = "POSTADRESSE";
    private static final String MIDLERTIDIG_POSTADRESSE_UTLAND = "MIDLERTIDIG_POSTADRESSE_UTLAND";
    private static final String MIDLERTIDIG_POSTADRESSE_NORGE = "MIDLERTIDIG_POSTADRESSE_NORGE";
    private static final String CO_TILLEGGSADRESSETYPE = "C/O";
    private static final String V_TILLEGGSADRESSETYPE = "V/";

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
            if (midlertidigPostadresse.getEndringstidspunkt() != null) {
                mp.endringstidspunkt = KonverteringsUtils.xmlGregorianCalendarToLocalDateTime(midlertidigPostadresse.getEndringstidspunkt());
            }
            if (midlertidigPostadresse.getPostleveringsPeriode() != null) {
                mp.postleveringsPeriode = mapTilPeriode(midlertidigPostadresse.getPostleveringsPeriode());
            }
        }
        return mp;
    }

    static UstrukturertAdresse mapTilGjeldendePostadresse(Bruker person) {
        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        if (person.getGjeldendePostadressetype() != null) {
            if (BOSTEDSADRESSE.equals(person.getGjeldendePostadressetype().getValue()) &&
                person.getBostedsadresse() != null) {
                return mapBostedadresse(person);
            } else if (POSTADRESSE.equals(person.getGjeldendePostadressetype().getValue()) &&
                person.getPostadresse().getUstrukturertAdresse() != null) {
                return mapPostadresse(person);
            } else if (MIDLERTIDIG_POSTADRESSE_UTLAND.equals(person.getGjeldendePostadressetype()
                .getValue()) && person.getMidlertidigPostadresse() != null) {
                return mapMidlertidigUtland(person);
            } else if (MIDLERTIDIG_POSTADRESSE_NORGE.equals(person.getGjeldendePostadressetype()
                .getValue()) && person.getMidlertidigPostadresse() != null) {
                return mapMidlertidigNorge(person);
            }
        }

        return ustrukturertAdresse;
    }

    private static MidlertidigPostadresseNorge mapTilMidlertidigPostadresseNorge(
        no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge midlertidigPostadresseNorge) {
        MidlertidigPostadresseNorge mpn = new MidlertidigPostadresseNorge();
        final var strukturertAdresse = midlertidigPostadresseNorge.getStrukturertAdresse();
        if (strukturertAdresse != null) {
            if (strukturertAdresse instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) {
                mpn.gateadresse = mapTilGateadresse((no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) strukturertAdresse);
            }
            if (strukturertAdresse instanceof Matrikkeladresse) {
                Matrikkeladresse matrikkeladresse = (Matrikkeladresse) strukturertAdresse;
                mpn.gateadresse = new Gateadresse();
                mpn.gateadresse.setGatenavn(matrikkeladresse.getEiendomsnavn());
            }
            if (strukturertAdresse instanceof StedsadresseNorge) {
                StedsadresseNorge stedsadresseNorge = (StedsadresseNorge) strukturertAdresse;
                mpn.poststed = stedsadresseNorge.getPoststed().getValue();
            }
            mpn.land = Land.av(strukturertAdresse.getLandkode().getValue());
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

    private static UstrukturertAdresse mapBostedadresse(Bruker person) {
        UstrukturertAdresse postadresse = new UstrukturertAdresse();

        if (person.getBostedsadresse().getStrukturertAdresse() instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) {
            no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse gateadresse = (no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) person.getBostedsadresse().getStrukturertAdresse();

            postadresse.adresselinje1 = (gateadresse.getGatenavn() == null ? "" : gateadresse.getGatenavn())
                + " "
                + (gateadresse.getHusnummer() == null ? "" : gateadresse.getHusnummer().toString())
                + (gateadresse.getHusbokstav() == null ? "" : gateadresse.getHusbokstav());
            
        } else if (person.getBostedsadresse().getStrukturertAdresse() instanceof Matrikkeladresse) {
            Matrikkeladresse matrikkeladresse = (Matrikkeladresse) person.getBostedsadresse().getStrukturertAdresse();
            postadresse.adresselinje1 = matrikkeladresse.getEiendomsnavn();
        } else if (person.getBostedsadresse().getStrukturertAdresse() instanceof Postboksadresse) {
            Postboksadresse postboksadresse = (Postboksadresse) person.getBostedsadresse().getStrukturertAdresse();
            postadresse.adresselinje1 = "Postboks " + postboksadresse.getPostboksnummer();
        }

        if (person.getBostedsadresse().getStrukturertAdresse() instanceof StedsadresseNorge) {
            StedsadresseNorge stedsadresseNorge = (StedsadresseNorge) person.getBostedsadresse().getStrukturertAdresse();
            if (stedsadresseNorge.getPoststed() != null) {
                postadresse.postnr = stedsadresseNorge.getPoststed().getValue();
            }
        } else if (person.getBostedsadresse().getStrukturertAdresse() instanceof PostboksadresseNorsk) {
            PostboksadresseNorsk postboksadresseNorsk = (PostboksadresseNorsk) person.getBostedsadresse()
                .getStrukturertAdresse();
            if (postboksadresseNorsk.getPoststed() != null) {
                postadresse.postnr = postboksadresseNorsk.getPoststed().getValue();
            }
        }
        if (person.getBostedsadresse().getStrukturertAdresse().getLandkode() != null) {
            postadresse.land = Land.av(person.getBostedsadresse()
                .getStrukturertAdresse()
                .getLandkode()
                .getValue());
        }

        return postadresse;
    }

    private static UstrukturertAdresse mapPostadresse(Bruker person) {
        UstrukturertAdresse postadresse = new UstrukturertAdresse();

        postadresse.adresselinje1 = person.getPostadresse().getUstrukturertAdresse().getAdresselinje1();
        postadresse.adresselinje2 = person.getPostadresse().getUstrukturertAdresse().getAdresselinje2();
        postadresse.adresselinje3 = person.getPostadresse().getUstrukturertAdresse().getAdresselinje3();
        postadresse.adresselinje4 = person.getPostadresse().getUstrukturertAdresse().getAdresselinje4();

        if (person.getPostadresse().getUstrukturertAdresse().getLandkode() != null) {
            postadresse.land = Land.av(person.getPostadresse().getUstrukturertAdresse().getLandkode().getValue());
        }

        return postadresse;
    }

    private static UstrukturertAdresse mapMidlertidigUtland(Bruker person) {
        UstrukturertAdresse postadresse = new UstrukturertAdresse();

        no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseUtland midlertidigPostadresseUtland =
            (no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseUtland) person.getMidlertidigPostadresse();

        if (midlertidigPostadresseUtland.getUstrukturertAdresse() != null) {
            postadresse.adresselinje1 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje1();
            postadresse.adresselinje2 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje2();
            postadresse.adresselinje3 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje3();
            postadresse.adresselinje4 = midlertidigPostadresseUtland.getUstrukturertAdresse().getAdresselinje4();

            if (midlertidigPostadresseUtland.getUstrukturertAdresse().getLandkode() != null) {
                postadresse.land = Land.av(midlertidigPostadresseUtland.getUstrukturertAdresse().getLandkode().getValue());
            }
        }

        return postadresse;
    }

    private static UstrukturertAdresse mapMidlertidigNorge(Bruker person) {
        UstrukturertAdresse postadresse = new UstrukturertAdresse();
        no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge midlertidigPostadresse =
            (no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge) person.getMidlertidigPostadresse();

        final StrukturertAdresse strukturertAdresse = midlertidigPostadresse.getStrukturertAdresse();

        if (strukturertAdresse instanceof no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) {
            no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse gateadresse = (no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse) strukturertAdresse;
            postadresse.adresselinje1 = (Optional.ofNullable(gateadresse.getGatenavn())
                .orElse("") + " " + Optional.ofNullable(gateadresse.getHusnummer() == null ? null : gateadresse.getHusnummer()
                .toString()).orElse("") + Optional.ofNullable(gateadresse.getHusbokstav()).orElse(""));
        } else if (strukturertAdresse instanceof Matrikkeladresse) {
            Matrikkeladresse matrikkeladresse = (Matrikkeladresse) midlertidigPostadresse
                .getStrukturertAdresse();
            postadresse.adresselinje1 = matrikkeladresse.getEiendomsnavn();
        } else if (strukturertAdresse instanceof Postboksadresse) {
            Postboksadresse postboksadresse = (Postboksadresse) midlertidigPostadresse
                .getStrukturertAdresse();
            postadresse.adresselinje1 = "Postboks " + postboksadresse.getPostboksnummer();
        }

        if (strukturertAdresse instanceof StedsadresseNorge) {
            StedsadresseNorge stedsadresseNorge = (StedsadresseNorge) midlertidigPostadresse
                .getStrukturertAdresse();
            if (stedsadresseNorge.getPoststed() != null) {
                postadresse.postnr = stedsadresseNorge.getPoststed().getValue();
            }
        } else if (strukturertAdresse instanceof PostboksadresseNorsk) {
            PostboksadresseNorsk postboksadresseNorsk = (PostboksadresseNorsk) midlertidigPostadresse
                .getStrukturertAdresse();
            if (postboksadresseNorsk.getPoststed() != null) {
                postadresse.postnr = postboksadresseNorsk.getPoststed().getValue();
            }
        }

        if (strukturertAdresse.getLandkode() != null) {
            postadresse.land = Land.av(strukturertAdresse.getLandkode().getValue());
        }

        if (strukturertAdresse.getTilleggsadresse() != null) {
            if (CO_TILLEGGSADRESSETYPE.equalsIgnoreCase(strukturertAdresse.getTilleggsadresseType()) ||
                V_TILLEGGSADRESSETYPE.equalsIgnoreCase(strukturertAdresse.getTilleggsadresseType())) {
                return mapMidlertidigPostAdresseMedTilleggsadresseType(postadresse, strukturertAdresse);
            } else if (strukturertAdresse.getTilleggsadresse().startsWith(CO_TILLEGGSADRESSETYPE) ||
                strukturertAdresse.getTilleggsadresse().startsWith(V_TILLEGGSADRESSETYPE)) {
                return mapMidlertidigPostAdresseMedTilleggsadresse(postadresse, strukturertAdresse);
            }
        }
        return postadresse;
    }

    private static UstrukturertAdresse mapMidlertidigPostAdresseMedTilleggsadresseType(UstrukturertAdresse postadresse, StrukturertAdresse strukturertAdresse) {
        postadresse.adresselinje3 = postadresse.adresselinje2;
        postadresse.adresselinje2 = postadresse.adresselinje1;
        postadresse.adresselinje1 = strukturertAdresse.getTilleggsadresseType() + " " + strukturertAdresse.getTilleggsadresse();
        return postadresse;
    }

    private static UstrukturertAdresse mapMidlertidigPostAdresseMedTilleggsadresse(UstrukturertAdresse postadresse, StrukturertAdresse strukturertAdresse) {
        postadresse.adresselinje3 = postadresse.adresselinje2;
        postadresse.adresselinje2 = postadresse.adresselinje1;
        postadresse.adresselinje1 = strukturertAdresse.getTilleggsadresse();
        return postadresse;
    }
}
