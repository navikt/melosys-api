package no.nav.melosys.service.behandling

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.featuretoggle.ToggleName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.InvocationTargetException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ReplikerBehandlingsresultatServiceTest {
    private lateinit var behandlingsresultatOriginal: Behandlingsresultat
    private var behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val unleash = FakeUnleash()

    private lateinit var replikerBehandlingsresultatService: ReplikerBehandlingsresultatService

    @BeforeEach
    fun setup() {
        unleash.resetAll()

        replikerBehandlingsresultatService =
            ReplikerBehandlingsresultatService(behandlingsresultatService, unleash)
    }

    @Test
    @Throws(
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )

    fun replikerBehandlingOgBehandlingsresultat_replikererBehandlingsresultatObjekterOgCollections() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)
        val avklartefaktaOriginal = opprettAvklartefakta()
        behandlingsresultatOriginal.avklartefakta.add(avklartefaktaOriginal)
        val vilkaarsresultatOriginal = opprettVilkaarsresultat()
        behandlingsresultatOriginal.vilkaarsresultater.add(vilkaarsresultatOriginal)
        val lovvalgsperiodeOriginal = opprettLovvalgsperiode()
        behandlingsresultatOriginal.lovvalgsperioder.add(lovvalgsperiodeOriginal)
        behandlingsresultatOriginal.behandlingsresultatBegrunnelser.add(opprettBehandlingsresultatBegrunnelse())
        behandlingsresultatOriginal.kontrollresultater.add(opprettKontrollresultat())
        val anmodningsperiodeOriginal = opprettAnmodningsperiode()
        behandlingsresultatOriginal.anmodningsperioder.add(anmodningsperiodeOriginal)
        val utpekingsperiodeOriginal = opprettUtpekingsperiode()
        behandlingsresultatOriginal.utpekingsperioder.add(utpekingsperiodeOriginal)
        val innvilgetMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val avslaattMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT, 2L)
        val opphoertMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT, 3L)
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode)
        )
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode,
                grunnlagInntekstperiode = Inntektsperiode().apply {
                    id = 2L
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(1000.0)
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                })
        )
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode,
                grunnlagInntekstperiode = null,
            )
        )
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(avslaattMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(opphoertMedlemskapsperiode)
        behandlingsresultatOriginal.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG


        val behandlingReplika = Behandling.forTest {
            id = 2
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        behandlingsresultatReplika.apply {
            behandling shouldBe behandlingReplika
            id shouldBe null
            behandlingsmåte shouldBe behandlingsresultatOriginal.behandlingsmåte
            type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
            vedtakMetadata shouldBe null
            trygdeavgiftType shouldBe behandlingsresultatOriginal.trygdeavgiftType
        }

        behandlingsresultatReplika.lovvalgsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            fom shouldBe lovvalgsperiodeOriginal.fom
            tom shouldBe lovvalgsperiodeOriginal.tom
            medlPeriodeID shouldBe lovvalgsperiodeOriginal.medlPeriodeID
            dekning shouldBe lovvalgsperiodeOriginal.dekning
        }

        behandlingsresultatReplika.anmodningsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            medlPeriodeID shouldBe null
            erSendtUtland() shouldBe false
            anmodningsperiodeSvar shouldBe null
            fom shouldBe anmodningsperiodeOriginal.fom
            tom shouldBe anmodningsperiodeOriginal.tom
            lovvalgsland shouldBe anmodningsperiodeOriginal.lovvalgsland
            bestemmelse shouldBe anmodningsperiodeOriginal.bestemmelse
            dekning shouldBe anmodningsperiodeOriginal.dekning
        }

        behandlingsresultatReplika.utpekingsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            medlPeriodeID shouldBe null
            sendtUtland shouldBe null
            fom shouldBe utpekingsperiodeOriginal.fom
            tom shouldBe utpekingsperiodeOriginal.tom
            lovvalgsland shouldBe utpekingsperiodeOriginal.lovvalgsland
            bestemmelse shouldBe utpekingsperiodeOriginal.bestemmelse
        }

        val avklartefaktaReplika = behandlingsresultatReplika.avklartefakta.single()
        avklartefaktaReplika.apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            fakta shouldBe avklartefaktaOriginal.fakta
            type shouldBe avklartefaktaOriginal.type
        }
        avklartefaktaReplika.registreringer.single().apply {
            avklartefakta shouldBe avklartefaktaReplika
            id shouldBe null
            begrunnelseKode shouldBe avklartefaktaOriginal.registreringer.first().begrunnelseKode
        }

        val vilkaarsresultatReplika = behandlingsresultatReplika.vilkaarsresultater.single()
        vilkaarsresultatReplika.apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            begrunnelseFritekst shouldBe vilkaarsresultatOriginal.begrunnelseFritekst
            begrunnelseFritekstEessi shouldBe vilkaarsresultatOriginal.begrunnelseFritekstEessi
        }
        vilkaarsresultatReplika.begrunnelser.single().apply {
            vilkaarsresultat shouldBe vilkaarsresultatReplika
            id shouldBe null
            kode shouldBe vilkaarsresultatOriginal.begrunnelser.first().kode
        }

        behandlingsresultatReplika.behandlingsresultatBegrunnelser.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            kode shouldBe behandlingsresultatOriginal.behandlingsresultatBegrunnelser.first().kode
        }

        behandlingsresultatReplika.kontrollresultater.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            begrunnelse shouldBe behandlingsresultatOriginal.kontrollresultater.first().begrunnelse
        }

        behandlingsresultatReplika.utfallRegistreringUnntak.shouldBeNull()
        behandlingsresultatReplika.utfallUtpeking.shouldBeNull()

        behandlingsresultatOriginal.medlemskapsperioder shouldHaveSize 3
        val innvilgetMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.first { it.erInnvilget() }
        behandlingsresultatReplika.medlemskapsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            fom shouldBe innvilgetMedlemskapsperiodeOriginal.fom
            tom shouldBe innvilgetMedlemskapsperiodeOriginal.tom
            medlemskapstype shouldBe innvilgetMedlemskapsperiodeOriginal.medlemskapstype
            innvilgelsesresultat shouldBe innvilgetMedlemskapsperiodeOriginal.innvilgelsesresultat
            trygdedekning shouldBe innvilgetMedlemskapsperiodeOriginal.trygdedekning
            medlPeriodeID shouldBe innvilgetMedlemskapsperiodeOriginal.medlPeriodeID
            bestemmelse shouldBe innvilgetMedlemskapsperiodeOriginal.bestemmelse
        }

        val inntektsperioderOriginal = behandlingsresultatOriginal.hentInntektsperioder().toList()
        val inntektsperioderReplika = behandlingsresultatReplika.hentInntektsperioder().toList()

        inntektsperioderOriginal.forAll { original ->
            withClue("Forventet matchende Inntektsperiode for: $original") {
                inntektsperioderReplika.shouldExist { it.erReplikaAv(original) }
            }
        }

        val skatteforholdTilNorgeOriginal =
            behandlingsresultatOriginal.hentSkatteforholdTilNorge().first()
        behandlingsresultatReplika.hentSkatteforholdTilNorge().single().apply {
            id shouldBe null
            fomDato shouldBe skatteforholdTilNorgeOriginal.fomDato
            tomDato shouldBe skatteforholdTilNorgeOriginal.tomDato
            skatteplikttype shouldBe skatteforholdTilNorgeOriginal.skatteplikttype
        }


        val trygdeavgiftsperioderOriginal = behandlingsresultatOriginal.trygdeavgiftsperioder.toList()
        val trygdeavgiftsperioderReplika = behandlingsresultatReplika.trygdeavgiftsperioder.toList()

        trygdeavgiftsperioderOriginal.forAll { original ->
            withClue("Forventet matchende Trygdeavgiftsperiode for: $original") {
                trygdeavgiftsperioderReplika.shouldExist { it.erReplikaAv(original) }
            }
        }
    }

    @Test
    fun replikerBehandlingOgBehandlingsresultat_replikererBehandlingsresultatObjekterOgCollectionsLovvalgsperiodeMedTrygdeavgift() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)
        val avklartefaktaOriginal = opprettAvklartefakta()
        behandlingsresultatOriginal.avklartefakta.add(avklartefaktaOriginal)
        val vilkaarsresultatOriginal = opprettVilkaarsresultat()
        behandlingsresultatOriginal.vilkaarsresultater.add(vilkaarsresultatOriginal)
        behandlingsresultatOriginal.behandlingsresultatBegrunnelser.add(opprettBehandlingsresultatBegrunnelse())
        behandlingsresultatOriginal.kontrollresultater.add(opprettKontrollresultat())
        val anmodningsperiodeOriginal = opprettAnmodningsperiode()
        behandlingsresultatOriginal.anmodningsperioder.add(anmodningsperiodeOriginal)
        val utpekingsperiodeOriginal = opprettUtpekingsperiode()
        behandlingsresultatOriginal.utpekingsperioder.add(utpekingsperiodeOriginal)
        val innvilgetLovvalgsperiode = opprettLovvalgsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val avslaattLovvalgsperiode = opprettLovvalgsperiode(InnvilgelsesResultat.AVSLAATT, 2L)
        val opphoertLovvalgsperiode = opprettLovvalgsperiode(InnvilgelsesResultat.OPPHØRT, 3L)
        innvilgetLovvalgsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(grunnlagLovvalgsPeriode = innvilgetLovvalgsperiode)
        )
        innvilgetLovvalgsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagLovvalgsPeriode = innvilgetLovvalgsperiode,
                grunnlagInntekstperiode = Inntektsperiode().apply {
                    id = 2L
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(1000.0)
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                })
        )
        innvilgetLovvalgsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagLovvalgsPeriode = innvilgetLovvalgsperiode,
                grunnlagInntekstperiode = null,
            )
        )
        behandlingsresultatOriginal.apply {
            lovvalgsperioder.add(innvilgetLovvalgsperiode)
            lovvalgsperioder.add(avslaattLovvalgsperiode)
            lovvalgsperioder.add(opphoertLovvalgsperiode)
        }
        behandlingsresultatOriginal.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG


        val behandlingReplika = Behandling.forTest {
            id = 2
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        behandlingsresultatReplika.apply {
            behandling shouldBe behandlingReplika
            id shouldBe null
            behandlingsmåte shouldBe behandlingsresultatOriginal.behandlingsmåte
            type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
            vedtakMetadata shouldBe null
            trygdeavgiftType shouldBe behandlingsresultatOriginal.trygdeavgiftType
        }

        behandlingsresultatReplika.anmodningsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            medlPeriodeID shouldBe null
            erSendtUtland() shouldBe false
            anmodningsperiodeSvar shouldBe null
            fom shouldBe anmodningsperiodeOriginal.fom
            tom shouldBe anmodningsperiodeOriginal.tom
            lovvalgsland shouldBe anmodningsperiodeOriginal.lovvalgsland
            bestemmelse shouldBe anmodningsperiodeOriginal.bestemmelse
            dekning shouldBe anmodningsperiodeOriginal.dekning
        }

        behandlingsresultatReplika.utpekingsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            medlPeriodeID shouldBe null
            sendtUtland shouldBe null
            fom shouldBe utpekingsperiodeOriginal.fom
            tom shouldBe utpekingsperiodeOriginal.tom
            lovvalgsland shouldBe utpekingsperiodeOriginal.lovvalgsland
            bestemmelse shouldBe utpekingsperiodeOriginal.bestemmelse
        }

        val avklartefaktaReplika = behandlingsresultatReplika.avklartefakta.single()
        avklartefaktaReplika.apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            fakta shouldBe avklartefaktaOriginal.fakta
            type shouldBe avklartefaktaOriginal.type
        }
        avklartefaktaReplika.registreringer.single().apply {
            avklartefakta shouldBe avklartefaktaReplika
            id shouldBe null
            begrunnelseKode shouldBe avklartefaktaOriginal.registreringer.first().begrunnelseKode
        }

        val vilkaarsresultatReplika = behandlingsresultatReplika.vilkaarsresultater.single()
        vilkaarsresultatReplika.apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            begrunnelseFritekst shouldBe vilkaarsresultatOriginal.begrunnelseFritekst
            begrunnelseFritekstEessi shouldBe vilkaarsresultatOriginal.begrunnelseFritekstEessi
        }
        vilkaarsresultatReplika.begrunnelser.single().apply {
            vilkaarsresultat shouldBe vilkaarsresultatReplika
            id shouldBe null
            kode shouldBe vilkaarsresultatOriginal.begrunnelser.first().kode
        }

        behandlingsresultatReplika.behandlingsresultatBegrunnelser.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            kode shouldBe behandlingsresultatOriginal.behandlingsresultatBegrunnelser.first().kode
        }

        behandlingsresultatReplika.kontrollresultater.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            begrunnelse shouldBe behandlingsresultatOriginal.kontrollresultater.first().begrunnelse
        }

        behandlingsresultatReplika.utfallRegistreringUnntak.shouldBeNull()
        behandlingsresultatReplika.utfallUtpeking.shouldBeNull()

        behandlingsresultatOriginal.lovvalgsperioder shouldHaveSize 3
        val innvilgetLovvalgsperiodeOriginal = behandlingsresultatOriginal.lovvalgsperioder.first { it.erInnvilget() }
        behandlingsresultatReplika.lovvalgsperioder.single().apply {
            behandlingsresultat shouldBe behandlingsresultatReplika
            id shouldBe null
            fom shouldBe innvilgetLovvalgsperiodeOriginal.fom
            tom shouldBe innvilgetLovvalgsperiodeOriginal.tom
            medlemskapstype shouldBe innvilgetLovvalgsperiodeOriginal.medlemskapstype
            innvilgelsesresultat shouldBe innvilgetLovvalgsperiodeOriginal.innvilgelsesresultat
            dekning shouldBe innvilgetLovvalgsperiodeOriginal.dekning
            medlPeriodeID shouldBe innvilgetLovvalgsperiodeOriginal.medlPeriodeID
            bestemmelse shouldBe innvilgetLovvalgsperiodeOriginal.bestemmelse
        }

        val inntektsperioderOriginal = behandlingsresultatOriginal.hentInntektsperioder().toList()
        val inntektsperioderReplika = behandlingsresultatReplika.hentInntektsperioder().toList()

        inntektsperioderOriginal.forAll { original ->
            withClue("Forventet matchende Inntektsperiode for: $original") {
                inntektsperioderReplika.shouldExist { it.erReplikaAv(original) }
            }
        }

        val skatteforholdTilNorgeOriginal =
            behandlingsresultatOriginal.hentSkatteforholdTilNorge().first()
        behandlingsresultatReplika.hentSkatteforholdTilNorge().single().apply {
            id shouldBe null
            fomDato shouldBe skatteforholdTilNorgeOriginal.fomDato
            tomDato shouldBe skatteforholdTilNorgeOriginal.tomDato
            skatteplikttype shouldBe skatteforholdTilNorgeOriginal.skatteplikttype
        }


        val trygdeavgiftsperioderOriginal = behandlingsresultatOriginal.trygdeavgiftsperioder.toList()
        val trygdeavgiftsperioderReplika = behandlingsresultatReplika.trygdeavgiftsperioder.toList()

        trygdeavgiftsperioderOriginal.forAll { original ->
            withClue("Forventet matchende Trygdeavgiftsperiode for: $original") {
                trygdeavgiftsperioderReplika.shouldExist { it.erReplikaAv(original) }
            }
        }
    }

    @Test
    @Throws(
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )
    fun replikerBehandlingOgBehandlingsresultat_manglendeInnbetalingTrygdeavgift_replikererBehandlingsresultatObjekterOgCollections() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)
        val avklartefaktaOriginal = opprettAvklartefakta()
        behandlingsresultatOriginal.avklartefakta.add(avklartefaktaOriginal)
        val vilkaarsresultatOriginal = opprettVilkaarsresultat()
        behandlingsresultatOriginal.vilkaarsresultater.add(vilkaarsresultatOriginal)
        val lovvalgsperiodeOriginal = opprettLovvalgsperiode()
        behandlingsresultatOriginal.lovvalgsperioder.add(lovvalgsperiodeOriginal)
        behandlingsresultatOriginal.behandlingsresultatBegrunnelser.add(opprettBehandlingsresultatBegrunnelse())
        behandlingsresultatOriginal.kontrollresultater.add(opprettKontrollresultat())
        val anmodningsperiodeOriginal = opprettAnmodningsperiode()
        behandlingsresultatOriginal.anmodningsperioder.add(anmodningsperiodeOriginal)
        val utpekingsperiodeOriginal = opprettUtpekingsperiode()
        behandlingsresultatOriginal.utpekingsperioder.add(utpekingsperiodeOriginal)
        val innvilgetMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val avslaattMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT, 2L)
        val opphoertMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT, 3L)
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode)
        )
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(avslaattMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(opphoertMedlemskapsperiode)
        behandlingsresultatOriginal.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured
        behandlingsresultatOriginal.medlemskapsperioder.shouldHaveSize(3)
        val innvilgetMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.first { it.erInnvilget() }
        val opphørtMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.first { it.erOpphørt() }
        behandlingsresultatReplika.medlemskapsperioder
            .shouldHaveSize(2)
            .sortedBy { it.innvilgelsesresultat }
            .run {
                first().run {
                    behandlingsresultat.shouldBe(behandlingsresultatReplika)
                    id.shouldBe(null)
                    fom.shouldBe(innvilgetMedlemskapsperiodeOriginal.fom)
                    tom.shouldBe(innvilgetMedlemskapsperiodeOriginal.tom)
                    medlemskapstype.shouldBe(innvilgetMedlemskapsperiodeOriginal.medlemskapstype)
                    innvilgelsesresultat.shouldBe(innvilgetMedlemskapsperiodeOriginal.innvilgelsesresultat)
                    trygdedekning.shouldBe(innvilgetMedlemskapsperiodeOriginal.trygdedekning)
                    medlPeriodeID.shouldBe(innvilgetMedlemskapsperiodeOriginal.medlPeriodeID)
                    bestemmelse.shouldBe(innvilgetMedlemskapsperiodeOriginal.bestemmelse)
                }
                last().run {
                    behandlingsresultat.shouldBe(behandlingsresultatReplika)
                    id.shouldBe(null)
                    fom.shouldBe(opphørtMedlemskapsperiodeOriginal.fom)
                    tom.shouldBe(opphørtMedlemskapsperiodeOriginal.tom)
                    medlemskapstype.shouldBe(opphørtMedlemskapsperiodeOriginal.medlemskapstype)
                    innvilgelsesresultat.shouldBe(opphørtMedlemskapsperiodeOriginal.innvilgelsesresultat)
                    trygdedekning.shouldBe(opphørtMedlemskapsperiodeOriginal.trygdedekning)
                    medlPeriodeID.shouldBe(opphørtMedlemskapsperiodeOriginal.medlPeriodeID)
                    bestemmelse.shouldBe(opphørtMedlemskapsperiodeOriginal.bestemmelse)
                }
            }
    }


    @Test
    fun replikerBehandlingOgBehandlingsresultat_EØSPensjonist_replikererBehandlingsresultatObjekterOgCollections() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }

        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = opprettHelseutgiftDekkesPeriode(behandlingsresultatOriginal)
        val trygdeavgiftsperiode = lagTrygdeavgiftsperiode().copyEntity(grunnlagHelseutgiftDekkesPeriode = helseutgiftDekkesPeriode)

        helseutgiftDekkesPeriode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        behandlingsresultatOriginal.helseutgiftDekkesPeriode = helseutgiftDekkesPeriode
        behandlingsresultatOriginal.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured
        behandlingsresultatOriginal.helseutgiftDekkesPeriode.shouldNotBeNull()

        behandlingsresultatReplika.run {
            behandling!!.erEøsPensjonist() shouldBe true
            behandling!!.type shouldBe Behandlingstyper.NY_VURDERING
        }

        behandlingsresultatReplika.hentHelseutgiftDekkesPeriode()
            .run {
                behandlingsresultat shouldBe behandlingsresultatReplika
                trygdeavgiftsperioder shouldBe trygdeavgiftsperioder
                fomDato shouldBe behandlingsresultatOriginal.hentHelseutgiftDekkesPeriode().fomDato
                tomDato shouldBe behandlingsresultatOriginal.hentHelseutgiftDekkesPeriode().tomDato
                bostedLandkode shouldBe behandlingsresultatOriginal.hentHelseutgiftDekkesPeriode().bostedLandkode
            }
    }

    @Test
    fun `replikering av behandlingsresultat - manglende skatteforholdsperiode kaster exception`() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val innvilgetMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val trygdeavgiftsperiode = lagTrygdeavgiftsperiode().copyEntity(
            grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode,
            grunnlagInntekstperiode = null,
            grunnlagSkatteforholdTilNorge = null
        )
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        assertThrows<IllegalStateException> {
            replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)
        }.message shouldBe "SkatteforholdTilNorge ikke funnet"
    }

    @Test
    fun `replikering av behandlingsresultat - manglende medlemskapsperiode kaster exception`() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val innvilgetMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val trygdeavgiftsperiode = lagTrygdeavgiftsperiode()

        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0

        assertThrows<IllegalStateException> {
            replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)
        }
    }

    private fun lagTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        val inntektsperiode = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(1000.0)
            isArbeidsgiversavgiftBetalesTilSkatt = false
        }
        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        return Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.now(),
            periodeTil = LocalDate.now(),
            trygdeavgiftsbeløpMd = Penger(500.0),
            trygdesats = BigDecimal(50),
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforholdTilNorge
        )
    }

    private fun opprettMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat, id: Long): Medlemskapsperiode {
        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.id = id
        medlemskapsperiode.innvilgelsesresultat = innvilgelsesResultat
        medlemskapsperiode.medlPeriodeID = 77L
        medlemskapsperiode.fom = LocalDate.now()
        medlemskapsperiode.tom = LocalDate.now()
        medlemskapsperiode.medlemskapstype = Medlemskapstyper.PLIKTIG
        medlemskapsperiode.trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        medlemskapsperiode.medlPeriodeID = 123L
        medlemskapsperiode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        return medlemskapsperiode
    }

    private fun opprettHelseutgiftDekkesPeriode(behandlingsresultat: Behandlingsresultat): HelseutgiftDekkesPeriode {
        val helseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
            behandlingsresultat,
            fomDato = LocalDate.now(),
            tomDato = LocalDate.now(),
            bostedLandkode = Land_iso2.DK
        )

        return helseutgiftDekkesPeriode
    }

    private fun opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling: Behandling): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = 30L
        behandlingsresultat.behandling = tidligsteInaktiveBehandling
        behandlingsresultat.behandlingsmåte = Behandlingsmaate.MANUELT
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        val vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtaksdato = Instant.parse("2002-02-11T09:37:30Z")
        behandlingsresultat.vedtakMetadata = vedtakMetadata
        behandlingsresultat.avklartefakta = LinkedHashSet()
        behandlingsresultat.lovvalgsperioder = LinkedHashSet()
        behandlingsresultat.vilkaarsresultater = LinkedHashSet()
        behandlingsresultat.utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
        behandlingsresultat.utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
        return behandlingsresultat
    }

    private fun opprettLovvalgsperiode(innvilgelsesResultat: InnvilgelsesResultat? = InnvilgelsesResultat.INNVILGET, id: Long? = 32L): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.id = id
        lovvalgsperiode.innvilgelsesresultat = innvilgelsesResultat
        lovvalgsperiode.behandlingsresultat = behandlingsresultatOriginal
        lovvalgsperiode.dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        lovvalgsperiode.fom = LocalDate.now()
        lovvalgsperiode.tom = LocalDate.now().plusMonths(2)
        lovvalgsperiode.medlPeriodeID = 777L
        return lovvalgsperiode
    }

    private fun opprettAnmodningsperiode(): Anmodningsperiode {
        val anmodningsperiode = Anmodningsperiode()
        anmodningsperiode.id = 32L
        anmodningsperiode.fom = LocalDate.now()
        anmodningsperiode.tom = LocalDate.now().plusYears(1L)
        anmodningsperiode.lovvalgsland = Land_iso2.SE
        anmodningsperiode.unntakFraLovvalgsland = Land_iso2.NO
        anmodningsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        anmodningsperiode.unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        anmodningsperiode.tilleggsbestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1
        anmodningsperiode.behandlingsresultat = behandlingsresultatOriginal
        anmodningsperiode.setSendtUtland(true)
        anmodningsperiode.anmodningsperiodeSvar = AnmodningsperiodeSvar()
        anmodningsperiode.dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        return anmodningsperiode
    }

    private fun opprettUtpekingsperiode(): Utpekingsperiode {
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now(), LocalDate.now().plusYears(1), Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        )
        utpekingsperiode.id = 11111L
        utpekingsperiode.medlPeriodeID = 1242L
        utpekingsperiode.sendtUtland = LocalDate.now()
        return utpekingsperiode
    }

    private fun opprettAvklartefakta(): Avklartefakta {
        val avklartefakta = Avklartefakta()
        avklartefakta.id = 32L
        avklartefakta.behandlingsresultat = behandlingsresultatOriginal
        avklartefakta.fakta = "fakta"
        avklartefakta.type = Avklartefaktatyper.ARBEIDSLAND
        val avklartefaktaRegistrering = AvklartefaktaRegistrering()
        avklartefaktaRegistrering.begrunnelseKode = "registrering_begrunnelsekode"
        avklartefakta.registreringer.add(avklartefaktaRegistrering)
        return avklartefakta
    }

    private fun opprettBehandlingsresultatBegrunnelse(): BehandlingsresultatBegrunnelse {
        val behandlingsresultatBegrunnelse = BehandlingsresultatBegrunnelse()
        behandlingsresultatBegrunnelse.id = 32L
        behandlingsresultatBegrunnelse.behandlingsresultat = behandlingsresultatOriginal
        behandlingsresultatBegrunnelse.kode = "begrunnelsekode"
        return behandlingsresultatBegrunnelse
    }

    private fun opprettVilkaarsresultat(): Vilkaarsresultat {
        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.behandlingsresultat = behandlingsresultatOriginal
        vilkaarsresultat.id = 32L
        vilkaarsresultat.begrunnelseFritekst = "fritekst"
        vilkaarsresultat.begrunnelseFritekstEessi = "free text"
        val begrunnelser = HashSet<VilkaarBegrunnelse>()
        val vilkaarBegrunnelse = VilkaarBegrunnelse()
        vilkaarBegrunnelse.id = 2222L
        vilkaarBegrunnelse.kode = "kode"
        begrunnelser.add(vilkaarBegrunnelse)
        vilkaarsresultat.begrunnelser = begrunnelser
        return vilkaarsresultat
    }

    private fun opprettKontrollresultat(): Kontrollresultat {
        val kontrollresultat = Kontrollresultat()
        kontrollresultat.id = 123L
        kontrollresultat.behandlingsresultat = behandlingsresultatOriginal
        kontrollresultat.begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
        return kontrollresultat
    }


    @Test
    fun `filtrerTrygdeavgiftsperioder med toggle PÅ - feiler når trygdeavgiftsperiode går over flere år`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val trygdeavgiftsperioder = listOf(
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = LocalDate.of(inneværendeÅr - 1, 9, 1),
                periodeTil = LocalDate.of(inneværendeÅr, 8, 31),
                trygdeavgiftsbeløpMd = Penger(1000.0),
                trygdesats = BigDecimal("7.8")
            )
        )

        val exception =
            shouldThrow<IllegalStateException> { replikerBehandlingsresultatService.filtrerTrygdeavgiftsperioder(trygdeavgiftsperioder) }

        exception.message shouldBe "Trygdeavgiftsperiode 1 går over flere år (2024-09-01 - 2025-08-31)"
    }

    @Test
    fun `filtrerTrygdeavgiftsperioder med toggle PÅ - filtrerer bort perioder som slutter før inneværende år`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val trygdeavgiftsperioder = listOf(
            // Periode som slutter før inneværende år - skal filtreres bort
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
                periodeTil = LocalDate.of(inneværendeÅr - 2, 12, 31),
                trygdeavgiftsbeløpMd = Penger(500.0),
                trygdesats = BigDecimal("5.1")
            ),
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = LocalDate.of(inneværendeÅr - 1, 1, 1),
                periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
                trygdeavgiftsbeløpMd = Penger(500.0),
                trygdesats = BigDecimal("5.1")
            ),
            // Periode som går inn i inneværende år - skal beholdes og avkortes
            Trygdeavgiftsperiode(
                id = 2L,
                periodeFra = LocalDate.of(inneværendeÅr, 1, 1),
                periodeTil = LocalDate.of(inneværendeÅr, 6, 30),
                trygdeavgiftsbeløpMd = Penger(1500.0),
                trygdesats = BigDecimal("7.8")
            )
        )

        val resultat = replikerBehandlingsresultatService.filtrerTrygdeavgiftsperioder(trygdeavgiftsperioder)

        resultat shouldHaveSize 1
        resultat[0].periodeFra shouldBe LocalDate.of(inneværendeÅr, 1, 1) // Avkortet
        resultat[0].periodeTil shouldBe LocalDate.of(inneværendeÅr, 6, 30)
        resultat[0].trygdeavgiftsbeløpMd shouldBe Penger(1500.0)
    }

    @Test
    fun `replikerTrygdeavgiftForPensjonist med toggle PÅ - feiler når trygdeavgiftsperioder går over flere år`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = opprettHelseutgiftDekkesPeriode(behandlingsresultatOriginal)

        val inntektsperiode = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 6, 1)
            tomDato = LocalDate.of(inneværendeÅr, 6, 30)
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr, 12, 31)
        }

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr - 1, 9, 1), // Starter før inneværende år
            periodeTil = LocalDate.of(inneværendeÅr, 8, 31),    // Slutter i inneværende år
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        helseutgiftDekkesPeriode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        behandlingsresultatOriginal.helseutgiftDekkesPeriode = helseutgiftDekkesPeriode

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        val exception =
            shouldThrow<IllegalStateException> {
                replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)
            }

        exception.message shouldBe "Trygdeavgiftsperiode 1 går over flere år (2024-09-01 - 2025-08-31)"
    }

    @Test
    fun `replikerTrygdeavgiftForPensjonist med toggle PÅ - avkorter også inntektsperioder og skatteforhold som starter før inneværende år`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = opprettHelseutgiftDekkesPeriode(behandlingsresultatOriginal)

        // Opprett inntektsperiode som overlapper med inneværende år (skal avkortes)
        val overlappendeInntektsperiode = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 1, 1)  // Starter før inneværende år
            tomDato = LocalDate.of(inneværendeÅr, 12, 31)    // Slutter i inneværende år
        }

        // Opprett skatteforhold som overlapper med inneværende år (skal avkortes)
        val overlappendeSkatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 6, 1) // Starter før inneværende år
            tomDato = LocalDate.of(inneværendeÅr, 6, 30)     // Slutter i inneværende år
        }

        // Trygdeavgiftsperiode som overlapper med inneværende år
        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr - 1, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagInntekstperiode = overlappendeInntektsperiode,  // Denne skal avkortes
            grunnlagSkatteforholdTilNorge = overlappendeSkatteforhold // Denne skal avkortes
        )

        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(inneværendeÅr, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagInntekstperiode = overlappendeInntektsperiode,  // Denne skal avkortes
            grunnlagSkatteforholdTilNorge = overlappendeSkatteforhold // Denne skal avkortes
        )

        helseutgiftDekkesPeriode.trygdeavgiftsperioder.add(trygdeavgiftsperiode1)
        helseutgiftDekkesPeriode.trygdeavgiftsperioder.add(trygdeavgiftsperiode2)

        behandlingsresultatOriginal.helseutgiftDekkesPeriode = helseutgiftDekkesPeriode

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        // Trygdeavgiftsperioden skal beholdes men miste sin gamle inntektsperiode
        behandlingsresultatReplika.helseutgiftDekkesPeriode!!.trygdeavgiftsperioder shouldHaveSize 1
        val replisertPeriode = behandlingsresultatReplika.helseutgiftDekkesPeriode!!.trygdeavgiftsperioder.first()

        // Trygdeavgiftsperiode skal være avkortet
        replisertPeriode.periodeFra shouldBe LocalDate.of(inneværendeÅr, 1, 1) // Avkortet

        // Inntektsperioden skal også være avkortet til å starte 1. januar i inneværende år
        replisertPeriode.grunnlagInntekstperiode?.fomDato shouldBe LocalDate.of(inneværendeÅr, 1, 1) // Avkortet
        replisertPeriode.grunnlagInntekstperiode?.tomDato shouldBe LocalDate.of(inneværendeÅr, 12, 31) // Uendret

        // Skatteforholdet skal være avkortet til å starte 1. januar i inneværende år
        replisertPeriode.grunnlagSkatteforholdTilNorge?.fomDato shouldBe LocalDate.of(inneværendeÅr, 1, 1) // Avkortet
        replisertPeriode.grunnlagSkatteforholdTilNorge?.tomDato shouldBe LocalDate.of(inneværendeÅr, 6, 30)  // Uendret
    }

    @Test
    fun `replikerTrygdeavgift med toggle PÅ - filtrerer og avkorter for vanlige medlemmer`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode som trygdeavgiftsperiodene skal knyttes til
        val medlemskapsperiode = Medlemskapsperiode().apply {
            id = 1L
            fom = LocalDate.of(inneværendeÅr - 1, 1, 1)
            tom = LocalDate.of(inneværendeÅr + 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        // Opprett inntektsperiode som overlapper med inneværende år
        val inntektsperiode = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 6, 1)
            tomDato = LocalDate.of(inneværendeÅr, 6, 30)
        }

        // Opprett skatteforhold som overlapper med inneværende år
        val skatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 3, 1)
            tomDato = LocalDate.of(inneværendeÅr, 12, 31)
        }

        // Trygdeavgiftsperiode som starter før og slutter i inneværende år
        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr, 6, 30),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode1)

        // Trygdeavgiftsperiode som slutter før inneværende år (skal filtreres bort)
        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 2, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        val trygdeavgiftsperiode3 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(inneværendeÅr - 1, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode2)
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode3)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        // Kun periode som overlapper med inneværende år skal replikeres
        behandlingsresultatReplika.trygdeavgiftsperioder shouldHaveSize 1
        val replisertPeriode = behandlingsresultatReplika.trygdeavgiftsperioder.first()

        // Periode skal være avkortet til 1. januar
        replisertPeriode.periodeFra shouldBe LocalDate.of(inneværendeÅr, 1, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(inneværendeÅr, 6, 30)

        // Inntektsperiode skal også være avkortet
        replisertPeriode.grunnlagInntekstperiode?.fomDato shouldBe LocalDate.of(inneværendeÅr, 1, 1)
        replisertPeriode.grunnlagInntekstperiode?.tomDato shouldBe LocalDate.of(inneværendeÅr, 6, 30)

        // Skatteforhold skal også være avkortet
        replisertPeriode.grunnlagSkatteforholdTilNorge?.fomDato shouldBe LocalDate.of(inneværendeÅr, 1, 1)
        replisertPeriode.grunnlagSkatteforholdTilNorge?.tomDato shouldBe LocalDate.of(inneværendeÅr, 12, 31)
    }

    @Test
    fun `replikerTrygdeavgift med toggle AV - ingen filtrering eller avkorting`() {
        unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode
        val medlemskapsperiode = Medlemskapsperiode().apply {
            id = 1L
            fom = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tom = LocalDate.of(inneværendeÅr - 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        // Opprett skatteforhold (påkrevd)
        val skatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
        }

        // Trygdeavgiftsperiode som slutter før inneværende år
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        // Med toggle AV skal alle perioder replikeres uendret
        behandlingsresultatReplika.trygdeavgiftsperioder shouldHaveSize 1
        val replisertPeriode = behandlingsresultatReplika.trygdeavgiftsperioder.first()

        // Perioden skal være uendret
        replisertPeriode.periodeFra shouldBe LocalDate.of(inneværendeÅr - 2, 1, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(inneværendeÅr - 1, 12, 31)
    }

    @Test
    fun `replikerTrygdeavgiftForPensjonist med toggle AV - ingen filtrering eller avkorting`() {
        unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = opprettHelseutgiftDekkesPeriode(behandlingsresultatOriginal)

        // Opprett skatteforhold (påkrevd for pensjonist også)
        val skatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
        }

        // Trygdeavgiftsperiode som slutter før inneværende år
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        helseutgiftDekkesPeriode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        behandlingsresultatOriginal.helseutgiftDekkesPeriode = helseutgiftDekkesPeriode

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        // Med toggle AV skal alle perioder replikeres uendret
        behandlingsresultatReplika.helseutgiftDekkesPeriode!!.trygdeavgiftsperioder shouldHaveSize 1
        val replisertPeriode = behandlingsresultatReplika.helseutgiftDekkesPeriode!!.trygdeavgiftsperioder.first()

        // Perioden skal være uendret
        replisertPeriode.periodeFra shouldBe LocalDate.of(inneværendeÅr - 2, 1, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(inneværendeÅr - 1, 12, 31)
    }

    @Test
    fun `filtrerTrygdeavgiftsperioder med toggle AV - returnerer alle perioder uendret`() {
        unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val periode1 = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8")
        )

        val periode2 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(inneværendeÅr, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2")
        )

        val resultat = replikerBehandlingsresultatService.filtrerTrygdeavgiftsperioder(listOf(periode1, periode2))

        resultat shouldHaveSize 2
        resultat[0].periodeFra shouldBe LocalDate.of(inneværendeÅr - 2, 1, 1)
        resultat[0].periodeTil shouldBe LocalDate.of(inneværendeÅr - 1, 12, 31)
        resultat[1].periodeFra shouldBe LocalDate.of(inneværendeÅr, 1, 1)
        resultat[1].periodeTil shouldBe LocalDate.of(inneværendeÅr, 12, 31)
    }

    @Test
    fun `replikerTrygdeavgift med toggle PÅ - håndterer periode som starter og slutter i inneværende år korrekt`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode og skatteforhold
        val medlemskapsperiode = Medlemskapsperiode().apply {
            id = 1L
            fom = LocalDate.of(inneværendeÅr, 1, 1)
            tom = LocalDate.of(inneværendeÅr, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        val skatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr, 12, 31)
        }

        // Trygdeavgiftsperiode som starter og slutter i inneværende år (skal ikke avkortes)
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr, 3, 1),
            periodeTil = LocalDate.of(inneværendeÅr, 9, 30),
            trygdeavgiftsbeløpMd = Penger(1500.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        behandlingsresultatReplika.trygdeavgiftsperioder shouldHaveSize 1
        val replisertPeriode = behandlingsresultatReplika.trygdeavgiftsperioder.first()

        // Periode som allerede starter i inneværende år skal ikke avkortes
        replisertPeriode.periodeFra shouldBe LocalDate.of(inneværendeÅr, 3, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(inneværendeÅr, 9, 30)
    }

    @Test
    fun `replikerTrygdeavgift med toggle PÅ - håndterer periode som går over flere år fremover`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode og skatteforhold
        val medlemskapsperiode = Medlemskapsperiode().apply {
            id = 1L
            fom = LocalDate.of(inneværendeÅr - 1, 1, 1)
            tom = LocalDate.of(inneværendeÅr + 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        val skatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 1, 6, 1)
            tomDato = LocalDate.of(inneværendeÅr + 1, 12, 31)
        }

        // Trygdeavgiftsperiode som går fra forrige år til neste år
        val trygdeavgiftsperiodeIFjor = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(inneværendeÅr - 1, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        // Trygdeavgiftsperiode som går fra forrige år til neste år
        val trygdeavgiftsperiodeIÅr = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(inneværendeÅr, 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        val trygdeavgiftsperiodeNesteÅr = Trygdeavgiftsperiode(
            id = 3L,
            periodeFra = LocalDate.of(inneværendeÅr+1 , 1, 1),
            periodeTil = LocalDate.of(inneværendeÅr+1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeIFjor)
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeIÅr)
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeNesteÅr)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        behandlingsresultatReplika.trygdeavgiftsperioder shouldHaveSize 2
        val replisertPeriodeIÅr = behandlingsresultatReplika.trygdeavgiftsperioder.minBy { b -> b.periodeFra }
        val replisertPeriodeNesteÅr = behandlingsresultatReplika.trygdeavgiftsperioder.maxBy { b -> b.periodeFra }

        // Periode skal avkortes til å starte 1. januar, men slutt-dato skal være uendret
        replisertPeriodeIÅr.periodeFra shouldBe LocalDate.of(inneværendeÅr, 1, 1)
        replisertPeriodeIÅr.periodeTil shouldBe LocalDate.of(inneværendeÅr, 12, 31)

        replisertPeriodeNesteÅr.periodeFra shouldBe LocalDate.of(inneværendeÅr + 1, 1, 1)
        replisertPeriodeNesteÅr.periodeTil shouldBe LocalDate.of(inneværendeÅr + 1, 12, 31)
    }


    @Test
    fun `replikerLovvalgsperioder - trygdeavgiftsperioder collection er ikke delt mellom original og replika`() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val lovvalgsperiodeOriginal = opprettLovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
        }
        behandlingsresultatOriginal.lovvalgsperioder.add(lovvalgsperiodeOriginal)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured
        val lovvalgsperiodeReplika = behandlingsresultatReplika.lovvalgsperioder.first()

        // Verifiser at trygdeavgiftsperioder-samlingen IKKE er samme instans (shared reference bug fix)
        (lovvalgsperiodeReplika.trygdeavgiftsperioder !== lovvalgsperiodeOriginal.trygdeavgiftsperioder) shouldBe true

        // Verifiser at endringer i replika ikke påvirker original
        lovvalgsperiodeReplika.trygdeavgiftsperioder.clear()
        lovvalgsperiodeOriginal.trygdeavgiftsperioder shouldHaveSize 0 // Original var tom fra start

        // Verifiser at vi kan legge til i replika uten å påvirke original
        val nyTrygdeavgiftsperiode = lagTrygdeavgiftsperiode()
        lovvalgsperiodeReplika.trygdeavgiftsperioder.add(nyTrygdeavgiftsperiode)
        lovvalgsperiodeReplika.trygdeavgiftsperioder shouldHaveSize 1
        lovvalgsperiodeOriginal.trygdeavgiftsperioder shouldHaveSize 0
    }

    @Test
    fun `replikerMedlemskapsperioderBasertPåBehandlingstype inkluderer opphørt for MANGLENDE_INNBETALING_TRYGDEAVGIFT`() {
        val inneværendeÅr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Legg til både innvilget og opphørt medlemskapsperiode
        val innvilgetPeriode = Medlemskapsperiode().apply {
            id = 1L
            fom = LocalDate.of(inneværendeÅr, 1, 1)
            tom = LocalDate.of(inneværendeÅr, 6, 30)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }

        val opphørtPeriode = Medlemskapsperiode().apply {
            id = 2L
            fom = LocalDate.of(inneværendeÅr, 7, 1)
            tom = LocalDate.of(inneværendeÅr, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
        }

        val avslåttPeriode = Medlemskapsperiode().apply {
            id = 3L
            fom = LocalDate.of(inneværendeÅr - 1, 1, 1)
            tom = LocalDate.of(inneværendeÅr - 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }

        behandlingsresultatOriginal.medlemskapsperioder.addAll(setOf(innvilgetPeriode, opphørtPeriode, avslåttPeriode))

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0

        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)

        val behandlingsresultatReplika = slot.captured

        // For MANGLENDE_INNBETALING_TRYGDEAVGIFT skal både innvilget og opphørt replikeres, men ikke avslått
        behandlingsresultatReplika.medlemskapsperioder shouldHaveSize 2

        val replikerteStatuser = behandlingsresultatReplika.medlemskapsperioder
            .map { it.innvilgelsesresultat }
            .toSet()

        replikerteStatuser shouldContain InnvilgelsesResultat.INNVILGET
        replikerteStatuser shouldContain InnvilgelsesResultat.OPPHØRT
        replikerteStatuser shouldNotContain InnvilgelsesResultat.AVSLAATT
    }

    private fun Trygdeavgiftsperiode.erReplikaAv(original: Trygdeavgiftsperiode): Boolean =
        id == null &&
            periodeFra == original.periodeFra &&
            periodeTil == original.periodeTil &&
            trygdeavgiftsbeløpMd == original.trygdeavgiftsbeløpMd &&
            trygdesats == original.trygdesats &&
            grunnlagMedlemskapsperiode.erReplikaAv(original.grunnlagMedlemskapsperiode) &&
            grunnlagInntekstperiode.erReplikaAv(original.grunnlagInntekstperiode) &&
            grunnlagSkatteforholdTilNorge.erReplikaAv(original.grunnlagSkatteforholdTilNorge)

    private fun Medlemskapsperiode?.erReplikaAv(original: Medlemskapsperiode?): Boolean =
        this?.id == null && this?.trygdedekning == original?.trygdedekning

    private fun Inntektsperiode?.erReplikaAv(original: Inntektsperiode?): Boolean =
        this?.id == null &&
            this?.fomDato == original?.fomDato &&
            this?.tomDato == original?.tomDato &&
            this?.type == original?.type &&
            this?.avgiftspliktigMndInntekt == original?.avgiftspliktigMndInntekt &&
            this?.isArbeidsgiversavgiftBetalesTilSkatt == original?.isArbeidsgiversavgiftBetalesTilSkatt

    private fun SkatteforholdTilNorge?.erReplikaAv(original: SkatteforholdTilNorge?): Boolean =
        this?.id == null &&
            this?.fomDato == original?.fomDato &&
            this?.tomDato == original?.tomDato &&
            this?.skatteplikttype == original?.skatteplikttype
}
