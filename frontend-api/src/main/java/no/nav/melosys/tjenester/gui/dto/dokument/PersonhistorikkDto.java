package no.nav.melosys.tjenester.gui.dto.dokument;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.*;

public class PersonhistorikkDto {

    public List<BostedsadressePeriodeDto> bostedsadressePeriodeListe = new ArrayList<>();

    public List<PostadressePeriodeDto> postadressePeriodeListe = new ArrayList<>();

    public List<MidlertidigPostadressePeriodeDto> midlertidigAdressePeriodeListe = new ArrayList<>();

    public PersonhistorikkDto() {
    }

    public PersonhistorikkDto(PersonhistorikkDokument personhistorikk) {
        bostedsadressePeriodeListe = personhistorikk.bostedsadressePeriodeListe.stream()
            .map(BostedsadressePeriodeDto::new)
            .collect(Collectors.toList());

        postadressePeriodeListe = personhistorikk.postadressePeriodeListe.stream()
            .map(PostadressePeriodeDto::new)
            .collect(Collectors.toList());

        midlertidigAdressePeriodeListe = personhistorikk.midlertidigAdressePeriodeListe.stream()
            .map(MidlertidigPostadressePeriodeDto::new)
            .collect(Collectors.toList());
    }

    class BostedsadressePeriodeDto {
        final Bostedsadresse bostedsadresse;
        final Periode periode;

        BostedsadressePeriodeDto(BostedsadressePeriode bostedsadressePeriode) {
            this.bostedsadresse = bostedsadressePeriode.bostedsadresse;
            this.periode = bostedsadressePeriode.periode;
        }
    }

    class PostadressePeriodeDto {
        final UstrukturertAdresse postadresse;
        final Periode periode;

        PostadressePeriodeDto(PostadressePeriode postadressePeriode) {
            this.postadresse = postadressePeriode.postadresse;
            this.periode = postadressePeriode.periode;
        }
    }

    class MidlertidigPostadressePeriodeDto {
        final MidlertidigPostadresse midlertidigPostadresse;
        Periode periode;

        MidlertidigPostadressePeriodeDto(MidlertidigPostadresse midlertidigPostadresse) {
            this.midlertidigPostadresse = midlertidigPostadresse;
        }
    }
}
