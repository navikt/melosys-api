package no.nav.melosys.service.persondata.mapping.adresse;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.service.kodeverk.KodeverkService;

public final class BostedsadresseOversetter {
    private BostedsadresseOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Bostedsadresse oversett(
        Collection<no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse> bostedsadresse,
        KodeverkService kodeverkService) {
        return bostedsadresse.stream()
            .max(Comparator.comparing(HarMetadata::hentDatoSistRegistrert))
            .map(a -> BostedsadresseOversetter.oversett(a, kodeverkService))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .orElse(null);
    }

    public static Optional<Bostedsadresse> oversett(
        no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse bostedsadressePDL,
        KodeverkService kodeverkService) {
        StrukturertAdresse strukturertAdresse = null;

        if (bostedsadressePDL.vegadresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(bostedsadressePDL.vegadresse(),
                kodeverkService);
        } else if (bostedsadressePDL.utenlandskAdresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(bostedsadressePDL.utenlandskAdresse());
        } else if (bostedsadressePDL.matrikkeladresse() != null) {
            strukturertAdresse = PdlAdresseformatOversetter.lagStrukturertAdresse(bostedsadressePDL.matrikkeladresse(),
                kodeverkService);
        } else if (bostedsadressePDL.ukjentBosted() != null) {
            return Optional.empty();
        }

        return Optional.of(new Bostedsadresse(
            strukturertAdresse,
            bostedsadressePDL.coAdressenavn(),
            KonverteringsUtils.localDateTimeToLocalDate(bostedsadressePDL.gyldigFraOgMed()),
            KonverteringsUtils.localDateTimeToLocalDate(bostedsadressePDL.gyldigTilOgMed()),
            bostedsadressePDL.metadata().master(),
            bostedsadressePDL.hentKilde(),
            bostedsadressePDL.metadata().historisk())
        );
    }
}
