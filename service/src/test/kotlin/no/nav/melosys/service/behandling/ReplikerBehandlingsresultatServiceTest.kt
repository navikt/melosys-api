package no.nav.melosys.service.behandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ReplikerBehandlingsresultatServiceTest {
    private lateinit var behandlingsresultatOrig: Behandlingsresultat
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
        val tidligsteInaktiveBehandling = Behandling()
        tidligsteInaktiveBehandling.id = 1L
        behandlingsresultatOrig = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)
        val avklartefaktaOrig = opprettAvklartefakta()
        behandlingsresultatOrig.avklartefakta.add(avklartefaktaOrig)
        val vilkaarsresultatOrig = opprettVilkaarsresultat()
        behandlingsresultatOrig.vilkaarsresultater.add(vilkaarsresultatOrig)
        val lovvalgsperiodeOrig = opprettLovvalgsperiode()
        behandlingsresultatOrig.lovvalgsperioder.add(lovvalgsperiodeOrig)
        behandlingsresultatOrig.behandlingsresultatBegrunnelser.add(opprettBehandlingsresultatBegrunnelse())
        behandlingsresultatOrig.kontrollresultater.add(opprettKontrollresultat())
        val anmodningsperiodeOrig = opprettAnmodningsperiode()
        behandlingsresultatOrig.anmodningsperioder.add(anmodningsperiodeOrig)
        val utpekingsperiodeOrig = opprettUtpekingsperiode()
        behandlingsresultatOrig.utpekingsperioder.add(utpekingsperiodeOrig)
        val medlemAvFolketrygdenOrig = opprettMedlemAvFolketrygden()
        behandlingsresultatOrig.medlemAvFolketrygden = medlemAvFolketrygdenOrig

        val behandlingReplika = Behandling()
        behandlingReplika.id = 2L
        behandlingReplika.type = Behandlingstyper.NY_VURDERING

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOrig
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured

        Assertions.assertThat(behandlingsresultatReplika)
            .matches { it.behandling == behandlingReplika }
            .matches { it.id == null }
            .matches { it.behandlingsmåte == behandlingsresultatOrig.behandlingsmåte }
            .matches { it.type == Behandlingsresultattyper.IKKE_FASTSATT }
            .matches { it.vedtakMetadata == null }

        Assertions.assertThat(behandlingsresultatReplika.lovvalgsperioder)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.fom == lovvalgsperiodeOrig.fom }
            .matches { it.tom == lovvalgsperiodeOrig.tom }
            .matches { it.medlPeriodeID == lovvalgsperiodeOrig.medlPeriodeID }
            .matches { it.dekning == lovvalgsperiodeOrig.dekning }

        Assertions.assertThat(behandlingsresultatReplika.anmodningsperioder)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.medlPeriodeID == null }
            .matches { !it.erSendtUtland() }
            .matches { it.anmodningsperiodeSvar == null }
            .matches { it.fom == anmodningsperiodeOrig.fom }
            .matches { it.tom == anmodningsperiodeOrig.tom }
            .matches { it.lovvalgsland == anmodningsperiodeOrig.lovvalgsland }
            .matches { it.bestemmelse === anmodningsperiodeOrig.bestemmelse }
            .matches { it.dekning == anmodningsperiodeOrig.dekning }

        Assertions.assertThat(behandlingsresultatReplika.utpekingsperioder)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.medlPeriodeID == null }
            .matches { it.sendtUtland == null }
            .matches { it.fom == utpekingsperiodeOrig.fom }
            .matches { it.tom == utpekingsperiodeOrig.tom }
            .matches { it.lovvalgsland == utpekingsperiodeOrig.lovvalgsland }
            .matches { it.bestemmelse === utpekingsperiodeOrig.bestemmelse }

        Assertions.assertThat(behandlingsresultatReplika.avklartefakta)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.fakta == avklartefaktaOrig.fakta }
            .matches { it.type == avklartefaktaOrig.type }
        Assertions.assertThat(behandlingsresultatReplika.avklartefakta.first().registreringer)
            .singleElement()
            .matches { it.avklartefakta == behandlingsresultatReplika.avklartefakta.first() }
            .matches { it.id == null }
            .matches { it.begrunnelseKode == avklartefaktaOrig.registreringer.first().begrunnelseKode }

        Assertions.assertThat(behandlingsresultatReplika.vilkaarsresultater)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.begrunnelseFritekst == vilkaarsresultatOrig.begrunnelseFritekst }
            .matches { it.begrunnelseFritekstEessi == vilkaarsresultatOrig.begrunnelseFritekstEessi }
        Assertions.assertThat(behandlingsresultatReplika.vilkaarsresultater.first().begrunnelser)
            .singleElement()
            .matches { it.vilkaarsresultat == behandlingsresultatReplika.vilkaarsresultater.first() }
            .matches { it.id == null }
            .matches { it.kode == vilkaarsresultatOrig.begrunnelser.first().kode }

        Assertions.assertThat(behandlingsresultatReplika.behandlingsresultatBegrunnelser)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.kode == behandlingsresultatOrig.behandlingsresultatBegrunnelser.first().kode }

        Assertions.assertThat(behandlingsresultatReplika.kontrollresultater)
            .singleElement()
            .matches { it.behandlingsresultat === behandlingsresultatReplika }
            .matches { it.id == null }
            .matches { it.begrunnelse == behandlingsresultatOrig.kontrollresultater.first().begrunnelse }

        Assertions.assertThat(behandlingsresultatReplika.utfallRegistreringUnntak).isNull()

        Assertions.assertThat(behandlingsresultatReplika.utfallUtpeking).isNull()

        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden)
            .matches { it.behandlingsresultat == behandlingsresultatReplika }
            .matches { it.id == null }

        Assertions.assertThat(medlemAvFolketrygdenOrig.medlemskapsperioder).hasSize(3)
        val innvilgetMedlemskapsperiodeOrig = medlemAvFolketrygdenOrig.medlemskapsperioder.filter { it.erInnvilget() }.first()
        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden.medlemskapsperioder)
            .singleElement()
            .matches { it.medlemAvFolketrygden == behandlingsresultatReplika.medlemAvFolketrygden }
            .matches { it.id == null }
            .matches { it.fom == innvilgetMedlemskapsperiodeOrig.fom }
            .matches { it.tom == innvilgetMedlemskapsperiodeOrig.tom }
            .matches { it.arbeidsland == innvilgetMedlemskapsperiodeOrig.arbeidsland }
            .matches { it.medlemskapstype == innvilgetMedlemskapsperiodeOrig.medlemskapstype }
            .matches { it.innvilgelsesresultat == innvilgetMedlemskapsperiodeOrig.innvilgelsesresultat }
            .matches { it.trygdedekning == innvilgetMedlemskapsperiodeOrig.trygdedekning }
            .matches { it.medlPeriodeID == innvilgetMedlemskapsperiodeOrig.medlPeriodeID }
            .matches { it.bestemmelse == innvilgetMedlemskapsperiodeOrig.bestemmelse }

        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift)
            .matches { it.medlemAvFolketrygden == behandlingsresultatReplika.medlemAvFolketrygden }
            .matches { it.id == null }
            .matches { it.trygdeavgiftstype == behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftstype }

        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag)
            .matches { it.fastsattTrygdeavgift == behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift }
            .matches { it.id == null }

        val inntektsperiodeOrig =
            medlemAvFolketrygdenOrig.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.first()
        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder)
            .singleElement()
            .matches { it.trygdeavgiftsgrunnlag == behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag }
            .matches { it.id == null }
            .matches { it.fomDato == inntektsperiodeOrig.fomDato }
            .matches { it.tomDato == inntektsperiodeOrig.tomDato }
            .matches { it.type == inntektsperiodeOrig.type }
            .matches { it.avgiftspliktigInntektMnd == inntektsperiodeOrig.avgiftspliktigInntektMnd }
            .matches { it.isArbeidsgiversavgiftBetalesTilSkatt == inntektsperiodeOrig.isArbeidsgiversavgiftBetalesTilSkatt }

        val skatteforholdTilNorgeOrig =
            medlemAvFolketrygdenOrig.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.first()
        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge)
            .singleElement()
            .matches { it.trygdeavgiftsgrunnlag == behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag }
            .matches { it.id == null }
            .matches { it.fomDato == skatteforholdTilNorgeOrig.fomDato }
            .matches { it.tomDato == skatteforholdTilNorgeOrig.tomDato }
            .matches { it.skatteplikttype == skatteforholdTilNorgeOrig.skatteplikttype }

        val trygdeavgiftsperiodeOrig = medlemAvFolketrygdenOrig.fastsattTrygdeavgift.trygdeavgiftsperioder.first()
        Assertions.assertThat(behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder)
            .singleElement()
            .matches { it.fastsattTrygdeavgift == behandlingsresultatReplika.medlemAvFolketrygden.fastsattTrygdeavgift }
            .matches { it.id == null }
            .matches { it.periodeFra == trygdeavgiftsperiodeOrig.periodeFra }
            .matches { it.periodeTil == trygdeavgiftsperiodeOrig.periodeTil }
            .matches { it.trygdeavgiftsbeløpMd == trygdeavgiftsperiodeOrig.trygdeavgiftsbeløpMd }
            .matches { it.trygdesats == trygdeavgiftsperiodeOrig.trygdesats }
            .matches { it.grunnlagMedlemskapsperiode.id == null }
            .matches { it.grunnlagMedlemskapsperiode.trygdedekning == innvilgetMedlemskapsperiodeOrig.trygdedekning }
            .matches { it.grunnlagInntekstperiode.id == null }
            .matches { it.grunnlagInntekstperiode.avgiftspliktigInntektMnd == inntektsperiodeOrig.avgiftspliktigInntektMnd }
            .matches { it.grunnlagSkatteforholdTilNorge.id == null }
            .matches { it.grunnlagSkatteforholdTilNorge.skatteplikttype == skatteforholdTilNorgeOrig.skatteplikttype }
    }

    @Test
    @Throws(
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class,
        InvocationTargetException::class
    )
    fun replikerBehandlingOgBehandlingsresultat_manglendeInnbetalingTrygdeavgift_replikererBehandlingsresultatObjekterOgCollections() {
        val tidligsteInaktiveBehandling = Behandling()
        tidligsteInaktiveBehandling.id = 1L
        behandlingsresultatOrig = opprettBehandlingsresultatMedData(tidligsteInaktiveBehandling)
        val avklartefaktaOrig = opprettAvklartefakta()
        behandlingsresultatOrig.avklartefakta.add(avklartefaktaOrig)
        val vilkaarsresultatOrig = opprettVilkaarsresultat()
        behandlingsresultatOrig.vilkaarsresultater.add(vilkaarsresultatOrig)
        val lovvalgsperiodeOrig = opprettLovvalgsperiode()
        behandlingsresultatOrig.lovvalgsperioder.add(lovvalgsperiodeOrig)
        behandlingsresultatOrig.behandlingsresultatBegrunnelser.add(opprettBehandlingsresultatBegrunnelse())
        behandlingsresultatOrig.kontrollresultater.add(opprettKontrollresultat())
        val anmodningsperiodeOrig = opprettAnmodningsperiode()
        behandlingsresultatOrig.anmodningsperioder.add(anmodningsperiodeOrig)
        val utpekingsperiodeOrig = opprettUtpekingsperiode()
        behandlingsresultatOrig.utpekingsperioder.add(utpekingsperiodeOrig)
        val medlemAvFolketrygdenOrig = opprettMedlemAvFolketrygden()
        behandlingsresultatOrig.medlemAvFolketrygden = medlemAvFolketrygdenOrig

        val behandlingReplika = Behandling()
        behandlingReplika.id = 2L
        behandlingReplika.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOrig
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured
        medlemAvFolketrygdenOrig.medlemskapsperioder.shouldHaveSize(3)
        val innvilgetMedlemskapsperiodeOrig = medlemAvFolketrygdenOrig.medlemskapsperioder.filter { it.erInnvilget() }.first()
        val opphørtMedlemskapsperiodeOrig = medlemAvFolketrygdenOrig.medlemskapsperioder.filter { it.erOpphørt() }.first()
        behandlingsresultatReplika.medlemAvFolketrygden.medlemskapsperioder
            .shouldHaveSize(2)
            .sortedBy { it.innvilgelsesresultat }
            .run {
                first().run {
                    medlemAvFolketrygden.shouldBe(behandlingsresultatReplika.medlemAvFolketrygden)
                    id.shouldBe(null)
                    fom.shouldBe(innvilgetMedlemskapsperiodeOrig.fom)
                    tom.shouldBe(innvilgetMedlemskapsperiodeOrig.tom)
                    arbeidsland.shouldBe(innvilgetMedlemskapsperiodeOrig.arbeidsland)
                    medlemskapstype.shouldBe(innvilgetMedlemskapsperiodeOrig.medlemskapstype)
                    innvilgelsesresultat.shouldBe(innvilgetMedlemskapsperiodeOrig.innvilgelsesresultat)
                    trygdedekning.shouldBe(innvilgetMedlemskapsperiodeOrig.trygdedekning)
                    medlPeriodeID.shouldBe(innvilgetMedlemskapsperiodeOrig.medlPeriodeID)
                    bestemmelse.shouldBe(innvilgetMedlemskapsperiodeOrig.bestemmelse)
                }
                last().run {
                    medlemAvFolketrygden.shouldBe(behandlingsresultatReplika.medlemAvFolketrygden)
                    id.shouldBe(null)
                    fom.shouldBe(opphørtMedlemskapsperiodeOrig.fom)
                    tom.shouldBe(opphørtMedlemskapsperiodeOrig.tom)
                    arbeidsland.shouldBe(opphørtMedlemskapsperiodeOrig.arbeidsland)
                    medlemskapstype.shouldBe(opphørtMedlemskapsperiodeOrig.medlemskapstype)
                    innvilgelsesresultat.shouldBe(opphørtMedlemskapsperiodeOrig.innvilgelsesresultat)
                    trygdedekning.shouldBe(opphørtMedlemskapsperiodeOrig.trygdedekning)
                    medlPeriodeID.shouldBe(opphørtMedlemskapsperiodeOrig.medlPeriodeID)
                    bestemmelse.shouldBe(opphørtMedlemskapsperiodeOrig.bestemmelse)
                }
            }
    }

    private fun opprettMedlemAvFolketrygden(): MedlemAvFolketrygden {
        val medlemAvFolketrygden = MedlemAvFolketrygden()
        medlemAvFolketrygden.behandlingsresultat = behandlingsresultatOrig
        medlemAvFolketrygden.id = 30L
        medlemAvFolketrygden.addMedlemskapsperiode(opprettMedlemskapsperiode(InnvilgelsesResultat.INNVILGET))
        medlemAvFolketrygden.addMedlemskapsperiode(opprettMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT))
        medlemAvFolketrygden.addMedlemskapsperiode(opprettMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT))
        medlemAvFolketrygden.fastsattTrygdeavgift = opprettFastsattTrygdeavgift(medlemAvFolketrygden)
        return medlemAvFolketrygden
    }

    private fun opprettFastsattTrygdeavgift(medlemAvFolketrygden: MedlemAvFolketrygden): FastsattTrygdeavgift {
        val fastsattTrygdeavgift = FastsattTrygdeavgift()
        fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
        fastsattTrygdeavgift.id = 34L
        fastsattTrygdeavgift.trygdeavgiftstype = Trygdeavgift_typer.FORELØPIG
        fastsattTrygdeavgift.trygdeavgiftsgrunnlag = opprettTrygdeavgiftsgrunnlag(fastsattTrygdeavgift)
        fastsattTrygdeavgift.trygdeavgiftsperioder = opprettTrygdeavgiftsperioder(fastsattTrygdeavgift)
        return fastsattTrygdeavgift
    }

    private fun opprettTrygdeavgiftsperioder(fastsattTrygdeavgift: FastsattTrygdeavgift): Set<Trygdeavgiftsperiode> {
        return setOf(
            Trygdeavgiftsperiode().apply {
                id = 1L
                this.fastsattTrygdeavgift = fastsattTrygdeavgift
                periodeFra = LocalDate.now()
                periodeTil = LocalDate.now()
                trygdeavgiftsbeløpMd = Penger(500.0)
                trygdesats = BigDecimal(50)
                grunnlagInntekstperiode = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.first()
                grunnlagSkatteforholdTilNorge = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.first()
                grunnlagMedlemskapsperiode = fastsattTrygdeavgift.medlemAvFolketrygden.medlemskapsperioder.first()
            }
        )
    }

    private fun opprettTrygdeavgiftsgrunnlag(fastsattTrygdeavgift: FastsattTrygdeavgift): Trygdeavgiftsgrunnlag {
        val trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag()
        trygdeavgiftsgrunnlag.fastsattTrygdeavgift = fastsattTrygdeavgift
        trygdeavgiftsgrunnlag.id = 35L
        trygdeavgiftsgrunnlag.inntektsperioder = mutableListOf(
            Inntektsperiode().apply {
                id = 1L
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigInntektMnd = Penger(1000.0)
                isArbeidsgiversavgiftBetalesTilSkatt = false
            })
        trygdeavgiftsgrunnlag.skatteforholdTilNorge = mutableListOf(
            SkatteforholdTilNorge().apply {
                id = 1L
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            })
        return trygdeavgiftsgrunnlag
    }

    private fun opprettMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat): Medlemskapsperiode {
        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.id = 1L
        medlemskapsperiode.innvilgelsesresultat = innvilgelsesResultat
        medlemskapsperiode.medlPeriodeID = 77L
        medlemskapsperiode.fom = LocalDate.now()
        medlemskapsperiode.tom = LocalDate.now()
        medlemskapsperiode.arbeidsland = "YO"
        medlemskapsperiode.medlemskapstype = Medlemskapstyper.PLIKTIG
        medlemskapsperiode.trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        medlemskapsperiode.medlPeriodeID = 123L
        medlemskapsperiode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        return medlemskapsperiode
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
        lovvalgsperiode.behandlingsresultat = behandlingsresultatOrig
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
        anmodningsperiode.behandlingsresultat = behandlingsresultatOrig
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
        avklartefakta.behandlingsresultat = behandlingsresultatOrig
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
        behandlingsresultatBegrunnelse.behandlingsresultat = behandlingsresultatOrig
        behandlingsresultatBegrunnelse.kode = "begrunnelsekode"
        return behandlingsresultatBegrunnelse
    }

    private fun opprettVilkaarsresultat(): Vilkaarsresultat {
        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.behandlingsresultat = behandlingsresultatOrig
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
        kontrollresultat.behandlingsresultat = behandlingsresultatOrig
        kontrollresultat.begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
        return kontrollresultat
    }
}
