package no.nav.melosys.integrasjon.tps.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner;

public class FamiliemedlemMapper {

    private FamiliemedlemMapper() {
        throw new IllegalStateException("Utility");
    }

    static List<Familiemedlem> mapTilFamiliemedlemmer(List<no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon> harFraRolleI) {
        if (harFraRolleI == null) {
            return new ArrayList<>();
        }
        return harFraRolleI.stream()
            .map(familierelasjon -> {
                Familiemedlem familiemedlem = new Familiemedlem();
                familiemedlem.fnr = PersonopplysningMapper.mapFnr(familierelasjon.getTilPerson().getAktoer());
                familiemedlem.navn = familierelasjon.getTilPerson().getPersonnavn().getSammensattNavn();
                familiemedlem.familierelasjon = mapFamilierelasjon(familierelasjon.getTilRolle());
                return familiemedlem;
            }).collect(Collectors.toList());
    }

    private static Familierelasjon mapFamilierelasjon(Familierelasjoner tilRolle) {
        return Familierelasjon.valueOf(tilRolle.getValue());
    }
}
