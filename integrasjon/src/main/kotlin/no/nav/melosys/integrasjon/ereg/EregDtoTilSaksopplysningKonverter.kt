package no.nav.melosys.integrasjon.ereg

import mu.KotlinLogging
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.Organisasjonsnavn
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse

private val log = KotlinLogging.logger { }

class EregDtoTilSaksopplysningKonverter {
    fun lagSaksopplysning(organisasjon: OrganisasjonResponse.Organisasjon): Saksopplysning = Saksopplysning().apply {
        val responseOrganisasjonDetaljer = organisasjon.organisasjonDetaljer ?: throw TekniskException("organisasjonDetaljer er null")
        dokument = OrganisasjonDokument(
            orgnummer = organisasjon.organisasjonsnummer,
            navn = finnNavn(organisasjon),
            sektorkode = finSektorkode(organisasjon),
            enhetstype = finnEnhetstype(organisasjon),
            organisasjonDetaljer = OrganisasjonsDetaljer(
                orgnummer = organisasjon.organisasjonsnummer,
                navn = tilNavn(responseOrganisasjonDetaljer.navn),
                forretningsadresse = tilGeografiskAdresse(responseOrganisasjonDetaljer.forretningsadresser),
                postadresse = tilGeografiskAdresse(responseOrganisasjonDetaljer.postadresser),
                telefon = tilTelefon(responseOrganisasjonDetaljer.telefonnummer),
                epostadresse = tilEpost(responseOrganisasjonDetaljer.epostadresser),
                naering = responseOrganisasjonDetaljer.naeringer?.map { it.naeringskode } ?: emptyList(),
                opphoersdato = responseOrganisasjonDetaljer.opphoersdato
            )
        )
    }

    private fun finnNavn(organisasjon: OrganisasjonResponse.Organisasjon): String {
        fun sammensattNavnFraDetaljer(): String? {
            // Vil få litt oversikt om dette skjer - fjernes senere
            log.warn("Fant ikke sammensattnavn i organisasjon, prøver detaljer")
            return organisasjon.organisasjonDetaljer?.navn?.firstOrNull { it.sammensattnavn != null }?.sammensattnavn
        }

        fun navnFraNavnelinjeer(): String? {
            // Vil få litt oversikt om dette skjer - fjernes senere
            log.warn("Fant ikke sammensattnavn i organisasjonDetaljer, prøver navnelinjer.")
            return organisasjon.organisasjonDetaljer?.navn?.map {
                listOfNotNull(it.navnelinje1, it.navnelinje2, it.navnelinje3, it.navnelinje4, it.navnelinje5).joinToString(" ")
            }?.firstOrNull() { it.isNotEmpty() }
        }

        fun brukUkjentNanv(): String {
            // Vil få litt oversikt om dette skjer - fjernes senere
            log.warn("Fant ikke navn for organisasjon. Bruker UKJENT")
            return "UKJENT"
        }

        return organisasjon.navn?.sammensattnavn ?: sammensattNavnFraDetaljer() ?: navnFraNavnelinjeer() ?: brukUkjentNanv()
    }

    private fun finnEnhetstype(organisasjon: OrganisasjonResponse.Organisasjon): String? {
        return when (organisasjon) {
            is OrganisasjonResponse.JuridiskEnhet -> organisasjon.juridiskEnhetDetaljer?.enhetstype
            is OrganisasjonResponse.Organisasjonsledd -> organisasjon.organisasjonsleddDetaljer?.enhetstype
            is OrganisasjonResponse.Virksomhet -> organisasjon.virksomhetDetaljer?.enhetstype
            else -> null
        } ?: organisasjon.organisasjonDetaljer?.enhetstyper?.first { it.enhetstype != null }?.enhetstype
    }

    private fun finSektorkode(organisasjon: OrganisasjonResponse.Organisasjon): String {
        return when (organisasjon) {
            is OrganisasjonResponse.JuridiskEnhet -> organisasjon.juridiskEnhetDetaljer?.sektorkode
            is OrganisasjonResponse.Organisasjonsledd -> organisasjon.organisasjonsleddDetaljer?.sektorkode
            is OrganisasjonResponse.Virksomhet -> finnSektorkode(organisasjon)
            else -> null
        } ?: ""
    }

    private fun finnSektorkode(organisasjon: OrganisasjonResponse.Virksomhet) =
        organisasjon.bestaarAvOrganisasjonsledd?.first {
            it.organisasjonsledd?.organisasjonsleddDetaljer?.sektorkode != null
        }?.organisasjonsledd?.organisasjonsleddDetaljer?.sektorkode

    private fun tilNavn(navn: List<OrganisasjonResponse.Navn>?): List<Organisasjonsnavn> = navn?.map {
        Organisasjonsnavn().apply {
            bruksperiode = it.bruksperiode.tilPeriode()
            gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
            this.navn = listOf(it.sammensattnavn)
            redigertNavn = it.sammensattnavn
        }
    } ?: emptyList()

    private fun tilTelefon(telefonnummer: List<OrganisasjonResponse.Telefonnummer>?): List<Telefonnummer> = telefonnummer?.map {
        Telefonnummer().apply {
            identifikator = it.nummer
            type = it.telefontype
            retningsnummer = null // fjerns når vi ikke lengre bruker gammelt soap api - MELOSYS-6134
            bruksperiode = it.bruksperiode.tilPeriode()
            gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
        }
    } ?: emptyList()

    private fun tilEpost(epost: List<OrganisasjonResponse.Epostadresse>?): List<Epost> = epost?.map {
        Epost().apply {
            identifikator = it.adresse
            bruksperiode = it.bruksperiode.tilPeriode()
            gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
        }
    } ?: emptyList()

    private fun tilGeografiskAdresse(adresser: List<OrganisasjonResponse.Adresse>?): List<SemistrukturertAdresse> = adresser?.map {
        SemistrukturertAdresse().apply {
            bruksperiode = it.bruksperiode.tilPeriode()
            gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
            landkode = it.landkode
            adresselinje1 = it.adresselinje1
            adresselinje2 = it.adresselinje2
            adresselinje3 = it.adresselinje3
            postnr = it.postnummer
            poststed = it.poststed
            kommunenr = it.kommunenummer
            landkode = it.landkode
        }
    } ?: emptyList()

    private fun OrganisasjonResponse.Gyldighetsperiode.tilPeriode(): Periode = Periode(fom, tom)

    private fun OrganisasjonResponse.Bruksperiode.tilPeriode(): Periode = Periode(fom.toLocalDate(), tom?.toLocalDate())
}
