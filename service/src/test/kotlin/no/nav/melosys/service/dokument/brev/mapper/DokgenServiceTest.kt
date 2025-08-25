package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.Unleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.mockk.*
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.brev.*
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.StandardvedleggDto
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import no.nav.melosys.service.dokument.brev.SaksvedleggDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class DokgenServiceTest {

    private val dokgenConsumer: DokgenConsumer = mockk()
    private val dokumentproduksjonsInfoMapper: DokumentproduksjonsInfoMapper = mockk()
    private val joarkFasade: JoarkFasade = mockk()
    private val dokgenMalMapper: DokgenMalMapper = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val eregFasade: EregFasade = mockk()
    private val kontaktopplysningService: KontaktopplysningService = mockk()
    private val brevmottakerService: BrevmottakerService = mockk()
    private val prosessinstansService: ProsessinstansService = mockk()
    private val saksbehandlerService: SaksbehandlerService = mockk()
    private val utenlandskMyndighetService: UtenlandskMyndighetService = mockk()
    private val utledMottaksdato: UtledMottaksdato = mockk()
    private val unleash: Unleash = mockk()

    private lateinit var dokgenService: DokgenService

    private val behandlingId = 1L
    private val aktørId = "12345678910"

    @BeforeEach
    fun setup() {
        clearAllMocks()
        dokgenService = DokgenService(
            dokgenConsumer,
            dokumentproduksjonsInfoMapper,
            joarkFasade,
            dokgenMalMapper,
            behandlingService,
            eregFasade,
            kontaktopplysningService,
            brevmottakerService,
            prosessinstansService,
            saksbehandlerService,
            utenlandskMyndighetService,
            utledMottaksdato,
            unleash
        )
    }

    @Test
    fun `produserUtkast skal produsere pdf for bruker`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Mottakerroller.BRUKER
        )
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()
        every { unleash.isEnabled(any<String>()) } returns false

        val result = dokgenService.produserUtkast(behandlingId, brevbestillingDto)

        result shouldBe pdf
        verify { dokgenConsumer.lagPdf(any(), any(), false, true) }
    }

    @Test
    fun `produserUtkast skal produsere pdf for fullmektig`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Mottakerroller.FULLMEKTIG
        )
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG)
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()
        every { unleash.isEnabled(any<String>()) } returns false

        val result = dokgenService.produserUtkast(behandlingId, brevbestillingDto)

        result shouldBe pdf
    }

    @Test
    fun `produserUtkast skal produsere pdf for arbeidsgiver`() {
        val orgnr = "123456789"
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            Mottakerroller.ARBEIDSGIVER
        ).apply {
            this.orgnr = orgnr
        }
        val behandling = lagBehandling()
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns Optional.empty()
        every { eregFasade.hentOrganisasjon(any()) } returns mockk {
            every { dokument } returns OrganisasjonDokument("123456789", "Org navn", null, mockk(), "sektorkode")
        }
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()
        every { unleash.isEnabled(any<String>()) } returns false

        val result = dokgenService.produserUtkast(behandlingId, brevbestillingDto)

        result shouldBe pdf
    }

    @Test
    fun `produserUtkast skal produsere pdf for virksomhet`() {
        val orgnr = "123456789"
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET,
            Mottakerroller.VIRKSOMHET
        ).apply {
            this.orgnr = orgnr
        }
        val behandling = lagBehandling()
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns Optional.empty()
        every { eregFasade.hentOrganisasjon(any()) } returns mockk {
            every { dokument } returns OrganisasjonDokument("123456789", "Org navn", null, mockk(), "sektorkode")
        }
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()
        every { unleash.isEnabled(any<String>()) } returns false

        val result = dokgenService.produserUtkast(behandlingId, brevbestillingDto)

        result shouldBe pdf
    }

    @Test
    fun `produserUtkast skal produsere pdf for utenlandsk myndighet`() {
        val institusjonID = "NO:NAV"
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
            Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        ).apply {
            this.institusjonID = institusjonID
        }
        val behandling = lagBehandling()
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { utenlandskMyndighetService.hentUtenlandskMyndighet(any(), any()) } returns mockk()
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()
        every { unleash.isEnabled(any<String>()) } returns false

        val result = dokgenService.produserUtkast(behandlingId, brevbestillingDto)

        result shouldBe pdf
    }

    @Test
    fun `produserUtkast skal inkludere standardvedlegg for trygdeavtale GB når toggle er på`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.TRYGDEAVTALE_GB,
            Mottakerroller.BRUKER
        )
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()
        every { unleash.isEnabled(ToggleName.STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND) } returns true

        val result = dokgenService.produserUtkast(behandlingId, brevbestillingDto)

        result shouldBe pdf
        verify {
            dokgenMalMapper.mapBehandling(
                withArg { brevbestilling ->
                    brevbestilling.standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
                },
                any()
            )
        }
    }

    @Test
    fun `produserBrev skal produsere pdf med mottaker`() {
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER)
            .medBehandlingId(behandlingId)
            .build()
        val behandling = lagBehandling()
        val pdf = "PDF".toByteArray()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { dokumentproduksjonsInfoMapper.hentMalnavn(any()) } returns "malnavn"
        every { dokgenMalMapper.mapBehandling(any(), any()) } returns mockk()
        every { dokgenConsumer.lagPdf(any(), any(), any(), any()) } returns pdf
        every { utledMottaksdato.getMottaksdato(any(), isNull()) } returns LocalDate.now()

        val result = dokgenService.produserBrev(mottaker, brevbestilling)

        result shouldBe pdf
    }

    @Test
    fun `produserStandardvedlegg skal produsere standardvedlegg uten data`() {
        val standardvedleggType = StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
        val pdf = "PDF".toByteArray()

        every { dokgenConsumer.lagPdfForStandardvedlegg(any(), isNull()) } returns pdf

        val result = dokgenService.produserStandardvedlegg(standardvedleggType)

        result shouldBe pdf
        verify { dokgenConsumer.lagPdfForStandardvedlegg(standardvedleggType.malnavn, null) }
    }

    @Test
    fun `produserStandardvedlegg skal produsere standardvedlegg med data`() {
        val standardvedleggType = StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
        val standardvedleggDto = mockk<StandardvedleggDto>()
        val pdf = "PDF".toByteArray()

        every { dokgenConsumer.lagPdfForStandardvedlegg(any(), any()) } returns pdf

        val result = dokgenService.produserStandardvedlegg(standardvedleggType, standardvedleggDto)

        result shouldBe pdf
        verify { dokgenConsumer.lagPdfForStandardvedlegg(standardvedleggType.malnavn, standardvedleggDto) }
    }

    @Test
    fun `produserOgDistribuerBrev skal distribuere brev til bruker`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Mottakerroller.BRUKER
        )
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, any()) }
    }

    @Test
    fun `produserOgDistribuerBrev skal distribuere brev til arbeidsgiver med orgnr`() {
        val orgnr = "123456789"
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            Mottakerroller.ARBEIDSGIVER
        ).apply {
            this.orgnr = orgnr
        }
        val behandling = lagBehandling()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                withArg { m ->
                    m.rolle shouldBe Mottakerroller.ARBEIDSGIVER
                    m.orgnr shouldBe orgnr
                },
                any()
            )
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal distribuere brev til norsk myndighet`() {
        val orgnr1 = "123456789"
        val orgnr2 = "987654321"
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET,
            Mottakerroller.NORSK_MYNDIGHET
        ).apply {
            orgnrNorskMyndighet = listOf(orgnr1, orgnr2)
        }
        val behandling = lagBehandling()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify(exactly = 2) { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) }
        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                withArg { m ->
                    m.rolle shouldBe Mottakerroller.NORSK_MYNDIGHET
                    m.orgnr shouldBe orgnr1
                },
                any()
            )
        }
        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                withArg { m ->
                    m.rolle shouldBe Mottakerroller.NORSK_MYNDIGHET
                    m.orgnr shouldBe orgnr2
                },
                any()
            )
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal distribuere brev til annen person`() {
        val annenPersonIdent = "98765432109"
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Mottakerroller.BRUKER
        ).apply {
            annenPersonMottakerIdent = annenPersonIdent
        }
        val behandling = lagBehandling()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                withArg { m ->
                    m.rolle shouldBe Mottakerroller.BRUKER
                    m.personIdent shouldBe annenPersonIdent
                },
                any()
            )
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal håndtere kopimottakere`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Mottakerroller.BRUKER
        ).apply {
            kopiMottakere = listOf(
                KopiMottakerDto(Mottakerroller.ARBEIDSGIVER, "123456789", null, null),
                KopiMottakerDto(Mottakerroller.FULLMEKTIG, null, "98765432109", null)
            )
        }
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)
        val fullmektigMottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG)

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { brevmottakerService.avklarMottaker(any(), any(), any()) } returns fullmektigMottaker
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify(exactly = 3) { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) }
        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                withArg { m ->
                    m.rolle shouldBe Mottakerroller.ARBEIDSGIVER
                    m.orgnr shouldBe "123456789"
                },
                withArg { b ->
                    b.isBestillKopi() shouldBe true
                }
            )
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal håndtere vedlegg`() {
        val saksvedlegg = listOf(
            SaksvedleggDto("JP123", "DOK456"),
            SaksvedleggDto("JP789", "DOK012")
        )
        val fritekstvedlegg = listOf(
            FritekstvedleggDto("Tittel 1", "Fritekst 1"),
            FritekstvedleggDto("Tittel 2", "Fritekst 2")
        )
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Mottakerroller.BRUKER
        ).apply {
            this.saksVedlegg = saksvedlegg
            this.fritekstvedlegg = fritekstvedlegg
        }
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                mottaker,
                withArg { brevbestilling ->
                    brevbestilling.saksvedleggBestilling?.shouldHaveSize(2)
                    brevbestilling.fritekstvedleggBestilling?.shouldHaveSize(2)
                }
            )
        }
    }

    @Test
    fun `hentDokumentInfo skal returnere dokumentproduksjonsinfo`() {
        val produserbartDokument = Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
        val dokumentInfo = DokumentproduksjonsInfo("malnavn", "kategori", "tittel")

        every { dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(produserbartDokument) } returns dokumentInfo

        val result = dokgenService.hentDokumentInfo(produserbartDokument)

        result shouldBe dokumentInfo
    }

    @Test
    fun `erTilgjengeligDokgenmal skal returnere true for tilgjengelig mal`() {
        val produserbartDokument = Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER

        every { dokumentproduksjonsInfoMapper.tilgjengeligeMalerIDokgen() } returns setOf(produserbartDokument)

        val result = dokgenService.erTilgjengeligDokgenmal(produserbartDokument)

        result shouldBe true
    }

    @Test
    fun `erTilgjengeligDokgenmal skal returnere false for ikke tilgjengelig mal`() {
        val produserbartDokument = Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER

        every { dokumentproduksjonsInfoMapper.tilgjengeligeMalerIDokgen() } returns emptySet()

        val result = dokgenService.erTilgjengeligDokgenmal(produserbartDokument)

        result shouldBe false
    }

    @Test
    fun `produserOgDistribuerBrev skal håndtere mangelbrev med spesifikke felter`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.MANGELBREV_BRUKER,
            Mottakerroller.BRUKER
        ).apply {
            innledningFritekst = "Innledning"
            manglerFritekst = "Mangler"
            kontaktpersonNavn = "Kontaktperson"
        }
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                mottaker,
                withArg { brevbestilling ->
                    brevbestilling.distribusjonstype shouldBe Distribusjonstype.VIKTIG
                    (brevbestilling as MangelbrevBrevbestilling).innledningFritekst shouldBe "Innledning"
                    brevbestilling.manglerInfoFritekst shouldBe "Mangler"
                    brevbestilling.kontaktpersonNavn shouldBe "Kontaktperson"
                }
            )
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal håndtere fritekstbrev med dokumenttittel`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.FRITEKSTBREV,
            Mottakerroller.BRUKER
        ).apply {
            fritekstTittel = "Tittel"
            fritekst = "Fritekst"
            dokumentTittel = "Dokument tittel"
            distribusjonstype = Distribusjonstype.ANNET
            saksbehandlerNrToIdent = "X123456"
        }
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)
        val brevbestillingSlot = slot<DokgenBrevbestilling>()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { saksbehandlerService.hentNavnForIdent("X123456") } returns "Saksbehandler 2"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), capture(brevbestillingSlot)) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                mottaker,
                any()
            )
        }

        val brevbestilling = brevbestillingSlot.captured
        brevbestilling.run {
            distribusjonstype shouldBe Distribusjonstype.ANNET
            this shouldBe instanceOf(FritekstbrevBrevbestilling::class)
        }
        val fritekstbrev = brevbestilling as FritekstbrevBrevbestilling
        fritekstbrev.run {
            fritekstTittel shouldBe "Tittel"
            fritekst shouldBe "Fritekst"
            dokumentTittel shouldBe "Dokument tittel"
            saksbehandlerNrToNavn shouldBe "Saksbehandler 2"
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal håndtere henleggelsesbrev med begrunnelseskode`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.MELDING_HENLAGT_SAK,
            Mottakerroller.BRUKER
        ).apply {
            fritekst = "Henleggelse fritekst"
            begrunnelseKode = "BRUKER_DOED"
        }
        val behandling = lagBehandling()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), any(), any()) } returns listOf(mottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                mottaker,
                withArg { brevbestilling ->
                    brevbestilling.distribusjonstype shouldBe Distribusjonstype.VIKTIG
                    (brevbestilling as HenleggelseBrevbestilling).fritekst shouldBe "Henleggelse fritekst"
                    brevbestilling.begrunnelseKode shouldBe "BRUKER_DOED"
                }
            )
        }
    }

    @Test
    fun `produserBrev skal feile for utilgjengelig mal`() {
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.ATTEST_A1)
            .medBehandlingId(behandlingId)
            .build()
        val mottaker = Mottaker()
        val behandling = lagBehandling()

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { dokumentproduksjonsInfoMapper.hentMalnavn(Produserbaredokumenter.ATTEST_A1) } throws
            FunksjonellException("ProduserbartDokument ATTEST_A1 er ikke støttet")

        val exception = shouldThrow<FunksjonellException> {
            dokgenService.produserBrev(mottaker, brevbestilling)
        }

        exception.message shouldBe "ProduserbartDokument ATTEST_A1 er ikke støttet"
    }

    @Test
    fun `produserOgDistribuerBrev skal håndtere fullmektig privatperson med kopi`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
            Mottakerroller.ARBEIDSGIVER
        ).apply {
            orgnr = "123456789"
            manglerFritekst = "Mangler"
            kopiMottakere = listOf(
                KopiMottakerDto(Mottakerroller.FULLMEKTIG, null, null, null)
            )
        }
        val behandling = lagBehandling()
        val fullmektigMottaker = Mottaker(
            Mottakerroller.FULLMEKTIG,
            null,
            "12345678999",
            null,
            null,
            Land_iso2.NO
        )

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottaker(any(), any(), any()) } returns fullmektigMottaker
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify(exactly = 2) { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) }
        verify {
            prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(
                behandling,
                fullmektigMottaker,
                withArg { brevbestilling ->
                    brevbestilling.produserbartdokument shouldBe Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER
                    brevbestilling.behandlingId shouldBe behandlingId
                    (brevbestilling as MangelbrevBrevbestilling).manglerInfoFritekst shouldBe "Mangler"
                    brevbestilling.isBestillKopi() shouldBe true
                }
            )
        }
    }

    @Test
    fun `produserOgDistribuerBrev skal fjerne standardvedlegg for kopimottaker utenlandsk trygdemyndighet`() {
        val brevbestillingDto = lagBrevbestillingDto(
            Produserbaredokumenter.TRYGDEAVTALE_GB,
            Mottakerroller.BRUKER
        ).apply {
            standardvedleggType = StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
            kopiMottakere = listOf(
                KopiMottakerDto(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET, "123456789", null, "institusjonID")
            )
        }
        val behandling = lagBehandling()
        val brukerMottaker = Mottaker.medRolle(Mottakerroller.BRUKER)

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { brevmottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false)) } returns listOf(brukerMottaker)
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Saksbehandler Navn"
        every { unleash.isEnabled(ToggleName.STANDARDVEDLEGG_EGET_VEDLEGG_AVTALELAND) } returns true
        val brevbestillingSlot = mutableListOf<DokgenBrevbestilling>()
        every { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), capture(brevbestillingSlot)) } just runs

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto)

        verify(exactly = 2) { prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(any(), any(), any()) }

        // Check the captured brevbestillinger
        brevbestillingSlot.size shouldBe 2
        // First call should have standardvedlegg and not be a copy
        brevbestillingSlot[0].run {
            standardvedleggType shouldBe StandardvedleggType.VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE
            isBestillKopi() shouldBe false
        }
        // Second call (copy to utenlandsk trygdemyndighet) should have no standardvedlegg
        brevbestillingSlot[1].run {
            standardvedleggType shouldBe null
            isBestillKopi() shouldBe true
        }
    }

    // Helper methods
    private fun lagBrevbestillingDto(
        produserbartDokument: Produserbaredokumenter,
        mottaker: Mottakerroller
    ): BrevbestillingDto {
        return BrevbestillingDto().apply {
            produserbardokument = produserbartDokument
            this.mottaker = mottaker
            bestillersId = "X123456"
            kopiMottakere = emptyList()
            orgnrNorskMyndighet = emptyList()
        }
    }

    private fun lagBehandling(): Behandling = Behandling.forTest {
        id = behandlingId
        status = Behandlingsstatus.UNDER_BEHANDLING
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
    }
}
