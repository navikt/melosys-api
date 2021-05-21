package no.nav.melosys.integrasjon.tps.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner;

import static no.nav.melosys.domain.dokument.person.Familierelasjon.*;

public final class FamiliemedlemMapper {
    private static final Collection<Familierelasjon> FAMILIERELASJONER = Set.of(BARN, EKTE, REPA, SAM);

    private FamiliemedlemMapper() {
        throw new IllegalStateException("Utility");
    }

    static List<Familiemedlem> mapTilFamiliemedlemmer(
        List<no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon> familierelasjoner) {
        if (familierelasjoner == null) {
            return new ArrayList<>();
        }
        return familierelasjoner.stream()
            .filter(erFamilierelasjonRelevant())
            .map(familierelasjon -> {
                Familiemedlem familiemedlem = new Familiemedlem();
                familiemedlem.fnr = PersonMapper.mapFnr(familierelasjon.getTilPerson().getAktoer());
                familiemedlem.navn = familierelasjon.getTilPerson().getPersonnavn().getSammensattNavn();
                familiemedlem.familierelasjon = mapFamilierelasjon(familierelasjon.getTilRolle());
                familiemedlem.borMedBruker = familierelasjon.isHarSammeBosted();
                return familiemedlem;
            }).collect(Collectors.toList());
    }

    private static Predicate<no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon> erFamilierelasjonRelevant() {
        return familierelasjonTps -> FAMILIERELASJONER.contains(mapFamilierelasjon(familierelasjonTps.getTilRolle()));
    }

    private static Familierelasjon mapFamilierelasjon(Familierelasjoner tilRolle) {
        return Familierelasjon.valueOf(tilRolle.getValue());
    }
}
