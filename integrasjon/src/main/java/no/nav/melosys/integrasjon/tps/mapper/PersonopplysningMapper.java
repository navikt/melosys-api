package no.nav.melosys.integrasjon.tps.mapper;

import java.time.LocalDate;

import no.nav.melosys.domain.Personopplysning;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.Personstatus;
import no.nav.melosys.domain.dokument.person.Sivilstand;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*;

public class PersonopplysningMapper {

    private PersonopplysningMapper() {
        throw new IllegalStateException("Utility");
    }

    public static Personopplysning mapTilPersonopplysning(Person person) {
        Personopplysning p = new Personopplysning();
        p.fnr = mapFnr(person.getAktoer());
        p.sivilstand = mapSivilstand(person.getSivilstand());
        p.statsborgerskap = Land.av(person.getStatsborgerskap().getLand().getValue());
        p.kjønn = mapKjønn(person.getKjoenn());
        p.fornavn = person.getPersonnavn().getFornavn();
        p.mellomnavn = person.getPersonnavn().getMellomnavn();
        p.etternavn = person.getPersonnavn().getEtternavn();
        p.sammensattNavn = person.getPersonnavn().getSammensattNavn();
        p.fødselsdato = mapFødselsdato(person.getFoedselsdato());
        p.dødsdato = mapDødsdato(person.getDoedsdato());
        p.diskresjonskode = mapDiskresjonskode(person.getDiskresjonskode());
        p.personstatus = mapPersonstatus(person.getPersonstatus());
        p.bostedsadresse = AdresseMapper.mapTilBostedsadresse(person.getBostedsadresse());
        p.postadresse = AdresseMapper.mapTilPostadresse(person.getPostadresse());
        if (person instanceof Bruker) {
            p.midlertidigPostadresse = AdresseMapper.mapTilMidlertidigPostadresse(((Bruker) person).getMidlertidigPostadresse());
        }
        p.familiemedlemmer = FamiliemedlemMapper.mapTilFamiliemedlemmer(person.getHarFraRolleI());
        return p;
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
