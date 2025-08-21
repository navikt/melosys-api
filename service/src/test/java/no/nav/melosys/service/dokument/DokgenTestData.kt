package no.nav.melosys.service.dokument

import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet
import no.nav.melosys.domain.person.*
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import java.time.Instant
import java.time.LocalDate

object DokgenTestData {
    const val FNR_BRUKER = "05058892382"
    const val ORGNR = "999999999"
    const val SAMMENSATT_NAVN_BRUKER = "Donald Duck"
    const val ADRESSELINJE_1_BRUKER = "Andebygata 1"
    const val POSTNR_BRUKER = "9999"
    const val POSTSTED_BRUKER = "Andeby"
    const val SAKSNUMMER = "MEL-123"
    const val KONTAKT_NAVN = "Fetter Anton"
    const val NAVN_ORG = "Advokatene AS"
    const val POSTBOKS_ORG = "POSTBOKS 200"
    const val POSTNR_ORG = "9990"
    const val REGION = "NEVERLAND"
    val LOVVALGSPERIODE_FOM: LocalDate = LocalDate.of(2020, 1, 1)
    val LOVVALGSPERIODE_TOM: LocalDate = LocalDate.of(2021, 1, 1)
    const val FNR_FULLMEKTIG = "30098000492"
    const val ORGNR_FULLMEKTIG = "810072512"

    fun lagBehandling(): Behandling = lagBehandling(lagFagsak())

    fun lagBehandling(fagsak: Fagsak): Behandling =
        BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            .medMottatteOpplysninger(lagMottatteOpplysninger())
            .build()

    fun lagFagsak(medFullmektig: Boolean = false): Fagsak =
        FagsakTestFactory.builder()
            .saksnummer(SAKSNUMMER)
            .behandlinger(lagBehandlinger())
            .type(Sakstyper.FTRL)
            .tema(Sakstemaer.UNNTAK)
            .medBruker()
            .build().apply {
                registrertDato = Instant.now()
                endretAv = "L12345"
                if (medFullmektig) {
                    val fullmektig = Aktoer().apply {
                        rolle = Aktoersroller.FULLMEKTIG
                        setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
                    }
                    leggTilAktør(fullmektig)
                }
            }

    fun lagPersondata(fødselsdato: LocalDate? = null): Persondata {
        val bostedsadresse = Bostedsadresse(
            StrukturertAdresse(ADRESSELINJE_1_BRUKER, "42 C", POSTNR_BRUKER, null, null, Landkoder.NO.kode),
            null, null, null, "PDL", null, false
        )
        val kontaktadresse = Kontaktadresse(
            StrukturertAdresse(ADRESSELINJE_1_BRUKER, null, POSTNR_BRUKER, POSTSTED_BRUKER, null, "NO"),
            null, null, null, null, "PDL", null, null, false
        )
        return Personopplysninger(
            emptyList(), bostedsadresse, null, null,
            Foedsel(fødselsdato, null, null, null),
            Folkeregisteridentifikator(FNR_BRUKER), null,
            // For å få testene til å funke som med brukt med PersonDokument må fornavn og etternavn bytte plass.
            // Dette er nå en "feil" i prod og blir en egen oppgave å fikse.
            listOf(kontaktadresse), Navn("Duck", null, "Donald"), emptyList(), emptyList()
        )
    }

    fun lagAdresse() = UstrukturertAdresse().apply {
        adresselinje1 = ADRESSELINJE_1_BRUKER
        postnr = POSTNR_BRUKER
        poststed = POSTSTED_BRUKER
        land = Land(Land.NORGE)
    }

    fun lagKontaktOpplysning() = Kontaktopplysning().apply {
        kontaktNavn = KONTAKT_NAVN
    }

    fun lagOrg(): OrganisasjonDokument =
        OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn(NAVN_ORG)
            .organisasjonsDetaljer(lagOrgDetaljer())
            .build()

