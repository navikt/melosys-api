package no.nav.melosys.service.behandling

import io.kotest.matchers.collections.shouldHaveSize
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
import org.assertj.core.api.Assertions
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

    private lateinit var replikerBehandlingsresultatService: ReplikerBehandlingsresultatService

    @BeforeEach
    fun setup() {
        replikerBehandlingsresultatService =
            ReplikerBehandlingsresultatService(behandlingsresultatService)
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
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured

        Assertions.assertThat(behandlingsresultatReplika)
            .matches { it.behandling == behandlingReplika }
            .matches { it.id == null }
            .matches { it.behandlingsmåte == behandlingsresultatOriginal.behandlingsmåte }
            .matches { it.type == Behandlingsresultattyper.IKKE_FASTSATT }
            .matches { it.vedtakMetadata == null }

        Assertions.assertThat(behandlingsresultatReplika.lovvalgsperioder)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.fom == lovvalgsperiodeOriginal.fom }
            .matches { it.tom == lovvalgsperiodeOriginal.tom }
            .matches { it.medlPeriodeID == lovvalgsperiodeOriginal.medlPeriodeID }
            .matches { it.dekning == lovvalgsperiodeOriginal.dekning }

        Assertions.assertThat(behandlingsresultatReplika.anmodningsperioder)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.medlPeriodeID == null }
            .matches { !it.erSendtUtland() }
            .matches { it.anmodningsperiodeSvar == null }
            .matches { it.fom == anmodningsperiodeOriginal.fom }
            .matches { it.tom == anmodningsperiodeOriginal.tom }
            .matches { it.lovvalgsland == anmodningsperiodeOriginal.lovvalgsland }
            .matches { it.bestemmelse === anmodningsperiodeOriginal.bestemmelse }
            .matches { it.dekning == anmodningsperiodeOriginal.dekning }

        Assertions.assertThat(behandlingsresultatReplika.utpekingsperioder)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.medlPeriodeID == null }
            .matches { it.sendtUtland == null }
            .matches { it.fom == utpekingsperiodeOriginal.fom }
            .matches { it.tom == utpekingsperiodeOriginal.tom }
            .matches { it.lovvalgsland == utpekingsperiodeOriginal.lovvalgsland }
            .matches { it.bestemmelse === utpekingsperiodeOriginal.bestemmelse }

        Assertions.assertThat(behandlingsresultatReplika.avklartefakta)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.fakta == avklartefaktaOriginal.fakta }
            .matches { it.type == avklartefaktaOriginal.type }
        Assertions.assertThat(behandlingsresultatReplika.avklartefakta.first().registreringer)
            .singleElement()
            .matches { it.avklartefakta == behandlingsresultatReplika.avklartefakta.first() }
            .matches { it.id == null }
            .matches { it.begrunnelseKode == avklartefaktaOriginal.registreringer.first().begrunnelseKode }

        Assertions.assertThat(behandlingsresultatReplika.vilkaarsresultater)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.begrunnelseFritekst == vilkaarsresultatOriginal.begrunnelseFritekst }
            .matches { it.begrunnelseFritekstEessi == vilkaarsresultatOriginal.begrunnelseFritekstEessi }
        Assertions.assertThat(behandlingsresultatReplika.vilkaarsresultater.first().begrunnelser)
            .singleElement()
            .matches { it.vilkaarsresultat == behandlingsresultatReplika.vilkaarsresultater.first() }
            .matches { it.id == null }
            .matches { it.kode == vilkaarsresultatOriginal.begrunnelser.first().kode }

        Assertions.assertThat(behandlingsresultatReplika.behandlingsresultatBegrunnelser)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.kode == behandlingsresultatOriginal.behandlingsresultatBegrunnelser.first().kode }

        Assertions.assertThat(behandlingsresultatReplika.kontrollresultater)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.begrunnelse == behandlingsresultatOriginal.kontrollresultater.first().begrunnelse }

        Assertions.assertThat(behandlingsresultatReplika.utfallRegistreringUnntak).isNull()

        Assertions.assertThat(behandlingsresultatReplika.utfallUtpeking).isNull()

        Assertions.assertThat(behandlingsresultatReplika)
            .matches { it == behandlingsresultatReplika }
            .matches { it.id == null }

        Assertions.assertThat(behandlingsresultatOriginal.medlemskapsperioder).hasSize(3)
        val innvilgetMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.first { it.erInnvilget() }
        Assertions.assertThat(behandlingsresultatReplika.medlemskapsperioder)
            .singleElement()
            .matches { it.behandlingsresultat == behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.fom == innvilgetMedlemskapsperiodeOriginal.fom }
            .matches { it.tom == innvilgetMedlemskapsperiodeOriginal.tom }
            .matches { it.medlemskapstype == innvilgetMedlemskapsperiodeOriginal.medlemskapstype }
            .matches { it.innvilgelsesresultat == innvilgetMedlemskapsperiodeOriginal.innvilgelsesresultat }
            .matches { it.trygdedekning == innvilgetMedlemskapsperiodeOriginal.trygdedekning }
            .matches { it.medlPeriodeID == innvilgetMedlemskapsperiodeOriginal.medlPeriodeID }
            .matches { it.bestemmelse == innvilgetMedlemskapsperiodeOriginal.bestemmelse }

        Assertions.assertThat(behandlingsresultatReplika)
            .matches { it.id == null }
            .matches { it.trygdeavgiftType == behandlingsresultatReplika.trygdeavgiftType }

        Assertions.assertThat(behandlingsresultatReplika)
            .matches { it.id == null }


        val inntektsperioderOriginal = behandlingsresultatOriginal.hentInntektsperioder().toList()
        val inntektsperioderReplika = behandlingsresultatReplika.hentInntektsperioder().toList()

        inntektsperioderOriginal.forEach { originalInntektsperiode ->
            Assertions.assertThat(inntektsperioderReplika).anyMatch { replikertInntektsperiode ->
                replikertInntektsperiode.id == null &&
                    replikertInntektsperiode.fomDato == originalInntektsperiode.fomDato &&
                    replikertInntektsperiode.tomDato == originalInntektsperiode.tomDato &&
                    replikertInntektsperiode.type == originalInntektsperiode.type &&
                    replikertInntektsperiode.avgiftspliktigMndInntekt == originalInntektsperiode.avgiftspliktigMndInntekt &&
                    replikertInntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt == originalInntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt
            }.withFailMessage("Forventet matchende Inntektsperiode for: $originalInntektsperiode")
        }

        val skatteforholdTilNorgeOriginal =
            behandlingsresultatOriginal.hentSkatteforholdTilNorge().first()
        Assertions.assertThat(behandlingsresultatReplika.hentSkatteforholdTilNorge())
            .singleElement()
            .matches { it.id == null }
            .matches { it.fomDato == skatteforholdTilNorgeOriginal.fomDato }
            .matches { it.tomDato == skatteforholdTilNorgeOriginal.tomDato }
            .matches { it.skatteplikttype == skatteforholdTilNorgeOriginal.skatteplikttype }


        val trygdeavgiftsperioderOriginal = behandlingsresultatOriginal.trygdeavgiftsperioder.toList()
        val trygdeavgiftsperioderReplika = behandlingsresultatReplika.trygdeavgiftsperioder.toList()

        trygdeavgiftsperioderOriginal.forEach { originalTrygdeavgiftsperiode ->
            Assertions.assertThat(trygdeavgiftsperioderReplika).anyMatch { replikertTrygdeavgiftsperiode ->
                replikertTrygdeavgiftsperiode.id == null &&
                    replikertTrygdeavgiftsperiode.periodeFra == originalTrygdeavgiftsperiode.periodeFra &&
                    replikertTrygdeavgiftsperiode.periodeTil == originalTrygdeavgiftsperiode.periodeTil &&
                    replikertTrygdeavgiftsperiode.trygdeavgiftsbeløpMd == originalTrygdeavgiftsperiode.trygdeavgiftsbeløpMd &&
                    replikertTrygdeavgiftsperiode.trygdesats == originalTrygdeavgiftsperiode.trygdesats &&
                    replikertTrygdeavgiftsperiode.grunnlagMedlemskapsperiode?.id == null &&
                    replikertTrygdeavgiftsperiode.grunnlagMedlemskapsperiode?.trygdedekning == originalTrygdeavgiftsperiode.grunnlagMedlemskapsperiode?.trygdedekning &&
                    replikertTrygdeavgiftsperiode.grunnlagInntekstperiode?.id == null &&
                    replikertTrygdeavgiftsperiode.grunnlagInntekstperiode?.avgiftspliktigMndInntekt == originalTrygdeavgiftsperiode.grunnlagInntekstperiode?.avgiftspliktigMndInntekt &&
                    replikertTrygdeavgiftsperiode.grunnlagSkatteforholdTilNorge?.id == null &&
                    replikertTrygdeavgiftsperiode.grunnlagSkatteforholdTilNorge?.skatteplikttype == originalTrygdeavgiftsperiode.grunnlagSkatteforholdTilNorge?.skatteplikttype
            }.withFailMessage("Forventet matchende Trygdeavgiftsperiode for: $originalTrygdeavgiftsperiode")
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
    @Throws(
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )
    fun replikerBehandlingOgBehandlingsresultat_EØSPensjonist_replikererBehandlingsresultatObjekterOgCollections() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            tema = Behandlingstema.PENSJONIST
            fagsak?.type = Sakstyper.EU_EOS
            fagsak?.tema = Sakstemaer.TRYGDEAVGIFT
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
            fagsak?.type = Sakstyper.EU_EOS
            fagsak?.tema = Sakstemaer.TRYGDEAVGIFT
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured
        behandlingsresultatOriginal.helseutgiftDekkesPeriode.shouldNotBeNull()

        behandlingsresultatReplika.run {
            behandling.erEøsPensjonist() shouldBe true
            behandling.type shouldBe Behandlingstyper.NY_VURDERING
        }

        behandlingsresultatReplika.helseutgiftDekkesPeriode
            .run {
                behandlingsresultat shouldBe behandlingsresultatReplika
                trygdeavgiftsperioder shouldBe trygdeavgiftsperioder
                fomDato shouldBe behandlingsresultatOriginal.helseutgiftDekkesPeriode.fomDato
                tomDato shouldBe behandlingsresultatOriginal.helseutgiftDekkesPeriode.tomDato
                bostedLandkode shouldBe behandlingsresultatOriginal.helseutgiftDekkesPeriode.bostedLandkode
            }
    }

    @Test
    fun `replikering av behandlingsresultat - manglende skatteforholdsperiode kaster exception`() {
        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
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
        }
        behandlingsresultatOriginal = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val innvilgetMedlemskapsperiode = opprettMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val trygdeavgiftsperiode = lagTrygdeavgiftsperiode()

        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)

        val behandlingReplika = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
        }

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        every { behandlingsresultatService.lagre(any()) } returnsArgument 0

        assertThrows<IllegalStateException> {
            replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)
        }.message shouldBe "Medlemskapsperiode ikke funnet"
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

    private fun opprettLovvalgsperiode(): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.id = 32L
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
}
