package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.avklartefakta.AvklartFamiliemedlem;
import no.nav.melosys.domain.avklartefakta.AvklarteMedfolgendeFamiliemedlemmer;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.dokument.brev.IkkeOmfattetBarn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class MedfolgendeFamilieGrunnlag {

    private final AvklarteMedfolgendeFamiliemedlemmer avklarteMedfolgendeFamiliemedlemmer;
    private final PersonDokument person;

    public MedfolgendeFamilieGrunnlag(AvklarteMedfolgendeFamiliemedlemmer avklarteMedfolgendeFamiliemedlemmer, PersonDokument person) {
        this.avklarteMedfolgendeFamiliemedlemmer = avklarteMedfolgendeFamiliemedlemmer;
        this.person = person;
    }

    public List<String> hentOmfattedeBarn() {
        return person.familiemedlemmer.stream()
            .filter(familiemedlem -> familiemedlem.familierelasjon.equals(Familierelasjon.BARN))
            .filter(familiemedlem -> hentOmfattedeFnr().contains(familiemedlem.fnr))
            .map(familiemedlem -> familiemedlem.navn)
            .collect(Collectors.toList());
    }

    public List<IkkeOmfattetBarn> hentIkkeOmfattedeBarn() {
        Map<String, AvklartFamiliemedlem> ikkeOmfattedeFamiliemedlemmer = hentIkkeOmfattedeFamiliemedlemmer();
        return person.familiemedlemmer.stream()
            .filter(familiemedlem -> familiemedlem.familierelasjon.equals(Familierelasjon.BARN))
            .filter(familiemedlem -> ikkeOmfattedeFamiliemedlemmer.containsKey(familiemedlem.fnr))
            .map(familiemedlem -> new IkkeOmfattetBarn(
                familiemedlem.navn,
                ikkeOmfattedeFamiliemedlemmer.get(familiemedlem.fnr).begrunnelse)
            ).collect(Collectors.toList());
    }

    public String hentBegrunnelse() {
        return avklarteMedfolgendeFamiliemedlemmer.begrunnelseFritekst;
    }

    private Set<String> hentOmfattedeFnr() {
        return avklarteMedfolgendeFamiliemedlemmer.avklarteFamiliemedlemmer.stream()
            .filter(AvklartFamiliemedlem::erOmfattet)
            .map(af -> af.fnr)
            .collect(Collectors.toSet());
    }

    private Map<String, AvklartFamiliemedlem> hentIkkeOmfattedeFamiliemedlemmer() {
        return avklarteMedfolgendeFamiliemedlemmer.avklarteFamiliemedlemmer.stream()
            .filter(not(AvklartFamiliemedlem::erOmfattet))
            .collect(Collectors.toMap(af -> af.fnr, af -> af));
    }
}
