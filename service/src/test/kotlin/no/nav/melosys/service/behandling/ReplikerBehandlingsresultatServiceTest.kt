package no.nav.melosys.service.behandling

import io.kotest.matchers.collections.shouldHaveSize
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
        val tidligsteInaktiveBehandling = Behandling()
        tidligsteInaktiveBehandling.id = 1L
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
            lagTrygdeavgiftsperiode().apply {
                grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode
            }
        )
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(avslaattMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(opphoertMedlemskapsperiode)
        behandlingsresultatOriginal.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG


        val behandlingReplika = Behandling()
        behandlingReplika.id = 2L
        behandlingReplika.type = Behandlingstyper.NY_VURDERING

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
        val innvilgetMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.filter { it.erInnvilget() }.first()
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

        val inntektsperiodeOriginal = behandlingsresultatOriginal.hentInntektsperioder().first()
        Assertions.assertThat(behandlingsresultatReplika.hentInntektsperioder())
            .singleElement()
            .matches { it.id == null }
            .matches { it.fomDato == inntektsperiodeOriginal.fomDato }
            .matches { it.tomDato == inntektsperiodeOriginal.tomDato }
            .matches { it.type == inntektsperiodeOriginal.type }
            .matches { it.avgiftspliktigInntektMnd == inntektsperiodeOriginal.avgiftspliktigInntektMnd }
            .matches { it.isArbeidsgiversavgiftBetalesTilSkatt == inntektsperiodeOriginal.isArbeidsgiversavgiftBetalesTilSkatt }

        val skatteforholdTilNorgeOriginal =
            behandlingsresultatOriginal.hentSkatteforholdTilNorge().first()
        Assertions.assertThat(behandlingsresultatReplika.hentSkatteforholdTilNorge())
            .singleElement()
            .matches { it.id == null }
            .matches { it.fomDato == skatteforholdTilNorgeOriginal.fomDato }
            .matches { it.tomDato == skatteforholdTilNorgeOriginal.tomDato }
            .matches { it.skatteplikttype == skatteforholdTilNorgeOriginal.skatteplikttype }

        val trygdeavgiftsperiodeOriginal = behandlingsresultatOriginal.trygdeavgiftsperioder.first()
        Assertions.assertThat(behandlingsresultatReplika.trygdeavgiftsperioder)
            .singleElement()
            .matches { it.id == null }
            .matches { it.periodeFra == trygdeavgiftsperiodeOriginal.periodeFra }
            .matches { it.periodeTil == trygdeavgiftsperiodeOriginal.periodeTil }
            .matches { it.trygdeavgiftsbeløpMd == trygdeavgiftsperiodeOriginal.trygdeavgiftsbeløpMd }
            .matches { it.trygdesats == trygdeavgiftsperiodeOriginal.trygdesats }
            .matches { it.grunnlagMedlemskapsperiode.id == null }
            .matches { it.grunnlagMedlemskapsperiode.trygdedekning == innvilgetMedlemskapsperiodeOriginal.trygdedekning }
            .matches { it.grunnlagInntekstperiode.id == null }
            .matches { it.grunnlagInntekstperiode.avgiftspliktigInntektMnd == inntektsperiodeOriginal.avgiftspliktigInntektMnd }
            .matches { it.grunnlagSkatteforholdTilNorge.id == null }
            .matches { it.grunnlagSkatteforholdTilNorge.skatteplikttype == skatteforholdTilNorgeOriginal.skatteplikttype }
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
            lagTrygdeavgiftsperiode().apply {
                grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode
            }
        )
        behandlingsresultatOriginal.addMedlemskapsperiode(innvilgetMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(avslaattMedlemskapsperiode)
        behandlingsresultatOriginal.addMedlemskapsperiode(opphoertMedlemskapsperiode)
        behandlingsresultatOriginal.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG

        val behandlingReplika = Behandling()
        behandlingReplika.id = 2L
        behandlingReplika.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT

        every { behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id) } returns behandlingsresultatOriginal
        val slot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(slot)) } returnsArgument 0


        replikerBehandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingReplika)


        val behandlingsresultatReplika = slot.captured
        behandlingsresultatOriginal.medlemskapsperioder.shouldHaveSize(3)
        val innvilgetMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.filter { it.erInnvilget() }.first()
        val opphørtMedlemskapsperiodeOriginal = behandlingsresultatOriginal.medlemskapsperioder.filter { it.erOpphørt() }.first()
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

    private fun lagTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        val inntektsperiode = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigInntektMnd = Penger(1000.0)
            isArbeidsgiversavgiftBetalesTilSkatt = false
        }
        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

          return  Trygdeavgiftsperiode().apply {
                id = 1L
                periodeFra = LocalDate.now()
                periodeTil = LocalDate.now()
                trygdeavgiftsbeløpMd = Penger(500.0)
                trygdesats = BigDecimal(50)
                grunnlagInntekstperiode = inntektsperiode
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge
            }
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
