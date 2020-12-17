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
        dokument.fnr = mapFnr(person.getAktoer());
        dokument.sivilstand = mapSivilstand(person.getSivilstand());
        dokument.sivilstandGyldighetsperiodeFom = person.getSivilstand() == null ? null
            : KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getSivilstand().getFomGyldighetsperiode());
        dokument.statsborgerskap = person.getStatsborgerskap() == null ? null
            : Land.av(person.getStatsborgerskap().getLand().getValue());
        dokument.kjønn = mapKjønn(person.getKjoenn());
        if (person.getPersonnavn() != null) {
            dokument.fornavn = person.getPersonnavn().getFornavn();
            dokument.mellomnavn = person.getPersonnavn().getMellomnavn();
            dokument.etternavn = person.getPersonnavn().getEtternavn();
            dokument.sammensattNavn = person.getPersonnavn().getSammensattNavn();
        }
        dokument.fødselsdato = person.getFoedselsdato() == null ? null
            : KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getFoedselsdato().getFoedselsdato());
        dokument.dødsdato = person.getDoedsdato() == null ? null
            : KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getDoedsdato().getDoedsdato());
        dokument.diskresjonskode = mapDiskresjonskode(person.getDiskresjonskode());
        dokument.personstatus = person.getPersonstatus() == null ? null
            : Personstatus.valueOf(person.getPersonstatus().getPersonstatus().getValue());
        dokument.bostedsadresse = AdresseMapper.mapTilBostedsadresse(person.getBostedsadresse());
        dokument.postadresse = AdresseMapper.mapTilPostadresse(person.getPostadresse());
        if (person instanceof Bruker) {
            dokument.midlertidigPostadresse = AdresseMapper.mapTilMidlertidigPostadresse(((Bruker) person).getMidlertidigPostadresse());
            dokument.gjeldendePostadresse = AdresseMapper.mapTilGjeldendePostadresse((Bruker) person);
        }
        dokument.familiemedlemmer = FamiliemedlemMapper.mapTilFamiliemedlemmer(person.getHarFraRolleI());
        return dokument;
    }

    public static void berikFamiliemedlemMedOpplysninger(Familiemedlem familiemedlem, PersonDokument dokument, String ident) {
        if (familiemedlem.erBarn()) {
            dokument.hentAnnenForelder(ident)
                .ifPresent(forelder -> familiemedlem.fnrAnnenForelder = forelder.fnr);
            familiemedlem.fødselsdato = dokument.fødselsdato;
        } else if (familiemedlem.erEktefellePartnerSamboer()) {
            familiemedlem.sivilstand = dokument.sivilstand;
            familiemedlem.sivilstandGyldighetsperiodeFom = dokument.sivilstandGyldighetsperiodeFom;
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
        KjoennsType kt = new KjoennsType();
        kt.setKode(kjoenn.getKjoenn().getValue());
        return kt;
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
