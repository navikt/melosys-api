package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;

public final class PersonKontroller {

    private PersonKontroller() {
    }

    public static boolean personDød(PersonDokument personDokument) {
        return personDokument.dødsdato != null;
    }

    public static boolean personBosattINorge(PersonDokument personDokument) {
        Bostedsadresse bostedsadresse = personDokument.bostedsadresse;

        return bostedsadresse != null
            && bostedsadresse.getLand() != null
            && Land.NORGE.equals(bostedsadresse.getLand().getKode());
    }
}
