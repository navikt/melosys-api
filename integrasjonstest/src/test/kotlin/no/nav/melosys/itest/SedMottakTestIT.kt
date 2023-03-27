package no.nav.melosys.itest

import io.kotest.assertions.extracting
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.arkiv.*
import no.nav.melosys.domain.eessi.*
import no.nav.melosys.domain.eessi.melding.Avsender
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.saksflyt.ProsessStatus
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiRepo
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.ProsessinstansRepository
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors

@Import(KodeverkStub::class)
class SedMottakTestIT(
    @Autowired private val joarkFasade: JoarkFasade,
    @Autowired @Qualifier("melosysEessiMelding") private val melosysEessiMeldingKafkaTemplate: KafkaTemplate<String, MelosysEessiMelding>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val unleash: FakeUnleash
) : ComponentTestBase() {

    lateinit var rinaSaksnummer: String
    private val kafkaTopic = "teammelosys.eessi.v1-local"

    @BeforeEach
    fun setup() {
        SakRepo.clear()
        rinaSaksnummer = Random().nextInt(100000).toString()
    }

    @Test
    fun `A009 med etterfølgende X008 skal gi fagsak annulert`() {
        unleash.enable("melosys.sed.x008")
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A009.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA009 = melosysEessiMelding(
            BucType.LA_BUC_04, ref, SedType.A009, Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
            "12_1", opprettEessiJournalpost(SedType.A009)
        )
        val eessiMeldingX008 = melosysEessiMelding(
            BucType.LA_BUC_04, ref, SedType.X008, null, null, opprettEessiJournalpost(SedType.X008)
        )

        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX008)

        await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(3))
            .until {
                prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(
                    listOf(ProsessStatus.FERDIG),
                    ref
                ).isEmpty()
            }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .stream()
            .sorted(Comparator.comparing { obj: Prosessinstans -> obj.endretDato })
            .collect(Collectors.toList())

        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(5)
            .shouldContainInOrder(
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingX008.lagUnikIdentifikator(),
                eessiMeldingX008.lagUnikIdentifikator(),
            )

        prosessinstanserSortert.filter { it.behandling != null }[0]
            .apply { behandling.status.shouldBe(Behandlingsstatus.AVSLUTTET) }
            .apply { behandling.fagsak.status.shouldBe(Saksstatuser.ANNULLERT) }
            .apply {
                behandlingsresultatRepository.findWithLovvalgsperioderById(behandling.id).get().type.shouldBe(
                    Behandlingsresultattyper.HENLEGGELSE
                )
            }


    }

    @Test
    fun `A009 med etterfølgende X006 skal gi fagsak annulert`() {
        unleash.enable("melosys.sed.x006")
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A009.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA009 = melosysEessiMelding(
            BucType.LA_BUC_04, ref, SedType.A009, Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
            "12_1", opprettEessiJournalpost(SedType.A009)
        )
        val eessiMeldingX006 = melosysEessiMelding(
            BucType.LA_BUC_04,
            ref,
            SedType.X006,
            null,
            null,
            opprettEessiJournalpost(SedType.X008),
            isX006NavErFjernet = true
        )

        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX006)

        await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(3))
            .until {
                prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(
                    listOf(ProsessStatus.FERDIG),
                    ref
                ).isEmpty()
            }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .stream()
            .sorted(Comparator.comparing { obj: Prosessinstans -> obj.endretDato })
            .collect(Collectors.toList())

        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(5)
            .shouldContainInOrder(
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingX006.lagUnikIdentifikator(),
                eessiMeldingX006.lagUnikIdentifikator(),
            )

        prosessinstanserSortert.filter { it.behandling != null }[0]
            .apply { behandling.status.shouldBe(Behandlingsstatus.AVSLUTTET) }
            .apply { behandling.fagsak.status.shouldBe(Saksstatuser.ANNULLERT) }
            .apply {
                behandlingsresultatRepository.findWithLovvalgsperioderById(behandling.id).get().type.shouldBe(
                    Behandlingsresultattyper.HENLEGGELSE
                )
            }
    }

    @Test
    fun `A003 med etterfølgende X006 og lovvalgsland er NO skal gi manuelt behandling`() {
        unleash.enable("melosys.sed.x006")
        val ref = Random().nextInt(100000).toString()

        val eessiMeldingA003 = melosysEessiMelding(
            BucType.LA_BUC_02, ref, SedType.A003, Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
            "13_1_a", opprettEessiJournalpost(SedType.A003), "NO"
        )
        val eessiMeldingX006 = melosysEessiMelding(
            BucType.LA_BUC_02,
            ref,
            SedType.X006,
            null,
            null,
            opprettEessiJournalpost(SedType.X006),
            isX006NavErFjernet = true
        )

        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX006)

        await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(3))
            .until {
                prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(
                    listOf(ProsessStatus.FERDIG),
                    ref
                ).isEmpty()
            }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .sortedBy { it.endretDato }

        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(4)
            .shouldContainInOrder(
                eessiMeldingA003.lagUnikIdentifikator(),
                eessiMeldingA003.lagUnikIdentifikator(),
                eessiMeldingX006.lagUnikIdentifikator(),
                eessiMeldingX006.lagUnikIdentifikator(),
            )

        prosessinstanserSortert.filter { it.behandling != null }[0].behandling.apply {
            status.shouldBe(Behandlingsstatus.VURDER_DOKUMENT)
            fagsak.status.shouldBe(Saksstatuser.OPPRETTET)
            behandlingsresultatRepository.findWithLovvalgsperioderById(id).get().type.shouldBe(
                Behandlingsresultattyper.IKKE_FASTSATT
            )
        }
    }

    @Test
    fun `A003 med etterfølgende X008 og lovvalgsland er NO skal gi manuelt behandling`() {
        unleash.enable("melosys.sed.x008")
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A003.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA003 = melosysEessiMelding(
            BucType.LA_BUC_02, ref, SedType.A003, Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
            "13_1_a", opprettEessiJournalpost(SedType.A003), "NO"
        )
        val eessiMeldingX008 = melosysEessiMelding(
            BucType.LA_BUC_02, ref, SedType.X008, null, null, opprettEessiJournalpost(SedType.X008)
        )

        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX008)

        await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(3))
            .until {
                prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(
                    listOf(ProsessStatus.FERDIG),
                    ref
                ).isEmpty()
            }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .sortedBy { it.endretDato }

        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(4)
            .shouldContainInOrder(
                eessiMeldingA003.lagUnikIdentifikator(),
                eessiMeldingA003.lagUnikIdentifikator(),
                eessiMeldingX008.lagUnikIdentifikator(),
                eessiMeldingX008.lagUnikIdentifikator(),
            )

        prosessinstanserSortert.filter { it.behandling != null }[0].behandling.apply {
            status.shouldBe(Behandlingsstatus.VURDER_DOKUMENT)
            fagsak.status.shouldBe(Saksstatuser.OPPRETTET)
            behandlingsresultatRepository.findWithLovvalgsperioderById(id).get().type.shouldBe(
                Behandlingsresultattyper.IKKE_FASTSATT
            )
        }
    }

    @Test
    fun mottaSED_mottar3SED_blirBehandletEtterHverandre() {
        //Periode på 6 år - fører til et kontrolltreff
        val eessiMeldingA009 = melosysEessiMelding(
            BucType.LA_BUC_04, rinaSaksnummer, SedType.A009, Periode(LocalDate.now(), LocalDate.now().plusYears(6)),
            "12_1", opprettEessiJournalpost(SedType.A009)
        )
        val eessiMeldingX001 = melosysEessiMelding(
            BucType.LA_BUC_04, rinaSaksnummer, SedType.X001, null, null, opprettEessiJournalpost(SedType.X001)
        )
        val eessiMeldingX007 = melosysEessiMelding(
            BucType.LA_BUC_04, rinaSaksnummer, SedType.X007, null, null, opprettEessiJournalpost(SedType.X007)
        )


        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX001)
        melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX007)

        await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(3))
            .until {
                prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(
                    listOf(ProsessStatus.FERDIG),
                    rinaSaksnummer
                ).isEmpty()
            }


        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(rinaSaksnummer)
            .stream()
            .sorted(Comparator.comparing { obj: Prosessinstans -> obj.endretDato })
            .collect(Collectors.toList())

