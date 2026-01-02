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
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
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

        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val avslaattMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT, 2L)
        val opphoertMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT, 3L)

        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode)
        )
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode,
                grunnlagInntekstperiode = inntektForTest {
                    id = 2L
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(1000.0)
                    arbeidsgiversavgiftBetalesTilSkatt = false
                })
        )
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode,
                grunnlagInntekstperiode = null,
            )
        )

        val lovvalgsperiodeOriginal = lagLovvalgsperiode()
        val avklartefaktaOriginal = lagAvklartefakta()
        val vilkaarsresultatOriginal = lagVilkaarsresultat()
        val anmodningsperiodeOriginal = lagAnmodningsperiode()
        val utpekingsperiodeOriginal = lagUtpekingsperiode()

        val behandlingsresultatOriginal = Behandlingsresultat.forTest {
            id = 30L
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
            vedtakMetadata { vedtaksdato = Instant.parse("2002-02-11T09:37:30Z") }
            medExistingMedlemskapsperiode(innvilgetMedlemskapsperiode)
            medExistingMedlemskapsperiode(avslaattMedlemskapsperiode)
            medExistingMedlemskapsperiode(opphoertMedlemskapsperiode)
        }.also {
            it.avklartefakta.add(avklartefaktaOriginal)
            avklartefaktaOriginal.behandlingsresultat = it
            it.vilkaarsresultater.add(vilkaarsresultatOriginal)
            vilkaarsresultatOriginal.behandlingsresultat = it
            it.lovvalgsperioder.add(lovvalgsperiodeOriginal)
            lovvalgsperiodeOriginal.behandlingsresultat = it
            it.behandlingsresultatBegrunnelser.add(lagBehandlingsresultatBegrunnelse(it))
            it.kontrollresultater.add(lagKontrollresultat(it))
            it.anmodningsperioder.add(anmodningsperiodeOriginal)
            anmodningsperiodeOriginal.behandlingsresultat = it
            it.utpekingsperioder.add(utpekingsperiodeOriginal)
            utpekingsperiodeOriginal.behandlingsresultat = it
        }


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

        val innvilgetLovvalgsperiode = lagLovvalgsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val avslaattLovvalgsperiode = lagLovvalgsperiode(InnvilgelsesResultat.AVSLAATT, 2L)
        val opphoertLovvalgsperiode = lagLovvalgsperiode(InnvilgelsesResultat.OPPHØRT, 3L)

        innvilgetLovvalgsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(grunnlagLovvalgsPeriode = innvilgetLovvalgsperiode)
        )
        innvilgetLovvalgsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagLovvalgsPeriode = innvilgetLovvalgsperiode,
                grunnlagInntekstperiode = inntektForTest {
                    id = 2L
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(1000.0)
                    arbeidsgiversavgiftBetalesTilSkatt = false
                })
        )
        innvilgetLovvalgsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(
                grunnlagLovvalgsPeriode = innvilgetLovvalgsperiode,
                grunnlagInntekstperiode = null,
            )
        )

        val avklartefaktaOriginal = lagAvklartefakta()
        val vilkaarsresultatOriginal = lagVilkaarsresultat()
        val anmodningsperiodeOriginal = lagAnmodningsperiode()
        val utpekingsperiodeOriginal = lagUtpekingsperiode()

        val behandlingsresultatOriginal = Behandlingsresultat.forTest {
            id = 30L
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
            vedtakMetadata { vedtaksdato = Instant.parse("2002-02-11T09:37:30Z") }
        }.also {
            it.avklartefakta.add(avklartefaktaOriginal)
            avklartefaktaOriginal.behandlingsresultat = it
            it.vilkaarsresultater.add(vilkaarsresultatOriginal)
            vilkaarsresultatOriginal.behandlingsresultat = it
            it.behandlingsresultatBegrunnelser.add(lagBehandlingsresultatBegrunnelse(it))
            it.kontrollresultater.add(lagKontrollresultat(it))
            it.anmodningsperioder.add(anmodningsperiodeOriginal)
            anmodningsperiodeOriginal.behandlingsresultat = it
            it.utpekingsperioder.add(utpekingsperiodeOriginal)
            utpekingsperiodeOriginal.behandlingsresultat = it
            it.lovvalgsperioder.add(innvilgetLovvalgsperiode)
            innvilgetLovvalgsperiode.behandlingsresultat = it
            it.lovvalgsperioder.add(avslaattLovvalgsperiode)
            avslaattLovvalgsperiode.behandlingsresultat = it
            it.lovvalgsperioder.add(opphoertLovvalgsperiode)
            opphoertLovvalgsperiode.behandlingsresultat = it
        }


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
        val innvilgetLovvalgsperiodeReplika = behandlingsresultatReplika.lovvalgsperioder.first { it.erInnvilget() }

        innvilgetLovvalgsperiodeReplika.trygdeavgiftsperioder shouldNotBeSameInstanceAs innvilgetLovvalgsperiodeOriginal.trygdeavgiftsperioder

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

        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
        val avslaattMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT, 2L)
        val opphoertMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT, 3L)
        innvilgetMedlemskapsperiode.trygdeavgiftsperioder.add(
            lagTrygdeavgiftsperiode().copyEntity(grunnlagMedlemskapsperiode = innvilgetMedlemskapsperiode)
        )

        val lovvalgsperiodeOriginal = lagLovvalgsperiode()
        val avklartefaktaOriginal = lagAvklartefakta()
        val vilkaarsresultatOriginal = lagVilkaarsresultat()
        val anmodningsperiodeOriginal = lagAnmodningsperiode()
        val utpekingsperiodeOriginal = lagUtpekingsperiode()

        val behandlingsresultatOriginal = Behandlingsresultat.forTest {
            id = 30L
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
            vedtakMetadata { vedtaksdato = Instant.parse("2002-02-11T09:37:30Z") }
            medExistingMedlemskapsperiode(innvilgetMedlemskapsperiode)
            medExistingMedlemskapsperiode(avslaattMedlemskapsperiode)
            medExistingMedlemskapsperiode(opphoertMedlemskapsperiode)
        }.also {
            it.avklartefakta.add(avklartefaktaOriginal)
            avklartefaktaOriginal.behandlingsresultat = it
            it.vilkaarsresultater.add(vilkaarsresultatOriginal)
            vilkaarsresultatOriginal.behandlingsresultat = it
            it.lovvalgsperioder.add(lovvalgsperiodeOriginal)
            lovvalgsperiodeOriginal.behandlingsresultat = it
            it.behandlingsresultatBegrunnelser.add(lagBehandlingsresultatBegrunnelse(it))
            it.kontrollresultater.add(lagKontrollresultat(it))
            it.anmodningsperioder.add(anmodningsperiodeOriginal)
            anmodningsperiodeOriginal.behandlingsresultat = it
            it.utpekingsperioder.add(utpekingsperiodeOriginal)
            utpekingsperiodeOriginal.behandlingsresultat = it
        }

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

        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(behandlingsresultatOriginal)
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
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
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
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1L)
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
        val inntektsperiode = inntektForTest {
            id = 1L
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(1000.0)
            arbeidsgiversavgiftBetalesTilSkatt = false
        }
        val skatteforholdTilNorge = skatteforholdForTest {
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

    private fun lagMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat, id: Long) = medlemskapsperiodeForTest {
        this.id = id
        this.innvilgelsesresultat = innvilgelsesResultat
        medlPeriodeID = 123L
        fom = LocalDate.now()
        tom = LocalDate.now()
        medlemskapstype = Medlemskapstyper.PLIKTIG
        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
    }

    private fun lagHelseutgiftDekkesPeriode(behandlingsresultat: Behandlingsresultat) = HelseutgiftDekkesPeriode(
        behandlingsresultat,
        fomDato = LocalDate.now(),
        tomDato = LocalDate.now(),
        bostedLandkode = Land_iso2.DK
    )

    private fun lagBehandlingsresultatMedData(tidligsteInaktiveBehandling: Behandling) = Behandlingsresultat.forTest {
        id = 30L
        behandling = tidligsteInaktiveBehandling
        behandlingsmåte = Behandlingsmaate.MANUELT
        type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
        utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
        vedtakMetadata { vedtaksdato = Instant.parse("2002-02-11T09:37:30Z") }
    }

    private fun lagLovvalgsperiode(
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
        id: Long? = 32L
    ) = lovvalgsperiodeForTest {
        this.id = id
        this.innvilgelsesresultat = innvilgelsesResultat
        dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        fom = LocalDate.now()
        tom = LocalDate.now().plusMonths(2)
        medlPeriodeID = 777L
    }

    private fun lagAnmodningsperiode() = anmodningsperiodeForTest {
        id = 32L
        fom = LocalDate.now()
        tom = LocalDate.now().plusYears(1L)
        lovvalgsland = Land_iso2.SE
        unntakFraLovvalgsland = Land_iso2.NO
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        tilleggsbestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1
        sendtUtland = true
        anmodningsperiodeSvar { }
        dekning = Trygdedekninger.FULL_DEKNING_EOSFO
    }

    private fun lagUtpekingsperiode() = utpekingsperiodeForTest {
        id = 11111L
        fom = LocalDate.now()
        tom = LocalDate.now().plusYears(1)
        lovvalgsland = Land_iso2.SE
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
        tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        medlPeriodeID = 1242L
        sendtUtland = LocalDate.now()
    }

    private fun lagAvklartefakta() = avklartefaktaForTest {
        id = 32L
        fakta = "fakta"
        type = Avklartefaktatyper.ARBEIDSLAND
        registrering { begrunnelseKode = "registrering_begrunnelsekode" }
    }

    private fun lagBehandlingsresultatBegrunnelse(behandlingsresultat: Behandlingsresultat) =
        BehandlingsresultatBegrunnelse().apply {
            id = 32L
            this.behandlingsresultat = behandlingsresultat
            kode = "begrunnelsekode"
        }

    private fun lagVilkaarsresultat() = vilkaarsresultatForTest {
        id = 32L
        begrunnelseFritekst = "fritekst"
        begrunnelseFritekstEessi = "free text"
        begrunnelse("kode")
    }

    private fun lagKontrollresultat(behandlingsresultat: Behandlingsresultat) = kontrollresultatForTest {
        id = 123L
        begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
    }.apply { this.behandlingsresultat = behandlingsresultat }


    @Test
    fun `filtrerTrygdeavgiftsperioder med toggle PA - feiler når trygdeavgiftsperiode går over flere år`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val trygdeavgiftsperioder = listOf(
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = LocalDate.of(innevarendeAr - 1, 9, 1),
                periodeTil = LocalDate.of(innevarendeAr, 8, 31),
                trygdeavgiftsbeløpMd = Penger(1000.0),
                trygdesats = BigDecimal("7.8")
            )
        )

        val exception =
            shouldThrow<IllegalStateException> { replikerBehandlingsresultatService.filtrerTrygdeavgiftsperioder(trygdeavgiftsperioder) }

        exception.message shouldBe "Trygdeavgiftsperiode 1 går over flere år (${innevarendeAr - 1}-09-01 - ${innevarendeAr}-08-31)"
    }

    @Test
    fun `filtrerTrygdeavgiftsperioder med toggle PA - filtrerer bort perioder som slutter før innevaerende ar`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val trygdeavgiftsperioder = listOf(
            // Periode som slutter før innevaerende ar - skal filtreres bort
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = LocalDate.of(innevarendeAr - 2, 1, 1),
                periodeTil = LocalDate.of(innevarendeAr - 2, 12, 31),
                trygdeavgiftsbeløpMd = Penger(500.0),
                trygdesats = BigDecimal("5.1")
            ),
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = LocalDate.of(innevarendeAr - 1, 1, 1),
                periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
                trygdeavgiftsbeløpMd = Penger(500.0),
                trygdesats = BigDecimal("5.1")
            ),
            // Periode som går inn i innevaerende ar - skal beholdes og avkortes
            Trygdeavgiftsperiode(
                id = 2L,
                periodeFra = LocalDate.of(innevarendeAr, 1, 1),
                periodeTil = LocalDate.of(innevarendeAr, 6, 30),
                trygdeavgiftsbeløpMd = Penger(1500.0),
                trygdesats = BigDecimal("7.8")
            )
        )

        val resultat = replikerBehandlingsresultatService.filtrerTrygdeavgiftsperioder(trygdeavgiftsperioder)

        resultat shouldHaveSize 1
        resultat[0].periodeFra shouldBe LocalDate.of(innevarendeAr, 1, 1) // Avkortet
        resultat[0].periodeTil shouldBe LocalDate.of(innevarendeAr, 6, 30)
        resultat[0].trygdeavgiftsbeløpMd shouldBe Penger(1500.0)
    }

    @Test
    fun `replikerTrygdeavgiftForPensjonist med toggle PA - feiler når trygdeavgiftsperioder går over flere ar`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(behandlingsresultatOriginal)

        val inntektsperiode = inntektForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 6, 1)
            tomDato = LocalDate.of(innevarendeAr, 6, 30)
        }

        val skatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 1, 1)
            tomDato = LocalDate.of(innevarendeAr, 12, 31)
        }

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr - 1, 9, 1), // Starter før innevaerende ar
            periodeTil = LocalDate.of(innevarendeAr, 8, 31),    // Slutter i innevaerende ar
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

        exception.message shouldBe "Trygdeavgiftsperiode 1 går over flere år (${innevarendeAr - 1}-09-01 - ${innevarendeAr}-08-31)"
    }

    @Test
    fun `replikerTrygdeavgiftForPensjonist med toggle PA - avkorter også inntektsperioder og skatteforhold som starter før innevaerende ar`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(behandlingsresultatOriginal)

        // Opprett inntektsperiode som overlapper med innevaerende ar (skal avkortes)
        val overlappendeInntektsperiode = inntektForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 1, 1)  // Starter før innevaerende ar
            tomDato = LocalDate.of(innevarendeAr, 12, 31)    // Slutter i innevaerende ar
        }

        // Opprett skatteforhold som overlapper med innevaerende ar (skal avkortes)
        val overlappendeSkatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 6, 1) // Starter før innevaerende ar
            tomDato = LocalDate.of(innevarendeAr, 6, 30)     // Slutter i innevaerende ar
        }

        // Trygdeavgiftsperiode som overlapper med innevaerende ar
        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr - 1, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagInntekstperiode = overlappendeInntektsperiode,  // Denne skal avkortes
            grunnlagSkatteforholdTilNorge = overlappendeSkatteforhold // Denne skal avkortes
        )

        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(innevarendeAr, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr, 12, 31),
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
        replisertPeriode.periodeFra shouldBe LocalDate.of(innevarendeAr, 1, 1) // Avkortet

        // Inntektsperioden skal også være avkortet til å starte 1. januar i innevaerende ar
        replisertPeriode.grunnlagInntekstperiode?.fomDato shouldBe LocalDate.of(innevarendeAr, 1, 1) // Avkortet
        replisertPeriode.grunnlagInntekstperiode?.tomDato shouldBe LocalDate.of(innevarendeAr, 12, 31) // Uendret

        // Skatteforholdet skal være avkortet til å starte 1. januar i innevaerende ar
        replisertPeriode.grunnlagSkatteforholdTilNorge?.fomDato shouldBe LocalDate.of(innevarendeAr, 1, 1) // Avkortet
        replisertPeriode.grunnlagSkatteforholdTilNorge?.tomDato shouldBe LocalDate.of(innevarendeAr, 6, 30)  // Uendret
    }

    @Test
    fun `replikerTrygdeavgift med toggle PA - filtrerer og avkorter for vanlige medlemmer`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode som trygdeavgiftsperiodene skal knyttes til
        val medlemskapsperiode = medlemskapsperiodeForTest {
            id = 1L
            fom = LocalDate.of(innevarendeAr - 1, 1, 1)
            tom = LocalDate.of(innevarendeAr + 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        // Opprett inntektsperiode som overlapper med innevaerende ar
        val inntektsperiode = inntektForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 6, 1)
            tomDato = LocalDate.of(innevarendeAr, 6, 30)
        }

        // Opprett skatteforhold som overlapper med innevaerende ar
        val skatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 3, 1)
            tomDato = LocalDate.of(innevarendeAr, 12, 31)
        }

        // Trygdeavgiftsperiode som starter før og slutter i innevaerende ar
        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr, 6, 30),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode1)

        // Trygdeavgiftsperiode som slutter før innevaerende ar (skal filtreres bort)
        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(innevarendeAr - 2, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 2, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        val trygdeavgiftsperiode3 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(innevarendeAr - 1, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
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

        // Kun periode som overlapper med innevaerende ar skal replikeres
        behandlingsresultatReplika.trygdeavgiftsperioder shouldHaveSize 1
        val replisertPeriode = behandlingsresultatReplika.trygdeavgiftsperioder.first()

        // Periode skal være avkortet til 1. januar
        replisertPeriode.periodeFra shouldBe LocalDate.of(innevarendeAr, 1, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(innevarendeAr, 6, 30)

        // Inntektsperiode skal også være avkortet
        replisertPeriode.grunnlagInntekstperiode?.fomDato shouldBe LocalDate.of(innevarendeAr, 1, 1)
        replisertPeriode.grunnlagInntekstperiode?.tomDato shouldBe LocalDate.of(innevarendeAr, 6, 30)

        // Skatteforhold skal også være avkortet
        replisertPeriode.grunnlagSkatteforholdTilNorge?.fomDato shouldBe LocalDate.of(innevarendeAr, 1, 1)
        replisertPeriode.grunnlagSkatteforholdTilNorge?.tomDato shouldBe LocalDate.of(innevarendeAr, 12, 31)
    }

    @Test
    fun `replikerTrygdeavgift med toggle AV - ingen filtrering eller avkorting`() {
        unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode
        val medlemskapsperiode = medlemskapsperiodeForTest {
            id = 1L
            fom = LocalDate.of(innevarendeAr - 2, 1, 1)
            tom = LocalDate.of(innevarendeAr - 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        // Opprett skatteforhold (pakrevd)
        val skatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 2, 1, 1)
            tomDato = LocalDate.of(innevarendeAr - 1, 12, 31)
        }

        // Trygdeavgiftsperiode som slutter før innevaerende ar
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr - 2, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
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
        replisertPeriode.periodeFra shouldBe LocalDate.of(innevarendeAr - 2, 1, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(innevarendeAr - 1, 12, 31)
    }

    @Test
    fun `replikerTrygdeavgiftForPensjonist med toggle AV - ingen filtrering eller avkorting`() {
        unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.PENSJONIST
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(behandlingsresultatOriginal)

        // Opprett skatteforhold (pakrevd for pensjonist også)
        val skatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 2, 1, 1)
            tomDato = LocalDate.of(innevarendeAr - 1, 12, 31)
        }

        // Trygdeavgiftsperiode som slutter før innevaerende ar
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr - 2, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
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
        replisertPeriode.periodeFra shouldBe LocalDate.of(innevarendeAr - 2, 1, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(innevarendeAr - 1, 12, 31)
    }

    @Test
    fun `filtrerTrygdeavgiftsperioder med toggle AV - returnerer alle perioder uendret`() {
        unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val periode1 = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr - 2, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(1000.0),
            trygdesats = BigDecimal("7.8")
        )

        val periode2 = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(innevarendeAr, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2")
        )

        val resultat = replikerBehandlingsresultatService.filtrerTrygdeavgiftsperioder(listOf(periode1, periode2))

        resultat shouldHaveSize 2
        resultat[0].periodeFra shouldBe LocalDate.of(innevarendeAr - 2, 1, 1)
        resultat[0].periodeTil shouldBe LocalDate.of(innevarendeAr - 1, 12, 31)
        resultat[1].periodeFra shouldBe LocalDate.of(innevarendeAr, 1, 1)
        resultat[1].periodeTil shouldBe LocalDate.of(innevarendeAr, 12, 31)
    }

    @Test
    fun `replikerTrygdeavgift med toggle PA - håndterer periode som starter og slutter i innevaerende ar korrekt`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode og skatteforhold
        val medlemskapsperiode = medlemskapsperiodeForTest {
            id = 1L
            fom = LocalDate.of(innevarendeAr, 1, 1)
            tom = LocalDate.of(innevarendeAr, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        val skatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr, 1, 1)
            tomDato = LocalDate.of(innevarendeAr, 12, 31)
        }

        // Trygdeavgiftsperiode som starter og slutter i innevaerende ar (skal ikke avkortes)
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr, 3, 1),
            periodeTil = LocalDate.of(innevarendeAr, 9, 30),
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

        // Periode som allerede starter i innevaerende ar skal ikke avkortes
        replisertPeriode.periodeFra shouldBe LocalDate.of(innevarendeAr, 3, 1)
        replisertPeriode.periodeTil shouldBe LocalDate.of(innevarendeAr, 9, 30)
    }

    @Test
    fun `replikerTrygdeavgift med toggle PA - håndterer periode som går over flere ar fremover`() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            type = Behandlingstyper.NY_VURDERING
            fagsak {
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.UNNTAK
            }
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Opprett medlemskapsperiode og skatteforhold
        val medlemskapsperiode = medlemskapsperiodeForTest {
            id = 1L
            fom = LocalDate.of(innevarendeAr - 1, 1, 1)
            tom = LocalDate.of(innevarendeAr + 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandlingsresultatOriginal.medlemskapsperioder.add(medlemskapsperiode)

        val skatteforhold = skatteforholdForTest {
            id = 1L
            fomDato = LocalDate.of(innevarendeAr - 1, 6, 1)
            tomDato = LocalDate.of(innevarendeAr + 1, 12, 31)
        }

        // Trygdeavgiftsperiode som går fra forrige ar til neste ar
        val trygdeavgiftsperiodeIFjor = Trygdeavgiftsperiode(
            id = 1L,
            periodeFra = LocalDate.of(innevarendeAr - 1, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr - 1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        // Trygdeavgiftsperiode som går fra forrige ar til neste ar
        val trygdeavgiftsperiodeIAr = Trygdeavgiftsperiode(
            id = 2L,
            periodeFra = LocalDate.of(innevarendeAr, 1, 1),
            periodeTil = LocalDate.of(innevarendeAr, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        val trygdeavgiftsperiodeNesteAr = Trygdeavgiftsperiode(
            id = 3L,
            periodeFra = LocalDate.of(innevarendeAr+1 , 1, 1),
            periodeTil = LocalDate.of(innevarendeAr+1, 12, 31),
            trygdeavgiftsbeløpMd = Penger(2000.0),
            trygdesats = BigDecimal("8.2"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeIFjor)
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeIAr)
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeNesteAr)

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
        val replisertPeriodeIAr = behandlingsresultatReplika.trygdeavgiftsperioder.minBy { b -> b.periodeFra }
        val replisertPeriodeNesteAr = behandlingsresultatReplika.trygdeavgiftsperioder.maxBy { b -> b.periodeFra }

        // Periode skal avkortes til å starte 1. januar, men slutt-dato skal være uendret
        replisertPeriodeIAr.periodeFra shouldBe LocalDate.of(innevarendeAr, 1, 1)
        replisertPeriodeIAr.periodeTil shouldBe LocalDate.of(innevarendeAr, 12, 31)

        replisertPeriodeNesteAr.periodeFra shouldBe LocalDate.of(innevarendeAr + 1, 1, 1)
        replisertPeriodeNesteAr.periodeTil shouldBe LocalDate.of(innevarendeAr + 1, 12, 31)
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
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        val lovvalgsperiodeOriginal = lagLovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
        }
        behandlingsresultatOriginal.lovvalgsperioder.add(lovvalgsperiodeOriginal)
        lovvalgsperiodeOriginal.behandlingsresultat = behandlingsresultatOriginal

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
    fun `replikerMedlemskapsperioderBasertPaBehandlingstype inkluderer opphørt for MANGLENDE_INNBETALING_TRYGDEAVGIFT`() {
        val innevarendeAr = LocalDate.now().year

        val tidligsteInaktiveBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }
        val behandlingsresultatOriginal = lagBehandlingsresultatMedData(tidligsteInaktiveBehandling)

        // Legg til både innvilget og opphørt medlemskapsperiode
        val innvilgetPeriode = medlemskapsperiodeForTest {
            id = 1L
            fom = LocalDate.of(innevarendeAr, 1, 1)
            tom = LocalDate.of(innevarendeAr, 6, 30)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }

        val opphoertPeriode = medlemskapsperiodeForTest {
            id = 2L
            fom = LocalDate.of(innevarendeAr, 7, 1)
            tom = LocalDate.of(innevarendeAr, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
        }

        val avslaattPeriode = medlemskapsperiodeForTest {
            id = 3L
            fom = LocalDate.of(innevarendeAr - 1, 1, 1)
            tom = LocalDate.of(innevarendeAr - 1, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }

        behandlingsresultatOriginal.medlemskapsperioder.addAll(setOf(innvilgetPeriode, opphoertPeriode, avslaattPeriode))

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