    fun lagOrg(landkoder: Landkoder): OrganisasjonDokument {
        val semistrukturertAdresse = SemistrukturertAdresse().apply {
            landkode = landkoder.kode
            gyldighetsperiode = Periode(LocalDate.now(), LocalDate.now())
            postnr = POSTNR_ORG
        }
        val organisasjonsDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(semistrukturertAdresse)
            .build()
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn(NAVN_ORG)
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()
    }

    private fun lagBehandlinger(): List<Behandling> = listOf(
        BehandlingTestFactory.builderWithDefaults()
            .medType(Behandlingstyper.FØRSTEGANG)
            .medRegistrertDato(Instant.now())
            .build()
    )

    private fun lagMottatteOpplysninger() = MottatteOpplysninger().apply {
        mottatteOpplysningerData = lagMottatteOpplysningerdata()
    }

    fun lagMottatteOpplysningerSøknadUtenforEØS() = MottatteOpplysninger().apply {
        mottatteOpplysningerData = lagMottatteOpplysningerdataSøknadUtenforEØS()
    }

    private fun lagMottatteOpplysningerdata() = MottatteOpplysningerData().apply {
        soeknadsland = Soeknadsland(listOf("AT"), false)
    }

    private fun lagMottatteOpplysningerdataSøknadUtenforEØS() = SøknadNorgeEllerUtenforEØS().apply {
        soeknadsland = Soeknadsland(listOf("AT"), false)
        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
    }

    private fun lagOrgDetaljer(): OrganisasjonsDetaljer =
        OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(lagOrgAdresse())
            .build()

    private fun lagOrgAdresse(): GeografiskAdresse = SemistrukturertAdresse().apply {
        adresselinje1 = POSTBOKS_ORG
        postnr = POSTNR_ORG
        gyldighetsperiode = Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2))
        landkode = "NO"
    }

    fun lagTrygdeavtaleBehandling(): Behandling = lagTrygdeavtaleBehandling(
        RepresentantIUtlandet.av(
            "Foretaksnavn",
            listOf("Uk address"),
            Landkoder.GB
        )
    )

    fun lagTrygdeavtaleBehandling(representantIUtlandet: RepresentantIUtlandet?): Behandling =
        lagBehandling(lagFagsak()).apply {
            val mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS().apply {
                this.representantIUtlandet = representantIUtlandet
            }
            this.mottatteOpplysninger?.mottatteOpplysningerData = mottatteOpplysningerData
        }

    fun lagLovvalgsperiode() = Lovvalgsperiode().apply {
        fom = LOVVALGSPERIODE_FOM
        tom = LOVVALGSPERIODE_TOM
        dekning = Trygdedekninger.FULL_DEKNING_FTRL
        bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
    }

    fun lagMottaker(rolle: Mottakerroller): Mottaker = Mottaker().apply {
        when (rolle) {
            Mottakerroller.BRUKER -> {
                this.rolle = Mottakerroller.BRUKER
                aktørId = FNR_BRUKER
            }

            Mottakerroller.VIRKSOMHET -> {
                this.rolle = Mottakerroller.VIRKSOMHET
                orgnr = ORGNR
            }

            Mottakerroller.ARBEIDSGIVER -> {
                this.rolle = Mottakerroller.ARBEIDSGIVER
                orgnr = ORGNR_FULLMEKTIG
            }

            else -> throw IllegalArgumentException("Støtter ikke mottakerrolle ${rolle.kode}")
        }
    }

    fun lagMottakerFullmektig(aktoertype: Aktoertype): Mottaker = Mottaker().apply {
        when (aktoertype) {
            Aktoertype.PERSON -> personIdent = FNR_FULLMEKTIG
            Aktoertype.ORGANISASJON -> orgnr = ORGNR_FULLMEKTIG
            else -> throw IllegalArgumentException("Fullmektig må være person eller organisasjon")
        }
        rolle = Mottakerroller.FULLMEKTIG
    }
}
