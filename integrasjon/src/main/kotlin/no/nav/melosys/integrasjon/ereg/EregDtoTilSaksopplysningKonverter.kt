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
    fun lagSaksopplysning(organisasjon: OrganisasjonResponse.Organisasjon): Saksopplysning {
        return Saksopplysning().apply {
            dokument = OrganisasjonDokument().apply {
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
    }

    private fun tilNavn(navn: List<OrganisasjonResponse.Navn>?): List<Organisasjonsnavn> =
        navn?.map {
            Organisasjonsnavn().apply {
                bruksperiode = it.bruksperiode.tilPeriode()
                gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
                this.navn = listOf(it.sammensattnavn)
                redigertNavn = it.sammensattnavn
            }
        } ?: emptyList()

    private fun tilTelefon(telefonnummer: List<OrganisasjonResponse.Telefonnummer>?): List<Telefonnummer> {
        return telefonnummer?.map {
            Telefonnummer().apply {
                identifikator = it.nummer
                type = it.telefontype
                retningsnummer = null // skal vi finne ut retningsnummer?
                bruksperiode = it.bruksperiode.tilPeriode()
                gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
            }
        } ?: return emptyList()
    }

    private fun tilEpost(epost: List<OrganisasjonResponse.Epostadresse>?): List<Epost> {
        return epost?.map {
            Epost().apply {
                identifikator = it.adresse
                bruksperiode = it.bruksperiode.tilPeriode()
                gyldighetsperiode = it.gyldighetsperiode.tilPeriode()
            }
        } ?: return emptyList()
    }

    private fun tilGeografiskAdresse(adresser: List<OrganisasjonResponse.Adresse>?): List<SemistrukturertAdresse> {
        return adresser?.map {
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
        } ?: return emptyList()
    }

    private fun OrganisasjonResponse.Gyldighetsperiode.tilPeriode(): Periode = Periode(fom, tom)

    private fun OrganisasjonResponse.Bruksperiode.tilPeriode(): Periode = Periode(fom.toLocalDate(), tom?.toLocalDate())
}
