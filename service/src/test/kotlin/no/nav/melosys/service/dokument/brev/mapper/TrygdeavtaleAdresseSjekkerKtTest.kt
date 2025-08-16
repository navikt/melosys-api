package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.person.Foedsel
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.domain.person.adresse.PersonAdresse
import no.nav.melosys.service.dokument.DokgenTestData.LOVVALGSPERIODE_FOM
import no.nav.melosys.service.dokument.DokgenTestData.LOVVALGSPERIODE_TOM
import no.nav.melosys.service.dokument.brev.mapper.TrygdeavtaleAdresseSjekker.UKJENT
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrygdeavtaleAdresseSjekkerKtTest {

    @ParameterizedTest(name = "{6}")
    @MethodSource("sjekkAdresser")
    fun `map bruker norsk bostedsadresse`(
        landkodeBosted: Land_iso2?,
        landkodeOpphold: Land_iso2?,
        landkodeKontakt: Land_iso2?,
        norskAddresse: List<String>,
        trygdeavtaleAddresse: List<String>,
        soknadsland: Land_iso2,
        grunn: String
    ) {
        val persondata = lagPersonopplysninger(
            landkodeBosted, landkodeOpphold, landkodeKontakt,
            Optional.empty(), Optional.empty(), Optional.empty()
        )
        val trygdeavtaleAdresseSjekker = TrygdeavtaleAdresseSjekker(persondata)


        val gyldigNorskAdresse = trygdeavtaleAdresseSjekker.finnGyldigNorskAdresse(soknadsland)
        val gyldigTrygdeavtaleAdresse = trygdeavtaleAdresseSjekker.finnGyldigTrygdeavtaleAdresse(LOVVALGSPERIODE, soknadsland)


        gyldigNorskAdresse shouldBe norskAddresse
        gyldigTrygdeavtaleAdresse shouldBe trygdeavtaleAddresse
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("gyldigePerioder")
    fun `sjekk om adresse gyldighet er innenfor lovvalgsperiode for gyldige perioder`(
        lovvalgsperiode: Lovvalgsperiode,
        personAdresse: PersonAdresse,
        grunn: String
    ) {
        val result = TrygdeavtaleAdresseSjekker.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode)


        result shouldBe true
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("ugyldigePerioder")
    fun `sjekk om adresse gyldighet er innenfor lovvalgsperiode for ugyldige perioder`(
        lovvalgsperiode: Lovvalgsperiode,
        personAdresse: PersonAdresse,
        grunn: String
    ) {
        val result = TrygdeavtaleAdresseSjekker.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode)


        result shouldBe false
    }

    @Test
    fun `finn gyldig norsk adresse bruker har semistrukturert adresse mappes korrekt`() {
        val semistrukturertAdresse = SemistrukturertAdresse(
            "adresselinje 1", "adresselinje 2", null, null,
            "postnr", "poststed", Land_iso2.SE.kode
        )
        val kontaktadresse = Kontaktadresse(
            null, semistrukturertAdresse, null, null,
            null, "PDL", null, null, false
        )
        val persondata = Personopplysninger(
            emptyList(), null, null, null,
            Foedsel(LocalDate.EPOCH, null, null, null), null, null,
            listOf(kontaktadresse), Navn("Ole", "", "Norman"), emptyList(), emptyList()
        )
        val storbritanniaAdresseSjekker = TrygdeavtaleAdresseSjekker(persondata)


        val gyldigNorskAdresse = storbritanniaAdresseSjekker.finnGyldigNorskAdresse(Land_iso2.GB)


        gyldigNorskAdresse shouldContainExactly listOf(
            "Resident outside of Norway", "adresselinje 1 adresselinje 2", "postnr", "poststed", "Sverige"
        )
    }

    private val LOVVALGSPERIODE = TrygdeavtaleMapperTest.lagLovvalgsperiode()

    fun sjekkAdresser(): List<Arguments> = listOf(
        Arguments.of(
            Land_iso2.NO, Land_iso2.NO, Land_iso2.NO,
            listOf("bosted 42 C", "Norge"),
            listOf(UKJENT), Land_iso2.GB,
            "Velg bosted, når alle adresser er norske"
        ),
        Arguments.of(
            Land_iso2.SE, Land_iso2.SE, Land_iso2.NO,
            listOf("kontakt 1", "Norge"),
            listOf(UKJENT), Land_iso2.GB,
            "Kun kontakt med norsk adresse"
        ),
        Arguments.of(
            Land_iso2.SE, Land_iso2.NO, Land_iso2.SE,
            listOf("tilleggOpphold", "opphold 1", "Norge"),
            listOf(UKJENT), Land_iso2.GB,
            "Kun opphold med norsk adresse"
        ),
        Arguments.of(
            Land_iso2.NO, Land_iso2.SE, Land_iso2.SE,
            listOf("bosted 42 C", "Norge"),
            listOf(UKJENT),
            Land_iso2.US,
            "Kun bosted med norsk adresse"
        ),
        Arguments.of(
            Land_iso2.GB, Land_iso2.SE, Land_iso2.SE,
            listOf("No address in Norway"),
            listOf("bosted 42 C", "Storbritannia"),
            Land_iso2.GB,
            "Ingen norske adresser, men adresse i UK"
        ),
        Arguments.of(
            Land_iso2.GB, null, null,
            listOf("No address in Norway"),
            listOf("bosted 42 C", "Storbritannia"),
            Land_iso2.GB,
            "kun adresse i UK"
        ),
        Arguments.of(
            Land_iso2.US, null, null,
            listOf("No address in Norway"),
            listOf("bosted 42 C", "USA"),
            Land_iso2.US,
            "kun adresse i US"
        ),
        Arguments.of(
            Land_iso2.SE, Land_iso2.SE, Land_iso2.SE,
            listOf("Resident outside of Norway", "bosted 42 C", "Sverige"),
            listOf(UKJENT), Land_iso2.GB,
            "Utenlandsk adresse, men ikke i UK"
        ),
        Arguments.of(
            null, null, null,
            listOf(UKJENT),
            listOf(UKJENT),
            Land_iso2.US,
            "ingen adresser"
        )
    )

    fun gyldigePerioder(): List<Arguments> = listOf(
        Arguments.of(
            lagLovvalgsperiode(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            lagPersonAdresse(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            "lovalgsperiode er lik adresseperiode"
        ),
        Arguments.of(
            lagLovvalgsperiode(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            lagPersonAdresse(
                LocalDate.of(2020, 2, 1),
                LocalDate.of(2020, 3, 1)
            ),
            "lovalgsperiode har start før og slutt etter adresseperiode"
        ),
        Arguments.of(
            lagLovvalgsperiode(
                LocalDate.of(2020, 2, 1),
                LocalDate.of(2020, 3, 1)
            ),
            lagPersonAdresse(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            "adresseperiode har start før og slutt etter lovalgsperiode"
        ),
        Arguments.of(
            lagLovvalgsperiode(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2022, 1, 1)
            ),
            lagPersonAdresse(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            "lovalgsperiode start er lik adresseperiode slutt"
        ),
        Arguments.of(
            lagLovvalgsperiode(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            lagPersonAdresse(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2022, 1, 1)
            ),
            "lovalgsperiode slutt er lik adresseperiode start"
        ),
        Arguments.of(
            lagLovvalgsperiode(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1)
            ),
            lagPersonAdresse(
                LocalDate.of(2021, 1, 1),
                null
            ),
            "personadresse gyldigTom er null"
        )
    )

    fun ugyldigePerioder(): List<Arguments> = listOf(
        Arguments.of(
            lagLovvalgsperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1)),
            lagPersonAdresse(LocalDate.of(2020, 2, 1), LocalDate.of(2021, 1, 1)),
            "lovalgsperiode er før adresseperiode"
        ),
        Arguments.of(
            lagLovvalgsperiode(LocalDate.of(2021, 1, 2), LocalDate.of(2022, 1, 1)),
            lagPersonAdresse(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)),
            "lovalgsperiode er etter adresseperiode"
        ),
        Arguments.of(
            lagLovvalgsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)),
            lagPersonAdresse(null, null),
            "adresseperiode fom og tom er null"
        )
    )

    fun lagPersonopplysninger(
        landkodeBosted: Land_iso2?,
        landkodeOpphold: Land_iso2?,
        landkodeKontakt: Land_iso2?,
        landkodeBostedValgfritt: Optional<String>,
        landkodeOppholdValgfritt: Optional<String>,
        landkodeKontaktValgfritt: Optional<String>
    ): Persondata {
        val gyldigFom = LOVVALGSPERIODE_FOM
        val gyldigTom = LOVVALGSPERIODE_TOM

        val kunLandkodeBosted = landkodeBostedValgfritt.orElse(kodeEllerNull(landkodeBosted))
        val kunLandkodeOpphold = landkodeOppholdValgfritt.orElse(kodeEllerNull(landkodeOpphold))
        val kunLandkodeKontakt = landkodeKontaktValgfritt.orElse(kodeEllerNull(landkodeKontakt))

        val bostedsadresse = Bostedsadresse(
            StrukturertAdresse("bosted", "42 C", null, null, null, kunLandkodeBosted),
            null, gyldigFom, gyldigTom, "PDL", null, false
        )

        val kontaktadresse = Kontaktadresse(
            StrukturertAdresse("kontakt 1", null, null, null, null, kunLandkodeKontakt),
            null, null, gyldigFom, gyldigTom, "PDL", null, null,
            false
        )

        val oppholdsadresse = Oppholdsadresse(
            StrukturertAdresse(
                "tilleggOpphold", "opphold 1", null, null, null,
                null, null, kunLandkodeOpphold
            ), null,
            LOVVALGSPERIODE_FOM, LOVVALGSPERIODE_TOM,
            "PDL", null, null, false
        )

        return Personopplysninger(
            emptyList(), bostedsadresse, null, null,
            Foedsel(LocalDate.EPOCH, null, null, null), null, null,
            listOf(kontaktadresse), Navn("Ole", "", "Norman"), listOf(oppholdsadresse), emptyList()
        )
    }

    private fun kodeEllerNull(landkoder: Land_iso2?): String? = landkoder?.kode

    private fun lagPersonAdresse(gyldigFom: LocalDate?, gyldigTom: LocalDate?): PersonAdresse =
        Oppholdsadresse(null, null, gyldigFom, gyldigTom, null, null, null, false)

    private fun lagLovvalgsperiode(lovFom: LocalDate, lovTom: LocalDate): Lovvalgsperiode =
        Lovvalgsperiode().apply {
            fom = lovFom
            tom = lovTom
        }
}
