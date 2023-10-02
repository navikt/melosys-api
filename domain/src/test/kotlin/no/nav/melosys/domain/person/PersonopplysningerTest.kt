package no.nav.melosys.domain.person

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PersonopplysningerTest {
    @Test
    fun hentGjeldendePostadresse_bareBostedsadresse_lagPostadresseFraBostedsadresse() {
        lagPersonopplysninger(
            emptyList(),
            emptyList(),
            lagBostedsadresse()
        ).hentGjeldendePostadresse()!!.adresselinje1.shouldBe("gatenavnFraBostedsadresse")
    }

    @Test
    fun hentGjeldendePostadresse_ugyldigeKontaktadresser_lagPostadresseFraBostedsadresse() {
        lagPersonopplysninger(
            lagUgyldigeKontaktadresser(),
            emptyList(),
            lagBostedsadresse()
        ).hentGjeldendePostadresse()!!.adresselinje1.shouldBe("gatenavnFraBostedsadresse")
    }

    @Test
    fun hentGjeldendePostadresse_medKontaktadresser_lagPostadresseFraKontaktadressePDL() {
        lagPersonopplysninger(
            lagKontaktadresser(),
            lagOppholdsadresser(),
            lagBostedsadresse()
        ).hentGjeldendePostadresse()!!.adresselinje1.shouldBe("gatenavnKontaktadressePDL")
    }

    @Test
    fun hentGjeldendePostadresse_medOppholdsadresserOgUtenKontaktadresser_lagPostadresseFraOppholdsadresseFreg() {
        lagPersonopplysninger(
            emptyList(),
            lagOppholdsadresser(),
            lagBostedsadresse()
        ).hentGjeldendePostadresse()!!.adresselinje1.shouldBe("gatenavnOppholdsadresseFreg")
    }

    @Test
    fun hentGjeldendePostadresse_medBareKontakadresseFreg_lagPostadresseFraKontakadresseFreg() {
        lagPersonopplysninger(
            lagKontaktadresseFraFreg(),
            emptyList(),
            null
        ).hentGjeldendePostadresse()!!.adresselinje1.shouldBe("gatenavnKontaktadresseFreg")
    }

    @Test
    fun hentGjeldendePostadresse_medOppholdsadresserOgUtenBostedsadresse_lagPostadresseFraKontaktadressePDL() {
        lagPersonopplysninger(
            lagKontaktadresser(),
            lagOppholdsadresser(),
            null
        ).hentGjeldendePostadresse()!!.adresselinje1.shouldBe("gatenavnKontaktadressePDL")
    }

    private fun lagPersonopplysninger(
        kontaktadresser: Collection<Kontaktadresse>,
        oppholdsadresser: Collection<Oppholdsadresse>, bostedsadresse: Bostedsadresse?
    ): Personopplysninger {
        return Personopplysninger(
            emptyList(), bostedsadresse, null, null, null, null, null,
            kontaktadresser, null, oppholdsadresser, emptyList()
        )
    }

    private fun lagBostedsadresse(): Bostedsadresse {
        return Bostedsadresse(
            StrukturertAdresse("gatenavnFraBostedsadresse", null, "2040", null, null, "NO"),
            null, null, null, null, null, false
        )
    }

    private fun lagKontaktadresser(): Collection<Kontaktadresse> {
        return setOf(
            Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadressePDL"),
                null,
                null,
                null,
                null,
                Master.PDL.name,
                null,
                LocalDateTime.MAX,
                false
            ),
            Kontaktadresse(
                lagStrukturertAdresse("gammelGatenavnKontaktadressePDL"),
                null,
                null,
                null,
                null,
                Master.PDL.name,
                null,
                LocalDateTime.MIN,
                false
            ),
            Kontaktadresse(
                lagStrukturertAdresse("gatenavnKontaktadresseFreg"),
                null,
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MAX,
                false
            )
        )
    }

    private fun lagUgyldigeKontaktadresser(): Collection<Kontaktadresse> {
        return setOf(
            Kontaktadresse(
                lagStrukturertAdresse("gammelGatenavnKontaktadressePDL", null),
                null,
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MIN,
                true
            ),
            Kontaktadresse(
                null,
                lagSemistukturertAdresse(null),
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MAX,
                false
            )
        )
    }

    private fun lagKontaktadresseFraFreg(): Collection<Kontaktadresse> {
        return setOf(
            Kontaktadresse(
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
        )
    }

    private fun lagOppholdsadresser(): Collection<Oppholdsadresse> {
        return setOf(
            Oppholdsadresse(
                lagStrukturertAdresse("gammelGatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MIN,
                false
            ),
            Oppholdsadresse(
                lagStrukturertAdresse("gatenavnOppholdsadresseFreg"),
                null,
                null,
                null,
                Master.FREG.name,
                null,
                LocalDateTime.MAX,
                false
            )
        )
    }

    private fun lagStrukturertAdresse(gatenavn: String, landkode: String? = "NO"): StrukturertAdresse {
        return StrukturertAdresse(gatenavn, null, "1234", null, null, landkode)
    }

    private fun lagSemistukturertAdresse(landkode: String? = "NO"): SemistrukturertAdresse {
        return SemistrukturertAdresse(null, null, null, null, "4321", "UKJENT", landkode)
    }
}
