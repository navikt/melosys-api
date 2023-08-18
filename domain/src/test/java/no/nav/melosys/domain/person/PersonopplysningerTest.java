package no.nav.melosys.domain.person;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonopplysningerTest {

    @Test
    void hentGjeldendePostadresse_bareBostedsadresse_lagPostadresseFraBostedsadresse() {
        Postadresse gjeldendePostadresse =
            lagPersonopplysninger(Collections.emptyList(), Collections.emptyList(), lagBostedsadresse()).hentGjeldendePostadresse();

        assertThat(gjeldendePostadresse.adresselinje1()).isEqualTo("gatenavnFraBostedsadresse");
    }

    @Test
    void hentGjeldendePostadresse_medKontaktadresser_lagPostadresseFraKontaktadressePDL() {
        Postadresse gjeldendePostadresse =
            lagPersonopplysninger(lagKontaktadresser(), lagOppholdsadresser(), lagBostedsadresse()).hentGjeldendePostadresse();

        assertThat(gjeldendePostadresse.adresselinje1()).isEqualTo("gatenavnKontaktadressePDL");
    }

    @Test
    void hentGjeldendePostadresse_medOppholdsadresserOgUtenKontaktadresser_lagPostadresseFraOppholdsadresseFreg() {
        Postadresse gjeldendePostadresse =
            lagPersonopplysninger(Collections.emptyList(), lagOppholdsadresser(), lagBostedsadresse()).hentGjeldendePostadresse();

        assertThat(gjeldendePostadresse.adresselinje1()).isEqualTo("gatenavnOppholdsadresseFreg");
    }

    @Test
    void hentGjeldendePostadresse_medBareKontakadresseFreg_lagPostadresseFraKontakadresseFreg() {
        Postadresse gjeldendePostadresse =
            lagPersonopplysninger(lagKontaktadresseFraFreg(), Collections.emptyList(), null).hentGjeldendePostadresse();

        assertThat(gjeldendePostadresse.adresselinje1()).isEqualTo("gatenavnKontaktadresseFreg");
    }

    @Test
    void hentGjeldendePostadresse_medOppholdsadresserOgUtenBostedsadresse_lagPostadresseFraKontaktadressePDL() {
        Postadresse gjeldendePostadresse =
            lagPersonopplysninger(lagKontaktadresser(), lagOppholdsadresser(), null).hentGjeldendePostadresse();

        assertThat(gjeldendePostadresse.adresselinje1()).isEqualTo("gatenavnKontaktadressePDL");
    }

    private Personopplysninger lagPersonopplysninger(Collection<Kontaktadresse> kontaktadresser,
                                                     Collection<Oppholdsadresse> oppholdsadresser, Bostedsadresse bostedsadresse) {
        return new Personopplysninger(Collections.emptyList(), bostedsadresse, null, null, null, null, null,
            kontaktadresser, null, oppholdsadresser, Collections.emptyList());
    }

    private Bostedsadresse lagBostedsadresse() {
        return new Bostedsadresse(new StrukturertAdresse("gatenavnFraBostedsadresse", null, null, null, null, null),
            null, null, null, null, null, false);
    }

    private Collection<Kontaktadresse> lagKontaktadresser() {
        return Set.of(
            new Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadressePDL"),
                null,
                null,
                null,
                null,
                Master.PDL.name(),
                null,
                LocalDateTime.MAX,
                false
            ),
            new Kontaktadresse(
                lagStrukturertAdresse("gammelGatenavnKontaktadressePDL"),
                null,
                null,
                null,
                null,
                Master.PDL.name(),
                null,
                LocalDateTime.MIN,
                false
            ),
            new Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadresseFreg"),
                null,
                null,
                null,
                null,
                Master.FREG.name(),
                null,
                LocalDateTime.MAX,
                false
            )
        );
    }

    private Collection<Kontaktadresse> lagKontaktadresseFraFreg() {
        return Set.of(
            new Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadresseFreg"),
                null,
                null,
                null,
                null,
                "Freg",
                null,
                LocalDateTime.MAX,
                false
            )
        );
    }

    private Collection<Oppholdsadresse> lagOppholdsadresser() {
        return Set.of(
            new Oppholdsadresse(
                lagStrukturertAdresse("gammelGatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name(),
                null,
                LocalDateTime.MIN,
                false
            ),
            new Oppholdsadresse(
                lagStrukturertAdresse("gatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name(),
                null,
                LocalDateTime.MAX,
                false
            )
        );
    }

    private StrukturertAdresse lagStrukturertAdresse(String gatenavn) {
        return new StrukturertAdresse(gatenavn, null, "1234", null, null, null);
    }
}