//        Hver SED blir til en mottaksprosess + en behandlingsprosess
        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(6)
            .shouldContainExactly(
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingX001.lagUnikIdentifikator(),
                eessiMeldingX001.lagUnikIdentifikator(),
                eessiMeldingX007.lagUnikIdentifikator(),
                eessiMeldingX007.lagUnikIdentifikator(),
            )
    }

    private fun melosysEessiMelding(
        bucType: BucType,
        rinaSaksnummer: String?,
        sedType: SedType,
        periode: Periode?,
        artikkel: String?,
        journalpostID: String,
        lovvalgsland: String = "SE",
        isX006NavErFjernet: Boolean = false,
    ): MelosysEessiMelding {
        val eessiMelding = MelosysEessiMelding()
        eessiMelding.aktoerId = "1111111111111"
        eessiMelding.anmodningUnntak = null
        eessiMelding.arbeidssteder = emptyList()
        eessiMelding.bucType = bucType.name
        eessiMelding.gsakSaksnummer = null
        eessiMelding.artikkel = artikkel
        eessiMelding.avsender = Avsender("SE:123", "SE")
        eessiMelding.dokumentId = null
        eessiMelding.journalpostId = journalpostID
        eessiMelding.lovvalgsland = lovvalgsland
        eessiMelding.periode = periode
        eessiMelding.sedType = sedType.name
        eessiMelding.sedId = sedType.name
        eessiMelding.rinaSaksnummer = rinaSaksnummer
        eessiMelding.statsborgerskap = emptyList()
        eessiMelding.sedVersjon = "1"
        eessiMelding.isX006NavErFjernet = isX006NavErFjernet
        return eessiMelding
    }

    private fun opprettEessiJournalpost(sedType: SedType): String {
        val request = OpprettJournalpost()
        val hovedDokument = FysiskDokument()
        hovedDokument.dokumentKategori = "SED"
        hovedDokument.tittel = "$sedType-tittel"
        hovedDokument.brevkode = sedType.name
        hovedDokument.dokumentVarianter = listOf(
            DokumentVariant.lagDokumentVariant(
                ByteArray(0)
            )
        )
        request.setHoveddokument(hovedDokument)
        request.brukerId = "123123123"
        request.brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
        request.journalposttype = Journalposttype.INN
        request.journalførendeEnhet = "4530"
        request.tema = "UFM"
        request.korrespondansepartId = "SE:123"
        request.korrespondansepartNavn = "Sverige"
        request.korrespondansepartLand = "SE"
        request.setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON)
        request.mottaksKanal = "EESSI"
        request.journalposttype = Journalposttype.INN
        request.innhold = "$sedType-tittel"
        return joarkFasade.opprettJournalpost(request, false)
    }
}
