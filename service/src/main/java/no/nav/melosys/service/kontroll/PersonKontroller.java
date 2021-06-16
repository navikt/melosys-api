package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.Persondata;

public final class PersonKontroller {

    private PersonKontroller() {
    }

    public static boolean personDød(Persondata persondata) {
        return persondata.getDødsdato() != null;
    }

    public static boolean personBosattINorge(Persondata persondata) {
        Bostedsadresse bostedsadresse = persondata.getBostedsadresse();

        return bostedsadresse != null
            && bostedsadresse.getLand() != null
            && Land.NORGE.equals(bostedsadresse.getLand().getKode());
    }

    public static boolean harRegistrertBostedsadresse(Persondata persondata, BehandlingsgrunnlagData behandlingsgrunnlagData) {
        return !persondata.manglerBostedsadresse() || !behandlingsgrunnlagData.bosted.oppgittAdresse.erTom();
    }
}
