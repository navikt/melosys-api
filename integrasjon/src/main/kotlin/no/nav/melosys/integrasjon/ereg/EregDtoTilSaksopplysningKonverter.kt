package no.nav.melosys.integrasjon.ereg

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.Organisasjonsnavn
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonResponse

class EregDtoTilSaksopplysningKonverter {
    fun lagSaksopplysning(organisasjon: OrganisasjonResponse.Organisasjon): Saksopplysning = Saksopplysning().apply {
        dokument = OrganisasjonDokument().apply {
            orgnummer = organisasjon.organisasjonsnummer
            sektorkode = finSektorkode(organisasjon)
            oppstartsdato = null // finnes ikke, vi har registreringsdato, opphoersdato, sistEndret, stiftelsesdato
            enhetstype = organisasjon.organisasjonDetaljer?.enhetstyper?.first { it.enhetstype != null }?.enhetstype
            navn = listOf(organisasjon.navn?.sammensattnavn ?: "UKJENT")
            organisasjonDetaljer = OrganisasjonsDetaljer().apply {
                orgnummer = organisasjon.organisasjonsnummer
                navn = tilNavn(organisasjon.organisasjonDetaljer?.navn)
                forretningsadresse = tilGeografiskAdresse(organisasjon.organisasjonDetaljer?.forretningsadresser)
                postadresse = tilGeografiskAdresse(organisasjon.organisasjonDetaljer?.postadresser)
                telefon = tilTelefon(organisasjon.organisasjonDetaljer?.telefonnummer)
                epostadresse = tilEpost(organisasjon.organisasjonDetaljer?.epostadresser)
                naering = organisasjon.organisasjonDetaljer?.naeringer?.map { it.naeringskode } ?: emptyList()
                opphoersdato = organisasjon.organisasjonDetaljer?.opphoersdato
            }
        }
    }

    private fun finSektorkode(organisasjon: OrganisasjonResponse.Organisasjon): String? {
        return when (organisasjon) {
            is OrganisasjonResponse.JuridiskEnhet -> organisasjon.juridiskEnhetDetaljer?.sektorkode
            is OrganisasjonResponse.Organisasjonsledd -> organisasjon.organisasjonsleddDetaljer?.sektorkode
            is OrganisasjonResponse.Virksomhet -> finnSektorkode(organisasjon)
            else -> null
        }
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
            retningsnummer = null // skal vi finne ut retningsnummer?
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
