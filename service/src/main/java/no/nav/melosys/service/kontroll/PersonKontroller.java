package no.nav.melosys.service.kontroll;

import java.util.Optional;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;

public final class PersonKontroller {
    private static final String NORGE_ISO2_LANDKODE = "NO";

    private PersonKontroller() {
    }

    public static boolean erPersonDød(Persondata persondata) {
        return persondata.erPersonDød();
    }

    public static boolean personBosattINorge(Persondata persondata) {
        Optional<Bostedsadresse> bostedsadresseOptional = persondata.finnBostedsadresse();

        return bostedsadresseOptional.isPresent()
            && bostedsadresseOptional.get().strukturertAdresse().getLandkode() != null
            && NORGE_ISO2_LANDKODE.equals(bostedsadresseOptional.get().strukturertAdresse().getLandkode());
    }

    public static boolean harRegistrertBostedsadresse(Persondata persondata, BehandlingsgrunnlagData behandlingsgrunnlagData) {
        return !persondata.manglerBostedsadresse() || !behandlingsgrunnlagData.bosted.oppgittAdresse.erTom();
    }
}
