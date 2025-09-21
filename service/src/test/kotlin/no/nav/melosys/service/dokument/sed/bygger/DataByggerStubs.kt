package no.nav.melosys.service.dokument.sed.bygger

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.person.Familiemedlem
import no.nav.melosys.domain.dokument.person.Familierelasjon
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigArbeid
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import java.time.LocalDate

// For gjenbruk av AbstraktSedDatabygger implementasjonen i nåværende og fremtidige tester
object DataByggerStubs {

    fun hentBehandlingStub(): Behandling {
        val mottatteOpplysninger = MottatteOpplysninger()

        val fagsak = FagsakTestFactory.lagFagsak()
        val myndighet = Aktoer().apply {
            rolle = Aktoersroller.TRYGDEMYNDIGHET
            institusjonID = "SE:123321"
        }
        fagsak.leggTilAktør(myndighet)

        val saksopplysninger = mutableSetOf<Saksopplysning>()

        val foretakUtland = ForetakUtland().apply {
            adresse = hentStrukturertAddresseStub()
            navn = "navn foretak"
            uuid = "uuid"
        }

        val søknadDokument = Soeknad().apply {
            selvstendigArbeid = SelvstendigArbeid()
            this.foretakUtland = mutableListOf(foretakUtland)

            val selvstendigForetak = SelvstendigForetak().apply {
                orgnr = "12312312"
            }
            selvstendigArbeid.selvstendigForetak = listOf(selvstendigForetak)
            selvstendigArbeid.erSelvstendig = true

            val fysiskArbeidssted = FysiskArbeidssted("foretaknavn", hentStrukturertAddresseStub())
            arbeidPaaLand.fysiskeArbeidssteder = mutableListOf(fysiskArbeidssted)

            val utenlandskIdent = UtenlandskIdent().apply {
                ident = "439205843"
                landkode = "SE"
            }
            personOpplysninger.utenlandskIdent = listOf(utenlandskIdent)

            // Set up periode for test scenarios that expect søknadsperiode
            periode = no.nav.melosys.domain.mottatteopplysninger.data.Periode(
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusYears(1)
            )
        }
        mottatteOpplysninger.mottatteOpplysningerData = søknadDokument

        var saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.ARBFORH
            dokument = ArbeidsforholdDokument()
        }
        saksopplysninger.add(saksopplysning)

        søknadDokument.maritimtArbeid = listOf(MaritimtArbeid().apply {
            enhetNavn = "enhet"
        })

        val personDokument = lagPersonDokument()

        saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
            dokument = personDokument
        }
        saksopplysninger.add(saksopplysning)

        return BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(mottatteOpplysninger)
            .medSaksopplysninger(saksopplysninger)
            .build()
    }

    fun lagPersonDokument(): PersonDokument = PersonDokument().apply {
        setErEgenAnsatt(true)
        fødselsdato = LocalDate.now()

        bostedsadresse = Bostedsadresse(
            gateadresse = Gateadresse(),
            land = Land(Land.NORGE),
            poststed = "1212"
        )

        familiemedlemmer = listOf(Familiemedlem().apply {
            navn = "farnavn"
            fnr = "111111111"
            familierelasjon = Familierelasjon.FARA
        })

        kjønn = KjoennsType("M")
        fornavn = "Mrfornavn"
        etternavn = "Spock"
        statsborgerskap = Land(Land.NORGE)
    }

    fun hentBehandlingMedManglendeAdressefelterStub(
        fysiskArbeidsstedManglerLandkode: Boolean,
        arbeidsgivendeForetakUtlandManglerLandkode: Boolean,
        selvstendigForetakUtlandManglerLandkode: Boolean
    ): Behandling {
        val behandling = hentBehandlingStub()
        val mottatteOpplysningerData = behandling.mottatteOpplysninger!!.mottatteOpplysningerData

        val fysiskeArbeidssteder = mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder as MutableList<FysiskArbeidssted>
        val fysiskArbeidssted = fysiskeArbeidssteder.removeAt(0)
        fysiskArbeidssted.adresse.poststed = null
        if (fysiskArbeidsstedManglerLandkode) {
            fysiskArbeidssted.adresse.landkode = null
        }
        fysiskeArbeidssteder.add(fysiskArbeidssted)

        val foretakUtlandList = mottatteOpplysningerData.foretakUtland as MutableList<ForetakUtland>
        val foretakUtland = foretakUtlandList.removeAt(0)
        foretakUtland.adresse.postnummer = null
        foretakUtland.adresse.poststed = null
        if (arbeidsgivendeForetakUtlandManglerLandkode || selvstendigForetakUtlandManglerLandkode) {
            foretakUtland.adresse.landkode = null
        }
        foretakUtland.selvstendigNæringsvirksomhet = selvstendigForetakUtlandManglerLandkode
        foretakUtlandList.add(foretakUtland)

        return behandling
    }

    private fun hentStrukturertAddresseStub() = StrukturertAdresse(
        husnummerEtasjeLeilighet = "25",
        gatenavn = "Gatenavn",
        postnummer = "0165",
        region = "Region",
        landkode = Landkoder.NO.kode
    )

    fun hentOrganisasjonDokumentSetStub(): Set<OrganisasjonDokument> {
        val orgDokumentHashSet = hashSetOf<OrganisasjonDokument>()
        val organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .organisasjonsDetaljer(mockk<OrganisasjonsDetaljer>(relaxed = true))
            .orgnummer("orgnr")
            .build()

        every { organisasjonDokument.organisasjonDetaljer.hentStrukturertForretningsadresse() } returns hentStrukturertAddresseStub()
        orgDokumentHashSet.add(organisasjonDokument)

        return orgDokumentHashSet
    }
}
