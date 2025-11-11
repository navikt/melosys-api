package no.nav.melosys.saksflytapi

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldContainValue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.saksflytapi.journalfoering.DokumentRequest
import no.nav.melosys.saksflytapi.journalfoering.JournalfoeringOpprettRequest
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ProsessinstansServiceTest {

    @RelaxedMockK
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @RelaxedMockK
    private lateinit var prosessinstansRepo: ProsessinstansForServiceRepository

    @RelaxedMockK
    private lateinit var threadPoolTaskExecutor: ThreadPoolTaskExecutor

    private lateinit var prosessinstansService: ProsessinstansService
    private val piListCaptor = mutableListOf<Prosessinstans>()

    @BeforeEach
    fun setUp() {
        piListCaptor.clear() // Clear captured calls between tests

        prosessinstansService = ProsessinstansService(
            applicationEventPublisher,
            prosessinstansRepo,
            threadPoolTaskExecutor
        )

        every { prosessinstansRepo.save(any<Prosessinstans>()) } answers {
            val prosessinstans = firstArg<Prosessinstans>()
            piListCaptor.add(prosessinstans)
            prosessinstans
        }
    }

    @Test
    fun `lagre prosessinstans med saksbehandler skal sette status og publisere event`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.ANMODNING_OM_UNNTAK
        }
        val saksbehandler = "Z123456"


        prosessinstansService.lagre(prosessinstans, saksbehandler, null)

        prosessinstans.status shouldBe ProsessStatus.KLAR
        prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)

        verify { applicationEventPublisher.publishEvent(any<ProsessinstansOpprettetEvent>()) }
    }

    @Test
    fun `lagre prosessinstans uten saksbehandler skal hente fra SubjectHandler`() {
        val saksbehandler = settInnloggetSaksbehandler()
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.ANMODNING_OM_UNNTAK
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }


        prosessinstansService.lagre(prosessinstans)


        prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER) shouldBe saksbehandler
        verify { applicationEventPublisher.publishEvent(any<ProsessinstansOpprettetEvent>()) }
    }

    @Test
    fun `opprett prosessinstans for anmodning om unntak skal sette korrekte data`() {
        val behandling = Behandling.forTest { }
        val mottakerInstitusjon = "SE:123"
        val dokumentReferanse = DokumentReferanse("jpID", "dokID")


        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(
            behandling,
            setOf(mottakerInstitusjon),
            setOf(dokumentReferanse),
            "FRITEKST_SED",
            ""
        )


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.ANMODNING_OM_UNNTAK
            behandling shouldBe behandling
            hentData<List<String>>(ProsessDataKey.EESSI_MOTTAKERE).first() shouldBe mottakerInstitusjon
            hentData<Set<DokumentReferanse>>(ProsessDataKey.VEDLEGG_SED) shouldBe setOf(dokumentReferanse)
            getData(ProsessDataKey.YTTERLIGERE_INFO_SED) shouldBe "FRITEKST_SED"
        }
    }

    @Test
    fun `opprett prosessinstans for iverksett vedtak skal lagre behandling og resultattype`() {
        val behandling = Behandling.forTest { }
        val resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        val mottakerInstitusjon = "DE:2332"


        prosessinstansService.opprettProsessinstansIverksettVedtakEos(
            behandling,
            resultatType,
            "FRITEKST",
            "FRITEKST_SED",
            setOf(mottakerInstitusjon),
            true
        )


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.IVERKSETT_VEDTAK_EOS
            hentData<List<String>>(ProsessDataKey.EESSI_MOTTAKERE).first() shouldBe mottakerInstitusjon
            behandling shouldBe behandling
            Behandlingsresultattyper.valueOf(getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE)!!) shouldBe resultatType
            getData(ProsessDataKey.YTTERLIGERE_INFO_SED) shouldBe "FRITEKST_SED"
        }
    }

    @Test
    fun `opprett prosessinstans for henlagt fagsak skal sette korrekt type`() {
        settInnloggetSaksbehandler()
        val behandling = Behandling.forTest { }


        prosessinstansService.opprettProsessinstansFagsakHenlagt(behandling)


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.HENLEGG_SAK
            behandling shouldBe behandling
        }
    }

    @Test
    fun `opprett prosessinstans for videresend søknad skal sette dokumentreferanser`() {
        settInnloggetSaksbehandler()
        val behandling = Behandling.forTest { }
        val dokumentReferanse = DokumentReferanse("jpID", "dokID")


        prosessinstansService.opprettProsessinstansVideresendSoknad(
            behandling,
            null,
            "T",
            setOf(dokumentReferanse)
        )


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.VIDERESEND_SOKNAD
            finnData<List<String>>(ProsessDataKey.EESSI_MOTTAKERE) shouldBe null
            behandling shouldBe behandling
            getData(ProsessDataKey.BEGRUNNELSE_FRITEKST).shouldNotBeBlank()
            hentData<Set<DokumentReferanse>>(ProsessDataKey.VEDLEGG_SED) shouldBe setOf(dokumentReferanse)
        }
    }

    private fun lagBehandling(): Behandling = Behandling.forTest {
        mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = MottatteOpplysningerData()
        }
    }

    @Test
    fun `opprett prosessinstans for opprett og distribuer brev til bruker skal sende til korrekt mottaker`() {
        val saksbehandler = settInnloggetSaksbehandler()
        val behandling = lagBehandling()
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.BRUKER
            aktørId = "123"
            personIdent = null
            orgnr = null
        }

        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .build()


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling)


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.OPPRETT_OG_DISTRIBUER_BREV
            hentData<String>(ProsessDataKey.MOTTAKER) shouldBe mottaker.rolle!!.name
            getData(ProsessDataKey.AKTØR_ID) shouldBe mottaker.aktørId
            val lagretBrevbestilling = hentData<MangelbrevBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            lagretBrevbestilling.produserbartdokument shouldBe MANGELBREV_BRUKER
            getData(ProsessDataKey.SAKSBEHANDLER) shouldBe saksbehandler
        }
    }

    @Test
    fun `opprett prosessinstans for opprett og distribuer brev til arbeidsgiver skal sende til organisasjon`() {
        val saksbehandler = settInnloggetSaksbehandler()
        val behandling = lagBehandling()
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.ARBEIDSGIVER
            aktørId = null
            personIdent = null
            orgnr = "987654321"
        }

        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .build()


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling)


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.OPPRETT_OG_DISTRIBUER_BREV
            hentData<String>(ProsessDataKey.MOTTAKER) shouldBe mottaker.rolle!!.name
            getData(ProsessDataKey.ORGNR) shouldBe mottaker.orgnr
            val lagretBrevbestilling = hentData<MangelbrevBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            lagretBrevbestilling.produserbartdokument shouldBe MANGELBREV_ARBEIDSGIVER
            getData(ProsessDataKey.SAKSBEHANDLER) shouldBe saksbehandler
        }
    }

    @Test
    fun `opprett prosessinstans for opprett og distribuer brev til fullmektig person skal håndtere personident`() {
        val saksbehandler = settInnloggetSaksbehandler()
        val behandling = lagBehandling()
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.FULLMEKTIG
            aktørId = null
            personIdent = "123"
            orgnr = null
        }

        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .build()


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling)


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.OPPRETT_OG_DISTRIBUER_BREV
            hentData<String>(ProsessDataKey.MOTTAKER) shouldBe mottaker.rolle!!.name
            getData(ProsessDataKey.PERSON_IDENT) shouldBe mottaker.personIdent
            val lagretBrevbestilling = hentData<MangelbrevBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            lagretBrevbestilling.produserbartdokument shouldBe MANGELBREV_ARBEIDSGIVER
            getData(ProsessDataKey.SAKSBEHANDLER) shouldBe saksbehandler
        }
    }

    @Test
    fun `opprett prosessinstans for opprett og distribuer brev til fullmektig organisasjon skal håndtere orgnr`() {
        val saksbehandler = settInnloggetSaksbehandler()
        val behandling = lagBehandling()
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.FULLMEKTIG
            aktørId = null
            personIdent = null
            orgnr = "987654321"
        }

        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_ARBEIDSGIVER)
            .build()


        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling)


        val lagretInstans = piListCaptor.last()
        lagretInstans.run {
            type shouldBe ProsessType.OPPRETT_OG_DISTRIBUER_BREV
            hentData<String>(ProsessDataKey.MOTTAKER) shouldBe mottaker.rolle!!.name
            getData(ProsessDataKey.ORGNR) shouldBe mottaker.orgnr
            val lagretBrevbestilling = hentData<MangelbrevBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            lagretBrevbestilling.produserbartdokument shouldBe MANGELBREV_ARBEIDSGIVER
            getData(ProsessDataKey.SAKSBEHANDLER) shouldBe saksbehandler
        }
    }

    @Test
    fun `opprett prosessinstans for journalføring med utenlandsk myndighet skal sette avsender i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            avsenderType = Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET
            avsenderID = "DK"
        }
        val institusjonsIdForDk = "ID_FOR_DK"


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.JFR_NY_SAK_BRUKER,
            journalfoeringOpprettRequest,
            institusjonsIdForDk,
            false
        )


        prosessinstans.getData(ProsessDataKey.AVSENDER_ID) shouldBe institusjonsIdForDk
    }

    @Test
    fun `opprett prosessinstans for journalføring med elektronisk mottakskanal skal ikke sette avsender i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            avsenderType = Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET
            avsenderID = "DK"
            avsenderNavn = "Trygdemyndighet i Danmark"
        }
        val institusjonsIdForDk = "ID_FOR_DK"


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.JFR_NY_SAK_BRUKER,
            journalfoeringOpprettRequest,
            institusjonsIdForDk,
            true
        )


        prosessinstans.run {
            finnData<Boolean>(ProsessDataKey.MOTTAKSKANAL_ER_ELEKTRONISK) shouldBe true
            getData(ProsessDataKey.AVSENDER_ID) shouldBe null
            finnData<String>(ProsessDataKey.AVSENDER_LAND) shouldBe null
            finnData<String>(ProsessDataKey.AVSENDER_NAVN) shouldBe null
            finnData<String>(ProsessDataKey.AVSENDER_TYPE) shouldBe null
        }
    }

    @Test
    fun `opprett prosessinstans for journalføring med forvaltningsmelding mottaker bruker skal settes i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }

        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.ANMODNING_OM_UNNTAK,
            journalfoeringOpprettRequest,
            null,
            false
        )

        prosessinstans.hentData<ForvaltningsmeldingMottaker>(
            ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER
        ) shouldBe ForvaltningsmeldingMottaker.BRUKER
    }

    @Test
    fun `opprett prosessinstans for journalføring med forvaltningsmelding mottaker ingen skal settes i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.INGEN
        }


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.ANMODNING_OM_UNNTAK,
            journalfoeringOpprettRequest,
            null,
            false
        )


        prosessinstans.hentData<ForvaltningsmeldingMottaker>(
            ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER
        ) shouldBe ForvaltningsmeldingMottaker.INGEN
    }

    @Test
    fun `opprett prosessinstans for journalføring med virksomhet orgnr skal settes i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            brukerID = null
            virksomhetOrgnr = "orgnr"
        }

        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.ANMODNING_OM_UNNTAK,
            journalfoeringOpprettRequest,
            null,
            false
        )

        prosessinstans.getData(ProsessDataKey.VIRKSOMHET_ORGNR) shouldBe "orgnr"
    }

    @Test
    fun `opprett prosessinstans for journalføring med skal tilordnes true skal settes i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            skalTilordnes = true
        }


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.ANMODNING_OM_UNNTAK,
            journalfoeringOpprettRequest,
            null,
            false
        )


        prosessinstans.finnData<Boolean>(ProsessDataKey.SKAL_TILORDNES) shouldBe true
    }

    @Test
    fun `opprett prosessinstans for journalføring med skal tilordnes false skal settes i prosessinstans`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            skalTilordnes = false
        }


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.ANMODNING_OM_UNNTAK,
            journalfoeringOpprettRequest,
            null,
            false
        )


        prosessinstans.finnData<Boolean>(ProsessDataKey.SKAL_TILORDNES) shouldBe false
    }

    @Test
    fun `opprett prosessinstans for journalføring med vedlegg skal sette vedlegg og titler`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            hoveddokument.shouldNotBeNull().run {
                dokumentID = "hovedDokumentID"
                logiskeVedlegg = mutableListOf("tittel")
            }
            vedlegg = listOf(
                DokumentRequest("dokID1", "tittel1", mutableListOf()),
                DokumentRequest("hovedDokumentID", "Logisk ??", mutableListOf()),
            )
        }


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.JFR_NY_SAK_BRUKER,
            journalfoeringOpprettRequest,
            null,
            false
        )


        val fysiskeVedlegg = prosessinstans.hentData<Map<String, String>>(ProsessDataKey.FYSISKE_VEDLEGG)
        fysiskeVedlegg.shouldContainKey("dokID1")
        fysiskeVedlegg.shouldContainKey("hovedDokumentID")
        fysiskeVedlegg.shouldContainValue("tittel1")
        fysiskeVedlegg.shouldContainValue("Logisk ??")

        val logiskeVedlegg = prosessinstans.hentData<List<String>>(ProsessDataKey.LOGISKE_VEDLEGG_TITLER)
        logiskeVedlegg shouldContainExactly listOf("tittel")
    }

    @Test
    fun `opprett prosessinstans for journalføring med fysiske vedlegg skal sette vedlegg og titler`() {
        val journalfoeringOpprettRequest = lagJournalfoeringOpprettRequest().apply {
            hoveddokument!!.dokumentID = "hovedDokumentID"
            val vedlegg = mutableListOf<DokumentRequest>()
            val fysiskVedlegg = DokumentRequest("dokID1", "tittel1", mutableListOf<String>())
            vedlegg.add(fysiskVedlegg)
            val fysiskVedlegg2 = DokumentRequest("hovedDokumentID", "Logisk ??", mutableListOf<String>())
            vedlegg.add(fysiskVedlegg2)
            this.vedlegg = vedlegg
        }


        val prosessinstans = prosessinstansService.lagJournalføringProsessinstans(
            ProsessType.JFR_NY_SAK_BRUKER,
            journalfoeringOpprettRequest,
            null,
            false
        )


        val fysiskeVedlegg = prosessinstans.hentData<Map<String, String>>(ProsessDataKey.FYSISKE_VEDLEGG)
        fysiskeVedlegg.shouldContainKey("dokID1")
        fysiskeVedlegg.shouldContainKey("hovedDokumentID")
        fysiskeVedlegg.shouldContainValue("tittel1")
        fysiskeVedlegg.shouldContainValue("Logisk ??")
    }

    @Test
    fun `opprett prosessinstans for registrer unntak fra medlemskap skal lagre prosessinstans når alt er ok`() {
        val behandling = lagBehandling()


        prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.AVSLUTTET)


        piListCaptor.last().run {
            behandling shouldBe behandling
            hentData<Saksstatuser>(ProsessDataKey.SAKSSTATUS) shouldBe Saksstatuser.AVSLUTTET
        }
    }

    @Test
    fun `opprett prosessinstans for godkjenn unntaksperiode med EESSI melding`() {
        val melosysEessiMelding = lagMelosysEessiMelding()

        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(
            Behandling.forTest { },
            false,
            "fritekst",
            melosysEessiMelding
        )


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.REGISTRERING_UNNTAK_GODKJENN
            getData(ProsessDataKey.YTTERLIGERE_INFO_SED) shouldBe "fritekst"
            hentData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING) shouldBe melosysEessiMelding
            låsReferanse shouldBe melosysEessiMelding.lagUnikIdentifikator()
        }
    }

    @Test
    fun `opprett prosessinstans for godkjenn unntaksperiode`() {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(
            Behandling.forTest { },
            false,
            "fritekst",
            null
        )


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.REGISTRERING_UNNTAK_GODKJENN
            getData(ProsessDataKey.YTTERLIGERE_INFO_SED) shouldBe "fritekst"
            finnData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING) shouldBe null
        }
    }

    @Test
    fun `opprett prosessinstans for ikke godkjenn unntaksperiode`() {
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(
            Behandling.forTest { },
            "fritekst"
        )


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.REGISTRERING_UNNTAK_AVVIS
            getData(ProsessDataKey.BEGRUNNELSE_FRITEKST) shouldBe "fritekst"
        }
    }

    @Test
    fun `opprett prosessinstans for ny sak med mottatt anmodning om unntak`() {
        prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(lagMelosysEessiMelding(), AKTØR_ID)


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK
            hentData<Sakstemaer>(ProsessDataKey.SAKSTEMA) shouldBe Sakstemaer.UNNTAK
            hentData<Behandlingstema>(ProsessDataKey.BEHANDLINGSTEMA) shouldBe Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            getData(ProsessDataKey.AKTØR_ID) shouldBe AKTØR_ID
        }
    }

    @Test
    fun `opprett prosessinstans for ny sak med unntaksregistrering`() {
        prosessinstansService.opprettProsessinstansNySakUnntaksregistrering(
            lagMelosysEessiMelding(),
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            AKTØR_ID
        )


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.REGISTRERING_UNNTAK_NY_SAK
            hentData<Sakstemaer>(ProsessDataKey.SAKSTEMA) shouldBe Sakstemaer.UNNTAK
            hentData<Behandlingstema>(
                ProsessDataKey.BEHANDLINGSTEMA
            ) shouldBe Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
            getData(ProsessDataKey.AKTØR_ID) shouldBe AKTØR_ID
        }
    }

    @Test
    fun `opprett prosessinstans for ny sak med arbeid flere land`() {

        prosessinstansService.opprettProsessinstansNySakArbeidFlereLand(
            lagMelosysEessiMelding(),
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.BESLUTNING_LOVVALG_NORGE,
            AKTØR_ID
        )


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.ARBEID_FLERE_LAND_NY_SAK
            hentData<Sakstemaer>(ProsessDataKey.SAKSTEMA) shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
            hentData<Behandlingstema>(ProsessDataKey.BEHANDLINGSTEMA) shouldBe Behandlingstema.BESLUTNING_LOVVALG_NORGE
            getData(ProsessDataKey.AKTØR_ID) shouldBe AKTØR_ID
        }
    }

    @Test
    fun `behandle mottatt melding skal opprette prosessinstans`() {
        val eessiMelding = lagMelosysEessiMelding()


        prosessinstansService.opprettProsessinstansSedMottak(eessiMelding)


        val prosessinstans = piListCaptor.last()
        prosessinstans.shouldNotBeNull().run {
            getData(ProsessDataKey.AKTØR_ID).shouldNotBeBlank()
            getData(ProsessDataKey.JOURNALPOST_ID).shouldNotBeBlank()
            getData(ProsessDataKey.GSAK_SAK_ID).shouldNotBeBlank()
            getData(ProsessDataKey.EESSI_MELDING).shouldNotBeBlank()
        }
    }

    @Test
    fun `opprett prosessinstans for søknad mottatt skal opprette prosessinstans når den ikke finnes fra før`() {

        prosessinstansService.opprettProsessinstansSøknadMottatt("søknadID", false, false)

        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            type shouldBe ProsessType.MOTTAK_SOKNAD_ALTINN
            getData(ProsessDataKey.MOTTATT_SOKNAD_ID) shouldBe "søknadID"
            hentData<ForvaltningsmeldingMottaker>(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER) shouldBe ForvaltningsmeldingMottaker.BRUKER
        }
    }

    @Test
    fun `opprett prosessinstans for søknad mottatt skal ikke opprette prosessinstans når den finnes fra før`() {
        prosessinstansService.opprettProsessinstansSøknadMottatt("søknadID", true, false)

        verify(exactly = 0) { prosessinstansRepo.save(any<Prosessinstans>()) }
    }

    @Test
    fun `opprett prosessinstans for søknad mottatt med mottak eldre enn noen dager skal sette forvaltningsmelding mottaker ingen`() {

        prosessinstansService.opprettProsessinstansSøknadMottatt("søknadID", false, true)


        val prosessinstans = piListCaptor.last()
        prosessinstans.hentData<ForvaltningsmeldingMottaker>(
            ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER
        ) shouldBe ForvaltningsmeldingMottaker.INGEN
    }

    @Test
    fun `opprett prosessinstans for send brev skal opprette ny prosessinstans`() {
        val behandling = Behandling.forTest { }
        val doksysbrevbestilling = DoksysBrevbestilling.Builder()
            .medProduserbartDokument(INNVILGELSE_YRKESAKTIV)
            .build()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)

        prosessinstansService.opprettProsessinstansSendBrev(behandling, doksysbrevbestilling, mottaker)


        val prosessinstans = piListCaptor.last()
        prosessinstans.run {
            this.behandling shouldBe behandling
            val lagretBrevbestilling = hentData<DoksysBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            lagretBrevbestilling.produserbartdokument shouldBe INNVILGELSE_YRKESAKTIV
            lagretBrevbestilling.mottakere shouldBe listOf(mottaker)
        }
    }

    @Test
    fun `opprett prosessinstanser for send brev med flere mottakere skal opprette ny prosessinstans per mottaker`() {
        val behandling = Behandling.forTest { }
        val doksysbrevbestilling = DoksysBrevbestilling.Builder()
            .medProduserbartDokument(INNVILGELSE_YRKESAKTIV)
            .build()
        val mottakere = listOf(
            Mottaker.medRolle(Mottakerroller.BRUKER),
            Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER),
            Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
        )


        prosessinstansService.opprettProsessinstanserSendBrev(behandling, doksysbrevbestilling, mottakere)


        verify(exactly = 3) { prosessinstansRepo.save(any()) }
        val capturedCalls = piListCaptor.takeLast(3) // Get last 3 calls for this test
        capturedCalls[0].hentData<DoksysBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            .shouldNotBeNull()
            .mottakere shouldBe listOf(Mottaker.medRolle(Mottakerroller.BRUKER))

        capturedCalls[1].hentData<DoksysBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            .shouldNotBeNull()
            .mottakere shouldBe listOf(Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER))

        capturedCalls[2].hentData<DoksysBrevbestilling>(ProsessDataKey.BREVBESTILLING)
            .shouldNotBeNull()
            .mottakere shouldBe listOf(Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET))
    }

    @Test
    fun `opprett prosessinstanser for årsavregning`() {
        prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
            "MEL-2", "2023", Behandlingsaarsaktyper.MELDING_FRA_SKATT
        )


        verify(exactly = 1) { prosessinstansRepo.save(any()) }
        piListCaptor.last().run {
            this shouldNotBe null
            getData(ProsessDataKey.SAKSNUMMER) shouldBe "MEL-2"
            getData(ProsessDataKey.GJELDER_ÅR) shouldBe "2023"
            hentData<Behandlingsaarsaktyper>(ProsessDataKey.ÅRSAK_TYPE) shouldBe Behandlingsaarsaktyper.MELDING_FRA_SKATT
        }
    }

    private fun lagMelosysEessiMelding() = MelosysEessiMelding().apply {
        sedId = UUID.randomUUID().toString()
        sedVersjon = "v4"
        aktoerId = "123"
        artikkel = "12_1"
        dokumentId = "123321"
        gsakSaksnummer = 432432L
        journalpostId = "j123"
        lovvalgsland = "SE"

        val periode = Periode().apply {
            fom = LocalDate.EPOCH
            tom = LocalDate.EPOCH.plusYears(1)
        }
        this.periode = periode

        val statsborgerskap = Statsborgerskap("SE")

        rinaSaksnummer = "r123"
        sedId = "s123"
        this.statsborgerskap = listOf(statsborgerskap)
        sedType = "A009"
        bucType = "LA_BUC_04"
    }

    private fun lagJournalfoeringOpprettRequest() = JournalfoeringOpprettRequest().apply {
        behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        journalpostID = "journalpostid"
        oppgaveID = "oppgaveid"
        brukerID = "brukerid"
        avsenderID = "avsenderid"
        avsenderNavn = "avsendernavn"
        hoveddokument = DokumentRequest("dokumentid", "hovedkokumenttittel", mutableListOf<String>())
    }

    private fun settInnloggetSaksbehandler(): String {
        val saksbehandler = "Z123456"
        val subjectHandler = mockk<SpringSubjectHandler>(relaxed = true)
        SubjectHandler.set(subjectHandler)
        every { subjectHandler.userID } returns saksbehandler
        every { subjectHandler.userName } returns saksbehandler
        return saksbehandler
    }

    companion object {
        private const val AKTØR_ID = "aktørId"
    }
}
