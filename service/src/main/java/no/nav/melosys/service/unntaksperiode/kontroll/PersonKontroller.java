package no.nav.melosys.service.unntaksperiode.kontroll;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;

final class PersonKontroller {

    private PersonKontroller() {
    }

    static Unntak_periode_begrunnelser personDød(KontrollData kontrollData) {
        PersonDokument personDokument = kontrollData.personDokument;
        return personDokument.dødsdato != null ?
            Unntak_periode_begrunnelser.PERSON_DOD : null;
    }

    static Unntak_periode_begrunnelser personBosattINorge(KontrollData kontrollData) {
        Bostedsadresse bostedsadresse = kontrollData.personDokument.bostedsadresse;

        return bostedsadresse != null
            && bostedsadresse.getLand() != null
            && Land.NORGE.equals(bostedsadresse.getLand().getKode())
            ? Unntak_periode_begrunnelser.BOSATT_I_NORGE : null;
    }
}
