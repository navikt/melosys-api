package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.extracting
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.eessi.*
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.melosysmock.config.SoapConfig
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiRepo
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducer
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate
import java.util.*

@Import(SoapConfig::class)
class SedMottakTestIT(
    @Autowired private val eessiMeldingTestDataFactory: EessiMeldingTestDataFactory,
    @Autowired @Qualifier("melosysEessiMelding") private val melosysEessiMeldingKafkaTemplate: KafkaTemplate<String, MelosysEessiMelding>,
    @Autowired @Qualifier("jsonSomString") private val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val utpekingService: UtpekingService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak,
    @Autowired private val lovvalgsperiodeService: LovvalgsperiodeService,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
) : MockServerTestBaseWithProsessManager() {

    private val kafkaTopic = "teammelosys.eessi.v1-local"

    @MockkBean
    private lateinit var utstedtA1AivenProducer: UtstedtA1AivenProducer

    @Test
    fun `A009 med etterfølgende X008 skal gi fagsak annullert`() {
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A009.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA009 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            rinaSaksnummer = ref
            sedType = SedType.A009.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            artikkel = "12_1"
        }
        val eessiMeldingX008 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            rinaSaksnummer = ref
            sedType = SedType.X008.name
        }


        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 2,
                ProsessType.REGISTRERING_UNNTAK_NY_SAK to 1,
                ProsessType.MOTTAK_SED_JOURNALFØRING to 1,
                ProsessType.REGISTRERING_UNNTAK_GODKJENN to 1
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX008)
        }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .sortedBy { it.endretDato }

        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(5)
            .shouldContainInOrder(
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingX008.lagUnikIdentifikator(),
                eessiMeldingX008.lagUnikIdentifikator(),
            )

        prosessinstanserSortert.first { it.behandling != null }.behandling.run {
            status.shouldBe(Behandlingsstatus.AVSLUTTET)
            fagsak.status.shouldBe(Saksstatuser.ANNULLERT)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id)
                .shouldBePresent()
                .type.shouldBe(Behandlingsresultattyper.HENLEGGELSE)
        }

    }

    @Test
    fun `A009 med etterfølgende X006 skal gi fagsak annullert`() {
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A009.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA009 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            rinaSaksnummer = ref
            sedType = SedType.A009.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            artikkel = "12_1"
        }
        val eessiMeldingX006 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            rinaSaksnummer = ref
            sedType = SedType.X006.name
            isX006NavErFjernet = true
        }


        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 2,
                ProsessType.REGISTRERING_UNNTAK_NY_SAK to 1,
                ProsessType.MOTTAK_SED_JOURNALFØRING to 1,
                ProsessType.REGISTRERING_UNNTAK_GODKJENN to 1
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX006)
        }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .sortedBy { it.endretDato }

        extracting(prosessinstanserSortert) { låsReferanse }
            .shouldHaveSize(5)
            .shouldContainInOrder(
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingA009.lagUnikIdentifikator(),
                eessiMeldingX006.lagUnikIdentifikator(),
                eessiMeldingX006.lagUnikIdentifikator(),
            )

        prosessinstanserSortert.first { it.behandling != null }.behandling.run {
            status.shouldBe(Behandlingsstatus.AVSLUTTET)
            fagsak.status.shouldBe(Saksstatuser.ANNULLERT)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id)
                .shouldBePresent()
                .type.shouldBe(Behandlingsresultattyper.HENLEGGELSE)
        }
    }

    @Test
    fun `A003 med etterfølgende X006 og lovvalgsland er NO skal gi manuelt behandling`() {
        val ref = Random().nextInt(100000).toString()

        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            rinaSaksnummer = ref
            sedType = SedType.A003.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            artikkel = "13_1_a"
            lovvalgsland = "NO"
        }
        val eessiMeldingX006 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            rinaSaksnummer = ref
            sedType = SedType.X006.name
            isX006NavErFjernet = true
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 2,
                ProsessType.ARBEID_FLERE_LAND_NY_SAK to 1,
                ProsessType.MOTTAK_SED_JOURNALFØRING to 1
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX006)
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

        prosessinstanserSortert.first { it.behandling != null }.behandling.run {
            status.shouldBe(Behandlingsstatus.VURDER_DOKUMENT)
            fagsak.status.shouldBe(Saksstatuser.OPPRETTET)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id)
                .shouldBePresent().type.shouldBe(Behandlingsresultattyper.IKKE_FASTSATT)
        }
    }

    @Test
    fun `A003 med etterfølgende X008 og lovvalgsland er NO skal gi manuelt behandling`() {
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A003.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            rinaSaksnummer = ref
            sedType = SedType.A003.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            artikkel = "13_1_a"
            lovvalgsland = "NO"
        }
        val eessiMeldingX008 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            rinaSaksnummer = ref
            sedType = SedType.X008.name
            periode = null
            artikkel = null
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 2,
                ProsessType.ARBEID_FLERE_LAND_NY_SAK to 1,
                ProsessType.MOTTAK_SED_JOURNALFØRING to 1
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX008)
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

        prosessinstanserSortert.first { it.behandling != null }.behandling.run {
            status.shouldBe(Behandlingsstatus.VURDER_DOKUMENT)
            fagsak.status.shouldBe(Saksstatuser.OPPRETTET)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id).shouldBePresent().type
                .shouldBe(Behandlingsresultattyper.IKKE_FASTSATT)
        }
    }

    @Test
    fun `A003 med etterfølgende X008 og lovvalgsland ikke NO skal annullere saken og henlegge i melosys`() {
        val ref = Random().nextInt(100000).toString()

        val sedInfo = SedInformasjon(ref, SedType.A003.name, LocalDate.now(), LocalDate.now(), null, "AVBRUTT", null)
        val bucInformasjon = BucInformasjon(ref, true, null, LocalDate.now(), null, listOf(sedInfo))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            rinaSaksnummer = ref
            sedType = SedType.A003.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            artikkel = "13_1_a"
            lovvalgsland = "PL"
        }
        val eessiMeldingX008 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            rinaSaksnummer = ref
            sedType = SedType.X008.name
            periode = null
            artikkel = null
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.ARBEID_FLERE_LAND_NY_SAK to 1,
                ProsessType.MOTTAK_SED_JOURNALFØRING to 1,
                ProsessType.MOTTAK_SED to 2,
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX008)
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

        prosessinstanserSortert.first { it.behandling != null }.behandling.run {
            status.shouldBe(Behandlingsstatus.AVSLUTTET)
            fagsak.status.shouldBe(Saksstatuser.ANNULLERT)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id).shouldBePresent().type
                .shouldBe(Behandlingsresultattyper.HENLEGGELSE)
        }
        oppgaveRepo.repo.values
            .single()
            .run {
                status.shouldBe("FERDIGSTILT")
            }
    }

    @Test
    fun mottaSED_mottar3SED_blirBehandletEtterHverandre() {
        val rinaSaksnummer = Random().nextInt(100000).toString()

        //Periode på 6 år - fører til et kontrolltreff
        val eessiMeldingA009 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            this.rinaSaksnummer = rinaSaksnummer
            sedType = SedType.A009.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(6))
            artikkel = "12_1"
        }
        val eessiMeldingX001 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            this.rinaSaksnummer = rinaSaksnummer
            sedType = SedType.X001.name
        }
        val eessiMeldingX007 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_04.name
            this.rinaSaksnummer = rinaSaksnummer
            sedType = SedType.X007.name
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 3,
                ProsessType.REGISTRERING_UNNTAK_NY_SAK to 1,
                ProsessType.MOTTAK_SED_JOURNALFØRING to 2
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA009)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX001)
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingX007)
        }
        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(rinaSaksnummer)
            .sortedBy { it.endretDato }

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

    @ParameterizedTest(name = "{argumentSetName} - {0}")
    @MethodSource("toggleMedForventetdResultatForA003SendBrev")
    fun `Motta A003, godkjenne med A012, ugyldiggjøre godkjenning A012 med X008 for så å sende en A004`(forventedeProsessTyper: Pair<Boolean, Map<ProsessType, Int>>) {
        if (forventedeProsessTyper.first) fakeUnleash.enableAll() else fakeUnleash.disableAll()

        val utstedtA1MeldingCapturingSlot = slot<UtstedtA1Melding>()
        every { utstedtA1AivenProducer.produserMelding(capture(utstedtA1MeldingCapturingSlot)) } returns mockk<UtstedtA1Melding>()

        val rinaSaksnummer = Random().nextInt(100000).toString()

        val datoOmToÅr = LocalDate.now().plusYears(2)
        val sedInfoA003 =
            SedInformasjon(rinaSaksnummer, "A003id", datoOmToÅr, datoOmToÅr, SedType.A003.name, "MOTTATT", null)
        val sedInfoA012 =
            SedInformasjon(rinaSaksnummer, "A012id", datoOmToÅr, datoOmToÅr, SedType.A012.name, "SENDT", null)
        val bucInformasjon =
            BucInformasjon(rinaSaksnummer, true, "LA_BUC_02", LocalDate.now(), null, listOf(sedInfoA003, sedInfoA012))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)

        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            this.rinaSaksnummer = rinaSaksnummer
            sedType = SedType.A003.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(6))
            artikkel = "13_1_a"
            lovvalgsland = "NO"
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 1,
                ProsessType.ARBEID_FLERE_LAND_NY_SAK to 1
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
        }

        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(rinaSaksnummer)
            .sortedBy { it.endretDato }

        lovvalgsperiodeService.lagreLovvalgsperioder(
            prosessinstanserSortert.shouldHaveSize(2).last().behandlingOrFail().id,
            listOf(Lovvalgsperiode().apply {
                fom = datoOmToÅr
                tom = datoOmToÅr.plusDays(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            })
        )

        avklartefaktaService.lagreAvklarteFakta(
            prosessinstanserSortert.shouldHaveSize(2).last().behandlingOrFail().id, setOf(
                AvklartefaktaDto(
                    listOf("TRUE"), "VIRKSOMHET"
                ).apply {
                    avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
                    subjektID = "999999999"
                    begrunnelseKoder = emptyList()
                })
        )

        val vedtaksProsessInstans = prosessinstansTestManager.executeAndWait(
            forventedeProsessTyper.second
        ) {
            vedtaksfattingFasade.fattVedtak(
                prosessinstanserSortert.get(1).behandlingOrFail().id, FattVedtakRequest.Builder()
                    .medBehandlingsresultatType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND)
                    .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
                    .build()
            )
        }

        verify(exactly = 1) { utstedtA1AivenProducer.produserMelding(any()) }
        utstedtA1MeldingCapturingSlot.captured.apply {
            artikkel.shouldBe(Lovvalgsbestemmelse.ART_11_3_a)
        }

        val opprettNyVurderingProsessinstans = prosessinstansTestManager.executeAndWait(
            mapOf(ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to 1)
        ) {
            opprettBehandlingForSak.opprettBehandling(
                prosessinstanserSortert.get(1).behandlingOrFail().fagsak.saksnummer,
                OpprettSakDto().apply {
                    behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
                    behandlingstype = Behandlingstyper.NY_VURDERING
                    mottaksdato = LocalDate.now()
                    behandlingsaarsakType = Behandlingsaarsaktyper.HENVENDELSE
                    virksomhetOrgnr = "123456788"
                })
        }

        prosessinstansTestManager.executeAndWait(
            mapOf(ProsessType.UTPEKING_AVVIS to 1)
        ) {
            utpekingService.avvisUtpeking(opprettNyVurderingProsessinstans.behandlingOrFail().id, UtpekingAvvis().apply {
                begrunnelse = "lol"
                etterspørInformasjon = false
            })
        }

        MelosysEessiRepo.sedRepo.get(rinaSaksnummer)!!.shouldContainInOrder(
            SedType.A012,
            SedType.X008,
            SedType.A004,
        )
        vedtaksProsessInstans.behandling.run {
            status.shouldBe(Behandlingsstatus.AVSLUTTET)
            fagsak.status.shouldBe(Saksstatuser.LOVVALG_AVKLART)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id).shouldBePresent()
                .type.shouldBe(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND)
        }

        mockServer.verify(
            1,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/mal/orientering_til_arbeidsgiver_om_vedtak/lag-pdf?somKopi=false&utkast=false"))
        )
    }


    @ParameterizedTest
    @MethodSource("toggleMedForventetdResultatForA003SendBrev")
    fun `Motta A003, avvise med A004, ugyldiggjøre avvisning A004 med X008 for så å sende en A012`(forventedeProsessTyper: Pair<Boolean, Map<ProsessType, Int>>) {
        if (forventedeProsessTyper.first) fakeUnleash.enableAll() else fakeUnleash.disableAll()

        val utstedtA1MeldingCapturingSlot = slot<UtstedtA1Melding>()
        every { utstedtA1AivenProducer.produserMelding(capture(utstedtA1MeldingCapturingSlot)) } returns mockk<UtstedtA1Melding>()

        val rinaSaksnummer = Random().nextInt(100000).toString()

        val datoNå = LocalDate.now()
        val sedInfoA003 =
            SedInformasjon(rinaSaksnummer, "idA003", datoNå, datoNå, SedType.A003.name, "MOTTATT", null)
        val sedInfoA004 =
            SedInformasjon(rinaSaksnummer, "idA004", datoNå, datoNå, SedType.A004.name, "SENDT", null)
        val bucInformasjon =
            BucInformasjon(rinaSaksnummer, true, "LA_BUC_02", LocalDate.now(), null, listOf(sedInfoA003, sedInfoA004))
        MelosysEessiRepo.opprettBucinformasjon(bucInformasjon)


        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding {
            bucType = BucType.LA_BUC_02.name
            this.rinaSaksnummer = rinaSaksnummer
            sedType = SedType.A003.name
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
            artikkel = "13_1_a"
            lovvalgsland = "NO"
        }


        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 1,
                ProsessType.ARBEID_FLERE_LAND_NY_SAK to 1
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
        }


        val prosessinstanserSortert = prosessinstansRepository.findAllByLåsReferanseStartingWith(rinaSaksnummer)
            .sortedBy { it.endretDato }


        prosessinstansTestManager.executeAndWait(
            mapOf(ProsessType.UTPEKING_AVVIS to 1)
        ) {
            utpekingService.avvisUtpeking(
                prosessinstanserSortert
                    .shouldHaveSize(2).last().behandlingOrFail().id, UtpekingAvvis().apply {
                    begrunnelse = "lol"
                    etterspørInformasjon = false
                })
        }

        val opprettNyVurderingProsessinstans = prosessinstansTestManager.executeAndWait(
            mapOf(ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to 1)
        ) {
            opprettBehandlingForSak.opprettBehandling(
                prosessinstanserSortert.shouldHaveSize(2).last().behandlingOrFail().fagsak.saksnummer,
                OpprettSakDto().apply {
                    behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
                    behandlingstype = Behandlingstyper.NY_VURDERING
                    mottaksdato = LocalDate.now()
                    behandlingsaarsakType = Behandlingsaarsaktyper.HENVENDELSE
                    virksomhetOrgnr = "123456789"
                })
        }

        lovvalgsperiodeService.lagreLovvalgsperioder(
            opprettNyVurderingProsessinstans.behandlingOrFail().id,
            listOf(Lovvalgsperiode().apply {
                fom = LocalDate.now()
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            })
        )

        avklartefaktaService.lagreAvklarteFakta(
            opprettNyVurderingProsessinstans.behandlingOrFail().id, setOf(
                AvklartefaktaDto(
                    listOf("TRUE"), "VIRKSOMHET"
                ).apply {
                    avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
                    subjektID = "999999999"
                    begrunnelseKoder = emptyList()
                })
        )

        val vedtaksProsessInstans = prosessinstansTestManager.executeAndWait(
            forventedeProsessTyper.second
        ) {
            vedtaksfattingFasade.fattVedtak(
                opprettNyVurderingProsessinstans.behandlingOrFail().id, FattVedtakRequest.Builder()
                    .medBehandlingsresultatType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND)
                    .medVedtakstype(Vedtakstyper.KORRIGERT_VEDTAK)
                    .build()
            )
        }

        verify(exactly = 1) { utstedtA1AivenProducer.produserMelding(any()) }
        utstedtA1MeldingCapturingSlot.captured.apply {
            artikkel.shouldBe(Lovvalgsbestemmelse.ART_11_3_a)
        }

        MelosysEessiRepo.sedRepo.get(rinaSaksnummer)!!.shouldContainInOrder(
            SedType.A004,
            SedType.X008,
            SedType.A012,
        )
        vedtaksProsessInstans.behandling.apply {
            status.shouldBe(Behandlingsstatus.AVSLUTTET)
            fagsak.status.shouldBe(Saksstatuser.LOVVALG_AVKLART)
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(id).get().type.shouldBe(
                Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
            )
        }

        mockServer.verify(
            1,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/mal/orientering_til_arbeidsgiver_om_vedtak/lag-pdf?somKopi=false&utkast=false"))
        )
    }

    @Test
    fun `A003 skal virke med arbeidssted=null`() {
        val ref = Random().nextInt(100000).toString()
        val journalpostId = eessiMeldingTestDataFactory.opprettEessiJournalpost(ref)

        val melosysEessiMeldingSomFeilerProd = """
            {
                "sedId": "31fefdf52b264a04af5cf5dfc2d5b923",
                "sequenceId": null,
                "rinaSaksnummer": "$ref",
                "avsender": {
                    "avsenderID": "NL:1001",
                    "landkode": "NL"
                },
                "journalpostId": "$journalpostId",
                "dokumentId": null,
                "gsakSaksnummer": null,
                "aktoerId": "1111111111111",
                "statsborgerskap": [
                    {
                        "landkode": "NL"
                    }
                ],
                "arbeidssteder": [],
                "arbeidsland": [
                {
                    "land": "NO",
                    "arbeidssted": null
                }
              ],
                "periode": {
                    "fom": [2025,1,1],
                    "tom": [2025,12,31]
                },
                "lovvalgsland": "NL",
                "artikkel": "13_1_a",
                "erEndring": false,
                "midlertidigBestemmelse": true,
                "x006NavErFjernet": false,
                "ytterligereInformasjon": null,
                "bucType": "LA_BUC_02",
                "sedType": "A003",
                "sedVersjon": "1",
                "svarAnmodningUnntak": null,
                "anmodningUnntak": null
            }
        """

        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.MOTTAK_SED to 1,
                ProsessType.ARBEID_FLERE_LAND_NY_SAK to 1
            )
        ) {
            kafkaTemplate.send(kafkaTopic, melosysEessiMeldingSomFeilerProd)
        }

        prosessinstansRepository.findAllByLåsReferanseStartingWith(ref)
            .sortedBy { it.endretDato }
            .shouldHaveSize(2).last().run {
                behandling.status shouldBe Behandlingsstatus.OPPRETTET
                behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(behandling.id)
                    .shouldBePresent()
                    .type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
            }

    }

    companion object {
        @JvmStatic
        fun toggleMedForventetdResultatForA003SendBrev() = listOf(
            argumentSet(
                "toggle på", true to mapOf(
                    ProsessType.IVERKSETT_VEDTAK_EOS to 1,
                    ProsessType.SEND_BREV to 3,
                    ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 2
                )
            ),
            argumentSet(
                "toggle av",
                false to mapOf(
                    ProsessType.IVERKSETT_VEDTAK_EOS to 1,
                    ProsessType.SEND_BREV to 2,
                    ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
                )
            )
        )
    }
}
