package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.PersonAdresse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class StorbritanniaAdresseSjekker {
    static final String INGEN_ADRESSE_I_NORGE = "No address in Norway";
    static final String UKJENT = "Unknown";
    static final String BOSTED_UTENFOR_NORGE = "Resident outside of Norway";

    private final Persondata persondata;

    StorbritanniaAdresseSjekker(Persondata persondata) {
        this.persondata = persondata;
    }

    List<String> finnGyldigNorskAdresse() {
        return getPersonAdresser()
            .filter(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse(), Landkoder.NO))
            .findFirst()
            .map(personAdresse -> personAdresse.strukturertAdresse().toList())
            .orElse(findAdresseNårIkkeNorskAdresseMenAdresseIUk());
    }

    List<String> finnGyldigStorbritanniaAdresse(Lovvalgsperiode lovvalgsperiode) {
        return getPersonAdresser()
            .filter(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse(), Landkoder.GB))
            .filter(personAdresse -> sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .findFirst()
            .map(personAdresse -> personAdresse.strukturertAdresse().toList())
            .orElse(List.of(UKJENT));
    }

    private List<String> findAdresseNårIkkeNorskAdresseMenAdresseIUk() {
        return getPersonAdresser()
            .filter(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse(), Landkoder.GB))
            .findFirst()
            .map(personAdresse -> List.of(INGEN_ADRESSE_I_NORGE))
            .orElse(findAdresseNårIkkeNorskEllerUkAdresse());
    }

    private List<String> findAdresseNårIkkeNorskEllerUkAdresse() {
        return getPersonAdresser()
            .filter(personAdresse -> personAdresse.strukturertAdresse().getLandkode() != null)
            .findFirst()
            .map(personAdresse -> Stream.concat(
                Stream.of(BOSTED_UTENFOR_NORGE),
                personAdresse.strukturertAdresse().toList().stream()).toList()
            )
            .orElse(List.of(UKJENT));
    }

    private Stream<PersonAdresse> getPersonAdresser() {
        return Stream.of(
                persondata.finnBostedsadresse(),
                persondata.finnOppholdsadresse(),
                persondata.finnKontaktadresse())
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private boolean sjekkAdresseMotLand(StrukturertAdresse adresse, Landkoder landkode) {
        return adresse != null && landkode != null && adresse.getLandkode() != null && adresse.getLandkode().equals(landkode.getKode());
    }

    static boolean sjekkOmAdresseGyldighetErInnenforLovalgsperiode(PersonAdresse personAdresse, Lovvalgsperiode lovvalgsperiode) {
        if (personAdresse.gyldigFraOgMed() == null) return false;
        if (personAdresse.gyldigTilOgMed() == null) return false;

        if (lovvalgsperiode.getTom().isBefore(personAdresse.gyldigFraOgMed())) return false;
        return !lovvalgsperiode.getFom().isAfter(personAdresse.gyldigTilOgMed());
    }
}
