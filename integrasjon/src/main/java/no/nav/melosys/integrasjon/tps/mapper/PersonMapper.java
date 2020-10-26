package no.nav.melosys.integrasjon.tps.mapper;

import java.time.LocalDate;

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
        dokument.sivilstandGyldighetsperiodeFom = KonverteringsUtils.xmlGregorianCalendarToLocalDate(person.getSivilstand().getFomGyldighetsperiode());
        dokument.statsborgerskap = Land.av(person.getStatsborgerskap().getLand().getValue());
        dokument.kjønn = mapKjønn(person.getKjoenn());
        dokument.fornavn = person.getPersonnavn().getFornavn();
        dokument.mellomnavn = person.getPersonnavn().getMellomnavn();
        dokument.etternavn = person.getPersonnavn().getEtternavn();
        dokument.sammensattNavn = person.getPersonnavn().getSammensattNavn();
        dokument.fødselsdato = mapFødselsdato(person.getFoedselsdato());
        dokument.dødsdato = mapDødsdato(person.getDoedsdato());
        dokument.diskresjonskode = mapDiskresjonskode(person.getDiskresjonskode());
        dokument.personstatus = mapPersonstatus(person.getPersonstatus());
        dokument.bostedsadresse = AdresseMapper.mapTilBostedsadresse(person.getBostedsadresse());
        dokument.postadresse = AdresseMapper.mapTilPostadresse(person.getPostadresse());
        if (person instanceof Bruker) {
            dokument.midlertidigPostadresse = AdresseMapper.mapTilMidlertidigPostadresse(((Bruker) person).getMidlertidigPostadresse());
        }
        dokument.familiemedlemmer = FamiliemedlemMapper.mapTilFamiliemedlemmer(person.getHarFraRolleI());
        return dokument;
    }

    public static void berikFamiliemedlemMedOpplysninger(Familiemedlem familiemedlem, PersonDokument dokument, String ident) {
        if (familiemedlem.erBarn()) {
            dokument.hentAnnenForelder(ident)
                .ifPresent(forelder -> familiemedlem.fnrAnnenForelder = forelder.fnr);
        } else if (familiemedlem.erEktefellePartnerSamboer()) {
            familiemedlem.sivilstand = dokument.sivilstand;
            familiemedlem.sivilstandGyldighetsperiodeFom = dokument.sivilstandGyldighetsperiodeFom;
        }
    }

    static String mapFnr(Aktoer aktoer) {
        return ((PersonIdent) aktoer).getIdent().getIdent();
    }

    private static Sivilstand mapSivilstand(no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstand sivilstand) {
        Sivilstand s = new Sivilstand();
        s.setKode(sivilstand.getSivilstand().getValue());
        return s;
    }

    private static KjoennsType mapKjønn(Kjoenn kjoenn) {
        KjoennsType kt = new KjoennsType();
        kt.setKode(kjoenn.getKjoenn().getValue());
        return kt;
    }

    private static LocalDate mapFødselsdato(Foedselsdato foedselsdato) {
        return KonverteringsUtils.xmlGregorianCalendarToLocalDate(foedselsdato.getFoedselsdato());
    }

    private static LocalDate mapDødsdato(Doedsdato doedsdato) {
        if (doedsdato == null) {
            return null;
        }
        return KonverteringsUtils.xmlGregorianCalendarToLocalDate(doedsdato.getDoedsdato());
    }

    private static Diskresjonskode mapDiskresjonskode(Diskresjonskoder diskresjonskode) {
        if (diskresjonskode == null) {
            return null;
        }
        Diskresjonskode d = new Diskresjonskode();
        d.setKode(diskresjonskode.getValue());
        return d;
    }

    private static Personstatus mapPersonstatus(no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus personstatus) {
        return Personstatus.valueOf(personstatus.getPersonstatus().getValue());
    }
}
