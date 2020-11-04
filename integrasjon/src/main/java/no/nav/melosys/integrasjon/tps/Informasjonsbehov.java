package no.nav.melosys.integrasjon.tps;

import java.util.Collections;
import java.util.Set;

public enum Informasjonsbehov {
    INGEN(Collections.emptySet()),
    STANDARD(Set.of(no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE)),
    MED_FAMILIERELASJONER(Set.of(
        no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.ADRESSE,
        no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.FAMILIERELASJONER));

    private final Set<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> koder;

    Informasjonsbehov(Set<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> koder) {
        this.koder = koder;
    }

    Set<no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov> getKoder() {
        return koder;
    }
}
