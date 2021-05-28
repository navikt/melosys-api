package no.nav.melosys.tjenester.gui.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.PostadressePeriode;

public class PersonhistorikkDto {

    public final List<BostedsadressePeriodeDto> bostedsadressePerioder;

    public final List<PostadressePeriodeDto> postadressePerioder;

    public final List<MidlertidigPostadressePeriodeDto> midlertidigAdressePerioder;

    public PersonhistorikkDto() {
        bostedsadressePerioder = new ArrayList<>();
        postadressePerioder = new ArrayList<>();
        midlertidigAdressePerioder = new ArrayList<>();
    }

    public PersonhistorikkDto(PersonhistorikkDokument personhistorikk) {
        bostedsadressePerioder = personhistorikk.bostedsadressePeriodeListe.stream()
            .map(BostedsadressePeriodeDto::new)
            .collect(Collectors.toList());

        postadressePerioder = personhistorikk.postadressePeriodeListe.stream()
            .map(PostadressePeriodeDto::new)
            .collect(Collectors.toList());

        midlertidigAdressePerioder = personhistorikk.midlertidigAdressePeriodeListe.stream()
            .map(MidlertidigPostadressePeriodeDto::new)
            .collect(Collectors.toList());
    }

    public class BostedsadressePeriodeDto {
        public final Bostedsadresse bostedsadresse;
        public final Periode periode;

        BostedsadressePeriodeDto(BostedsadressePeriode bostedsadressePeriode) {
            this.bostedsadresse = bostedsadressePeriode.bostedsadresse;
            this.periode = bostedsadressePeriode.periode;
        }
    }

    public class PostadressePeriodeDto {
        public final UstrukturertAdresse postadresse;
        public final Periode periode;

        PostadressePeriodeDto(PostadressePeriode postadressePeriode) {
            this.postadresse = UstrukturertAdresse.av(postadressePeriode.postadresse);
            this.periode = postadressePeriode.periode;
        }
    }

    public class MidlertidigPostadressePeriodeDto {
        public final MidlertidigPostadresse midlertidigAdresse;
        public final Periode periode;

        MidlertidigPostadressePeriodeDto(MidlertidigPostadresse midlertidigPostadresse) {
            this.midlertidigAdresse = midlertidigPostadresse;
            this.periode = midlertidigPostadresse.postleveringsPeriode;
        }
    }
}
