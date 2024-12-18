package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.PersonAdresse;


public class TrygdeavtaleAdresseSjekker {
    static final String INGEN_ADRESSE_I_NORGE = "No address in Norway";
    static final String UKJENT = "Unknown";
    static final String BOSTED_UTENFOR_NORGE = "Resident outside of Norway";

    private final Persondata persondata;

    TrygdeavtaleAdresseSjekker(Persondata persondata) {
        this.persondata = persondata;
    }

    List<String> finnGyldigNorskAdresse(Land_iso2 soknadsland) {
        return getPersonAdresser()
            .filter(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse(), Land_iso2.NO))
            .findFirst()
            .map(personAdresse -> personAdresse.strukturertAdresse().toList())
            .orElse(finnAdresseNårIkkeNorskAdresseMenAdresseISoknadsland(soknadsland));
    }

    List<String> finnGyldigTrygdeavtaleAdresse(Lovvalgsperiode lovvalgsperiode, Land_iso2 soknadsland) {
        return getPersonAdresser()
            .filter(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse(), soknadsland))
            .filter(personAdresse -> sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .findFirst()
            .map(personAdresse -> personAdresse.strukturertAdresse().toList())
            .orElse(List.of(UKJENT));
    }

    private List<String> finnAdresseNårIkkeNorskAdresseMenAdresseISoknadsland(Land_iso2 soknadsland) {
        boolean harAdresseISoknadsland = getPersonAdresser()
            .anyMatch(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse(), soknadsland));

        if (harAdresseISoknadsland) return List.of(INGEN_ADRESSE_I_NORGE);
        return findAdresseNårIkkeNorskEllerSoknadslandadresse();
    }

    private List<String> findAdresseNårIkkeNorskEllerSoknadslandadresse() {
        return getPersonAdresser()
            .map(this::hentStrukturertAdresse)
            .filter(strukturertAdresse -> strukturertAdresse.getLandkode() != null)
            .findFirst()
            .map(strukturertAdresse -> {
                if (Arrays.stream(Land_iso2.values()).noneMatch(landIso2 -> landIso2.getKode().equals(strukturertAdresse.getLandkode()))) {
                    return Stream.of(BOSTED_UTENFOR_NORGE).toList();
                }
                return Stream.concat(
                    Stream.of(BOSTED_UTENFOR_NORGE),
                    strukturertAdresse.toList().stream()
                ).toList();
            })
            .orElse(List.of(UKJENT));
    }

    private StrukturertAdresse hentStrukturertAdresse(PersonAdresse personAdresse) {
        return personAdresse instanceof Kontaktadresse kontaktadresse
            ? kontaktadresse.hentEllerLagStrukturertAdresse()
            : personAdresse.strukturertAdresse();
    }

    private Stream<PersonAdresse> getPersonAdresser() {
        return Stream.of(
                persondata.finnBostedsadresse(),
                persondata.finnOppholdsadresse(),
                persondata.finnKontaktadresse())
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private boolean sjekkAdresseMotLand(StrukturertAdresse adresse, Land_iso2 landkode) {
        return adresse != null && landkode != null && adresse.getLandkode() != null && adresse.getLandkode().equals(landkode.getKode());
    }

    static boolean sjekkOmAdresseGyldighetErInnenforLovalgsperiode(PersonAdresse personAdresse, Lovvalgsperiode lovvalgsperiode) {
        if (personAdresse.gyldigFraOgMed() == null) return false;

        if (lovvalgsperiode.getTom() != null && lovvalgsperiode.getTom().isBefore(personAdresse.gyldigFraOgMed())) return false;
        if (personAdresse.gyldigTilOgMed() == null) return true;
        return !lovvalgsperiode.getFom().isAfter(personAdresse.gyldigTilOgMed());
    }
}
