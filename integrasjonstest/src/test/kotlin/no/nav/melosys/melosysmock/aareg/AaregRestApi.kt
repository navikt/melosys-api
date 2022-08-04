package no.nav.melosys.melosysmock.aareg

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.melosysmock.organisasjon.OrganisasjonRepo
import no.nav.melosys.melosysmock.person.PersonRepoStorage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/aareg-services/api/v1/arbeidstaker/arbeidsforhold")
class AaregRestApi(private val personRepoStorage: PersonRepoStorage) {

    @GetMapping
    fun finnArbeidsforholdPrArbeidstaker(
            @RequestParam("regelverk", required = false) regelverk: String? = null,
    )
            : List<Arbeidsforhold> {
        val organisasjon = OrganisasjonRepo.repo.values.first()
        val person = personRepoStorage.repo.values.first() // TODO: get this from header
        val ansettelsesperiodeFom = LocalDate.now().minusYears(5).toString()

        return mutableListOf(Arbeidsforhold().apply {
            arbeidsforholdId = "123"
            navArbeidsforholdId = 123
            opplysningspliktig = Opplysningspliktig().apply {
                type = "Organisasjon"
                organisasjonsnummer = organisasjon.orgnr
            }
            ansettelsesperiode = Ansettelsesperiode().apply {
                periode = Periode().apply {
                    fom = ansettelsesperiodeFom
                }
            }
            type = "ordinaertArbeidsforhold"
            arbeidsgiver = Arbeidsgiver().apply {
                organisasjonsnummer = organisasjon.orgnr
                type = "Organisasjon"
            }
            arbeidstaker = Arbeidstaker().apply {
                offentligIdent = person.ident
            }
            arbeidsavtaler.add(
                    Arbeidsavtale().apply {
                        type = "Ordinaer"
                        ansettelsesform = "fast"
                        gyldighetsperiode = Periode().apply {
                            fom = ansettelsesperiodeFom
                        }
                        arbeidstidsordning = "ikkeSkift" // Ikke skift
                        yrke = "0013008" // "Konsulent"
                        antallTimerPrUke = BigDecimal("37.5")
                        stillingsprosent = BigDecimal("100")
                        sistLoennsendring = ansettelsesperiodeFom
                        beregnetAntallTimerPrUke = BigDecimal("37.5")
                        stillingsprosent = BigDecimal("100")
                        sistStillingsendring = ansettelsesperiodeFom
                    }
            )
        })
    }

    class Arbeidsforhold(
            var arbeidsforholdId: String? = null,
            var navArbeidsforholdId: Int? = null,
            var ansettelsesperiode: Ansettelsesperiode? = null,
            var type: String? = null,
            var arbeidstaker: Arbeidstaker? = null,
            val arbeidsavtaler: MutableList<Arbeidsavtale> = mutableListOf(),
            @JsonInclude(JsonInclude.Include.NON_NULL)
            var permisjonPermitteringer: List<PermisjonPermitteringer>? = null,
            var utenlandsopphold: List<Utenlandsopphold>? = null,
            var arbeidsgiver: Arbeidsgiver? = null,
            var opplysningspliktig: Opplysningspliktig? = null,
            var innrapportertEtterAOrdningen: Boolean? = null,
            var registrert: String? = null,
            var sistBekreftet: String? = null,
            var antallTimerForTimeloennet: List<AntallTimerForTimeloennet>? = null
    )

    class AntallTimerForTimeloennet(
            var antallTimer: BigDecimal? = null,
            var periode: Periode? = null,
            var rapporteringsperiode: String? = null
    )

    class Opplysningspliktig(
            var type: String? = null,
            var organisasjonsnummer: String? = null
    )

    class Arbeidsgiver(
            var type: String? = null,
            var organisasjonsnummer: String? = null
    )

    class Utenlandsopphold(
            var landkode: String? = null,
            var periode: Periode? = null,
            var rapporteringsperiode: String? = null
    )

    class Ansettelsesperiode(
            var periode: Periode? = null
    )

    class Arbeidstaker(
            var type: String? = null,
            var offentligIdent: String? = null,
            var aktoerId: String? = null
    )

    class Periode(
            var fom: String? = null,
            var tom: String? = null
    )

    class PermisjonPermitteringer(
            var periode: Periode? = null,
            var permisjonPermitteringId: String? = null,
            var prosent: BigDecimal? = null,
            var type: String? = null,
            var varslingskode: String? = null
    )

    class Arbeidsavtale(
            var type: String? = null,
            var arbeidstidsordning: String? = null,
            var yrke: String? = null,
            var ansettelsesform: String? = null,
            var stillingsprosent: BigDecimal? = null,
            var beregnetAntallTimerPrUke: BigDecimal? = null,
            var gyldighetsperiode: Periode? = null,
            var sistStillingsendring: String? = null,
            var sistLoennsendring: String? = null,
            var antallTimerPrUke: BigDecimal? = null
    )
}

