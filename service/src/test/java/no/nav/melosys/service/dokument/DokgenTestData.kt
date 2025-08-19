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
    @JvmField
    val FNR_BRUKER = "05058892382"

    @JvmField
    val ORGNR = "999999999"

    @JvmField
    val SAMMENSATT_NAVN_BRUKER = "Donald Duck"

    @JvmField
    val ADRESSELINJE_1_BRUKER = "Andebygata 1"

    @JvmField
    val POSTNR_BRUKER = "9999"

    @JvmField
    val POSTSTED_BRUKER = "Andeby"

    @JvmField
    val SAKSNUMMER = "MEL-123"

    @JvmField
    val KONTAKT_NAVN = "Fetter Anton"

    @JvmField
    val NAVN_ORG = "Advokatene AS"

    @JvmField
    val POSTBOKS_ORG = "POSTBOKS 200"

    @JvmField
    val POSTNR_ORG = "9990"

    @JvmField
    val REGION = "NEVERLAND"

    @JvmField
    val LOVVALGSPERIODE_FOM: LocalDate = LocalDate.of(2020, 1, 1)

    @JvmField
    val LOVVALGSPERIODE_TOM: LocalDate = LocalDate.of(2021, 1, 1)

    @JvmField
    val FNR_FULLMEKTIG = "30098000492"

    @JvmField
    val ORGNR_FULLMEKTIG = "810072512"

    @JvmStatic
    fun lagBehandling(): Behandling {
        return lagBehandling(lagFagsak())
    }

    @JvmStatic
    fun lagBehandling(fagsak: Fagsak): Behandling {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            .medMottatteOpplysninger(lagMottatteOpplysninger())
            .build()
    }

    @JvmStatic
    @JvmOverloads
    fun lagFagsak(medFullmektig: Boolean = false): Fagsak {
        val fagsak = FagsakTestFactory.builder()
            .saksnummer(SAKSNUMMER)
            .behandlinger(lagBehandlinger())
            .type(Sakstyper.FTRL)
            .tema(Sakstemaer.UNNTAK)
            .medBruker()
            .build()

        fagsak.registrertDato = Instant.now()
        fagsak.endretAv = "L12345"
        if (medFullmektig) {
            val fullmektig = Aktoer()
            fullmektig.rolle = Aktoersroller.FULLMEKTIG
            fullmektig.setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
            fagsak.leggTilAktør(fullmektig)
        }
        return fagsak
    }

    @JvmStatic
    @JvmOverloads
    fun lagPersondata(fødselsdato: LocalDate? = null): Persondata {
        val bostedsadresse = Bostedsadresse(
            StrukturertAdresse(ADRESSELINJE_1_BRUKER, "42 C", POSTNR_BRUKER, null, null, Landkoder.NO.kode),
            null, null, null, "PDL", null, false
        )

        val kontaktadresse = Kontaktadresse(
            StrukturertAdresse(ADRESSELINJE_1_BRUKER, null, POSTNR_BRUKER, POSTSTED_BRUKER, null, "NO"),
            null, null, null, null, "PDL", null, null,
            false
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

    @JvmStatic
    fun lagAdresse(): UstrukturertAdresse {
        val ustrukturertAdresse = UstrukturertAdresse()
        ustrukturertAdresse.adresselinje1 = ADRESSELINJE_1_BRUKER
        ustrukturertAdresse.postnr = POSTNR_BRUKER
        ustrukturertAdresse.poststed = POSTSTED_BRUKER
        ustrukturertAdresse.land = Land(Land.NORGE)
        return ustrukturertAdresse
    }

    @JvmStatic
    fun lagKontaktOpplysning(): Kontaktopplysning {
        val kontaktopplysning = Kontaktopplysning()
        kontaktopplysning.kontaktNavn = KONTAKT_NAVN
        return kontaktopplysning
    }

    @JvmStatic
    fun lagOrg(): OrganisasjonDokument {
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn(NAVN_ORG)
            .organisasjonsDetaljer(lagOrgDetaljer())
            .build()
    }

    @JvmStatic
    fun lagOrg(landkoder: Landkoder): OrganisasjonDokument {
        val semistrukturertAdresse = SemistrukturertAdresse()
        semistrukturertAdresse.landkode = landkoder.kode
        semistrukturertAdresse.gyldighetsperiode = Periode(LocalDate.now(), LocalDate.now())
        semistrukturertAdresse.postnr = POSTNR_ORG
        val organisasjonsDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(semistrukturertAdresse)
            .build()
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn(NAVN_ORG)
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()
    }

    private fun lagBehandlinger(): List<Behandling> {
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medType(Behandlingstyper.FØRSTEGANG)
            .medRegistrertDato(Instant.now())
            .build()

        return listOf(behandling)
    }

    private fun lagMottatteOpplysninger(): MottatteOpplysninger {
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.mottatteOpplysningerData = lagMottatteOpplysningerdata()
        return mottatteOpplysninger
    }

    @JvmStatic
    fun lagMottatteOpplysningerSøknadUtenforEØS(): MottatteOpplysninger {
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.mottatteOpplysningerData = lagMottatteOpplysningerdataSøknadUtenforEØS()
        return mottatteOpplysninger
    }

    private fun lagMottatteOpplysningerdata(): MottatteOpplysningerData {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.soeknadsland = Soeknadsland(listOf("AT"), false)
        return mottatteOpplysningerData
    }

    private fun lagMottatteOpplysningerdataSøknadUtenforEØS(): MottatteOpplysningerData {
        val mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        mottatteOpplysningerData.soeknadsland = Soeknadsland(listOf("AT"), false)
        mottatteOpplysningerData.trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        return mottatteOpplysningerData
    }

    private fun lagOrgDetaljer(): OrganisasjonsDetaljer {
        return OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(lagOrgAdresse())
            .build()
    }

    private fun lagOrgAdresse(): GeografiskAdresse {
        val semistrukturertAdresse = SemistrukturertAdresse()
        semistrukturertAdresse.adresselinje1 = POSTBOKS_ORG
        semistrukturertAdresse.postnr = POSTNR_ORG
        semistrukturertAdresse.gyldighetsperiode = Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2))
        semistrukturertAdresse.landkode = "NO"
        return semistrukturertAdresse
    }

    @JvmStatic
    fun lagTrygdeavtaleBehandling(): Behandling {
        return lagTrygdeavtaleBehandling(
            RepresentantIUtlandet.av(
                "Foretaksnavn",
                listOf("Uk address"),
                Landkoder.GB
            )
        )
    }

    @JvmStatic
    fun lagTrygdeavtaleBehandling(representantIUtlandet: RepresentantIUtlandet?): Behandling {
        val behandling = lagBehandling(lagFagsak())
        val mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        mottatteOpplysningerData.representantIUtlandet = representantIUtlandet
        behandling.mottatteOpplysninger?.mottatteOpplysningerData = mottatteOpplysningerData
        return behandling
    }

    @JvmStatic
    fun lagLovvalgsperiode(): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.fom = LOVVALGSPERIODE_FOM
        lovvalgsperiode.tom = LOVVALGSPERIODE_TOM
        lovvalgsperiode.dekning = Trygdedekninger.FULL_DEKNING_FTRL
        lovvalgsperiode.bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        return lovvalgsperiode
    }

    @JvmStatic
    fun lagMottaker(rolle: Mottakerroller): Mottaker {
        val mottaker = Mottaker()
        when (rolle) {
            Mottakerroller.BRUKER -> {
                mottaker.rolle = Mottakerroller.BRUKER
                mottaker.aktørId = FNR_BRUKER
            }

            Mottakerroller.VIRKSOMHET -> {
                mottaker.rolle = Mottakerroller.VIRKSOMHET
                mottaker.orgnr = ORGNR
            }

            Mottakerroller.ARBEIDSGIVER -> {
                mottaker.rolle = Mottakerroller.ARBEIDSGIVER
                mottaker.orgnr = ORGNR_FULLMEKTIG
            }

            else -> throw IllegalArgumentException("Støtter ikke mottakerrolle ${rolle.kode}")
        }
        return mottaker
    }

    @JvmStatic
    fun lagMottakerFullmektig(aktoertype: Aktoertype): Mottaker {
        val fullmektig = Mottaker()
        when (aktoertype) {
            Aktoertype.PERSON -> fullmektig.personIdent = FNR_FULLMEKTIG
            Aktoertype.ORGANISASJON -> fullmektig.orgnr = ORGNR_FULLMEKTIG
            else -> throw IllegalArgumentException("Fullmektig må være person eller organisasjon")
        }
        fullmektig.rolle = Mottakerroller.FULLMEKTIG
        return fullmektig
    }
}
