package no.nav.melosys.saksflyt

import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo
import java.time.LocalDate

object TestdataFactory {

    fun lagBehandling(): Behandling {
        val fagsak = lagFagsak()
        return Behandling.forTest {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            saksopplysninger = mutableSetOf(lagPersonopplysning())
            this.fagsak = fagsak
        }
    }

    fun lagBehandlingNyVurdering() = lagBehandling().apply {
        id = 3L
        type = Behandlingstyper.NY_VURDERING
    }


    fun lagOrgMedPostadresse() =
        OrganisasjonDokumentTestFactory.builder()
            .orgnummer("122344")
            .organisasjonsDetaljer(lagOrgDetaljerMedPostadresse())
            .build()


    fun lagOrgMedForretningsadresse() =
        OrganisasjonDokumentTestFactory.builder()
            .orgnummer("122344")
            .organisasjonsDetaljer(lagOrgDetaljerMedForretningsadresse())
            .build()


    fun lagKontaktOpplysning() =
        Kontaktopplysning().apply {
            kontaktNavn = "Donald Duck"
        }


    fun lagDokumentInfo() =
        DokumentproduksjonsInfo("dummy_mal", DokumentKategoriKode.IB.kode, "Dummy tittel", null)

    internal fun lagPersonopplysning() =
        Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
            dokument = PersonDokument().apply {
                fnr = "99887766554"
            }
        }


    fun lagFagsak(): Fagsak = lagFagsak("MEL-test")


    fun lagFagsak(saksnummer: String) =
        Fagsak(
            saksnummer,
            123L,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            mutableSetOf(lagBruker()),
            mutableListOf()
        )


    fun lagBruker() =
        Aktoer().apply {
            rolle = BRUKER
            aktørId = "aktørID"
        }

    internal fun lagOrgDetaljerMedPostadresse() =
        OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(lagOrgadresse("1234"))
            .build()

    internal fun lagOrgDetaljerMedForretningsadresse() =
        OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(lagOrgadresse("1234"))
            .build()

    internal fun lagOrgadresse(postnummer: String): GeografiskAdresse =
        SemistrukturertAdresse().apply {
            gyldighetsperiode = Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2))
            adresselinje1 = "Testgata 3"
            postnr = postnummer
            landkode = "NO"
        }
}
