package no.nav.melosys.integrasjon.tps.mapper;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.person.Personstatus;
import no.nav.melosys.domain.dokument.person.Sivilstand;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;

public class PersonMapper {

    private PersonMapper() {
        throw new IllegalStateException("Utility");
    }

    public static PersonDokument mapTilPerson(no.nav.tjeneste.virksomhet.person.v3.informasjon.Person person) {
        PersonDokument dokument = new PersonDokument();
        dokument.setFnr(mapFnr(person.getAktoer()));
        dokument.setSivilstand(mapSivilstand(person.getSivilstand()));
        dokument.setSivilstandGyldighetsperiodeFom(person.getSivilstand() == null ? null
            : KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getSivilstand().getFomGyldighetsperiode()));
        dokument.setStatsborgerskap(person.getStatsborgerskap() == null ? null
            : Land.av(person.getStatsborgerskap().getLand().getValue()));
        dokument.setKjønn(mapKjønn(person.getKjoenn()));
        if (person.getPersonnavn() != null) {
            dokument.setFornavn(person.getPersonnavn().getFornavn());
            dokument.setMellomnavn(person.getPersonnavn().getMellomnavn());
            dokument.setEtternavn(person.getPersonnavn().getEtternavn());
            dokument.setSammensattNavn(person.getPersonnavn().getSammensattNavn());
        }
        dokument.setFødselsdato(person.getFoedselsdato() == null ? null
            : KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getFoedselsdato().getFoedselsdato()));
        dokument.setDødsdato(person.getDoedsdato() == null ? null
            : KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getDoedsdato().getDoedsdato()));
        dokument.setDiskresjonskode(mapDiskresjonskode(person.getDiskresjonskode()));
        dokument.setPersonstatus(person.getPersonstatus() == null ? null
            : Personstatus.valueOf(person.getPersonstatus().getPersonstatus().getValue()));
        dokument.setBostedsadresse(AdresseMapper.mapTilBostedsadresse(person.getBostedsadresse()));
        dokument.setPostadresse(AdresseMapper.mapTilPostadresse(person.getPostadresse()));
        if (person instanceof Bruker) {
            dokument.setMidlertidigPostadresse(
                    AdresseMapper.mapTilMidlertidigPostadresse(((Bruker) person).getMidlertidigPostadresse()));
            dokument.setGjeldendePostadresse(AdresseMapper.mapTilGjeldendePostadresse((Bruker) person));
        }
        dokument.setFamiliemedlemmer(FamiliemedlemMapper.mapTilFamiliemedlemmer(person.getHarFraRolleI()));
        return dokument;
    }

    public static void berikFamiliemedlemMedOpplysninger(Familiemedlem familiemedlem, PersonDokument dokument, String ident) {
        if (familiemedlem.erBarn()) {
            dokument.hentAnnenForelder(ident)
                .ifPresent(forelder -> familiemedlem.fnrAnnenForelder = forelder.fnr);
            familiemedlem.fødselsdato = dokument.getFødselsdato();
        } else if (familiemedlem.erEktefellePartnerSamboer()) {
            familiemedlem.sivilstand = dokument.getSivilstand();
            familiemedlem.sivilstandGyldighetsperiodeFom = dokument.getSivilstandGyldighetsperiodeFom();
        }
    }

    static String mapFnr(Aktoer aktoer) {
        if (aktoer instanceof PersonIdent) {
            return ((PersonIdent) aktoer).getIdent().getIdent();
        }
        return null;
    }

    private static Sivilstand mapSivilstand(no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstand sivilstand) {
        if (sivilstand == null) {
            return null;
        }
        Sivilstand s = new Sivilstand();
        s.setKode(sivilstand.getSivilstand().getValue());
        return s;
    }

    private static KjoennsType mapKjønn(Kjoenn kjoenn) {
        if (kjoenn == null) {
            return null;
        }
        return new KjoennsType(kjoenn.getKjoenn().getValue());
    }

    private static Diskresjonskode mapDiskresjonskode(Diskresjonskoder diskresjonskode) {
        if (diskresjonskode == null) {
            return null;
        }
        Diskresjonskode d = new Diskresjonskode();
        d.setKode(diskresjonskode.getValue());
        return d;
    }
}
