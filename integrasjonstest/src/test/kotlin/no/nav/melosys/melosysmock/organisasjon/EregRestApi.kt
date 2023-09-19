package no.nav.melosys.melosysmock.organisasjon

import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("ereg/v2")
@Unprotected
class EregRestApi {
    @GetMapping("/organisasjon/{orgnummer}")
    fun hentOrganisasjon( @PathVariable orgnummer: String): OrganisasjonResponse.Organisasjon {
        val organisasjonModell = OrganisasjonRepo.repo[orgnummer]
            ?: throw IkkeFunnetException("Ingen organisasjon med orgnr $orgnummer")

        val fomLdt = LocalDateTime.now().minusYears(1)
        val fomLd = LocalDate.now().minusYears(1)
        val bruksperiode = OrganisasjonResponse.Bruksperiode(fomLdt)
        val gyldighetsperiode = OrganisasjonResponse.Gyldighetsperiode(fomLd)

        return OrganisasjonResponse.JuridiskEnhet(
            organisasjonsnummer = orgnummer,
            juridiskEnhetDetaljer = OrganisasjonResponse.JuridiskEnhetDetaljer(sektorkode = "2100", enhetstype = "AS"),
            organisasjonDetaljer = OrganisasjonResponse.OrganisasjonDetaljer(
                registreringsdato = fomLdt,
                navn = listOf(
                    OrganisasjonResponse.Navn(
                        bruksperiode = bruksperiode, gyldighetsperiode = gyldighetsperiode,
                        sammensattnavn = organisasjonModell.navn,
                        navnelinje1 = organisasjonModell.navn,
                    )
                ),
                enhetstyper = listOf(OrganisasjonResponse.Enhetstype(bruksperiode, gyldighetsperiode, "AS")),
                forretningsadresser = listOf(
                    OrganisasjonResponse.Adresse(
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
                    OrganisasjonResponse.Adresse(
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
                    OrganisasjonResponse.Telefonnummer(
                        bruksperiode, gyldighetsperiode, "+47 12 34 56 78",
                        "ARBT"
                    )
                ),
                navSpesifikkInformasjon = OrganisasjonResponse.NAVSpesifikkInformasjon(bruksperiode, gyldighetsperiode, false),
                naeringer = listOf(
                    OrganisasjonResponse.Naering(
                        bruksperiode, gyldighetsperiode, "81.210"
                    )
                )
            )
        )
    }
}
