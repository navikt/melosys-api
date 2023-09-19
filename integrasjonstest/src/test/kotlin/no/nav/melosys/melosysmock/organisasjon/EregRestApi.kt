package no.nav.melosys.melosysmock.organisasjon

import no.nav.melosys.exception.IkkeFunnetException
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse as OR

@RestController
@RequestMapping("ereg/v2")
@Unprotected
class EregRestApi {
    @GetMapping("/organisasjon/{orgnummer}")
    fun hentOrganisasjon( @PathVariable orgnummer: String): OR.Organisasjon {
        val organisasjonModell = OrganisasjonRepo.repo[orgnummer]
            ?: throw IkkeFunnetException("Ingen organisasjon med orgnr $orgnummer")

        val fomLdt = LocalDateTime.now().minusYears(1)
        val fomLd = LocalDate.now().minusYears(1)
        val bruksperiode = OR.Bruksperiode(fomLdt)
        val gyldighetsperiode = OR.Gyldighetsperiode(fomLd)

        return OR.JuridiskEnhet(
            organisasjonsnummer = orgnummer,
            juridiskEnhetDetaljer = OR.JuridiskEnhetDetaljer(sektorkode = "2100", enhetstype = "AS"),
            organisasjonDetaljer = OR.OrganisasjonDetaljer(
                registreringsdato = fomLdt,
                navn = listOf(
                    OR.Navn(
                        bruksperiode = bruksperiode, gyldighetsperiode = gyldighetsperiode,
                        sammensattnavn = organisasjonModell.navn,
                        navnelinje1 = organisasjonModell.navn,
                    )
                ),
                enhetstyper = listOf(OR.Enhetstype(bruksperiode, gyldighetsperiode, "AS")),
                forretningsadresser = listOf(
                    OR.Adresse(
                        bruksperiode = bruksperiode,
                        gyldighetsperiode = gyldighetsperiode,
                        adresselinje1 = organisasjonModell.forretningsadresse.adresselinje1,
                        poststed = organisasjonModell.forretningsadresse.poststed,
                        postnummer = organisasjonModell.forretningsadresse.postnummer,
                        kommunenummer = organisasjonModell.forretningsadresse.kommunenr,
                        landkode = organisasjonModell.forretningsadresse.landkode
                    )
                ),
                postadresser = listOf(
                    OR.Adresse(
                        bruksperiode = bruksperiode,
                        gyldighetsperiode = gyldighetsperiode,
                        adresselinje1 = organisasjonModell.forretningsadresse.adresselinje1,
                        poststed = organisasjonModell.forretningsadresse.poststed,
                        postnummer = organisasjonModell.forretningsadresse.postnummer,
                        kommunenummer = organisasjonModell.forretningsadresse.kommunenr,
                        landkode = organisasjonModell.forretningsadresse.landkode
                    )
                ),
                telefonnummer = listOf(
                    OR.Telefonnummer(
                        bruksperiode, gyldighetsperiode, "+47 12 34 56 78",
                        "ARBT"
                    )
                ),
                navSpesifikkInformasjon = OR.NAVSpesifikkInformasjon(bruksperiode, gyldighetsperiode, false),
                naeringer = listOf(
                    OR.Naering(
                        bruksperiode, gyldighetsperiode, "81.210"
                    )
                )
            )
        )
    }
}
