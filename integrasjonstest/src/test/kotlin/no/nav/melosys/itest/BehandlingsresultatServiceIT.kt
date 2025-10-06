package no.nav.melosys.itest

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.getunleash.FakeUnleash
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
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.ReplikerBehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.toJsonString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Import(value = [ReplikerBehandlingsresultatService::class, BehandlingsresultatService::class, SaksbehandlingRegler::class, FakeUnleash::class, VilkaarsresultatService::class])
class BehandlingsresultatServiceIT(
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val replikerBehandlingsresultatService: ReplikerBehandlingsresultatService,
    @Autowired
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository
) : DataJpaTestBase() {
    data class Behandlinger(val orginal: Behandling, val replika: Behandling)

    // Denne kan ikke wires inn i siden vi ikke har hele spring contex
    private val objectMapper = jacksonObjectMapper().apply {
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        registerModule(JavaTimeModule())
    }

    @Test
    fun `id i database blir riktig etter replikerting`() {
        val behandlinger = lagFagsakMedBehandlinger()

        val behandlingsresultat = lagBehandlingsresultat(behandlinger.orginal)
        behandlingsresultatRepository.save(behandlingsresultat)
        replikerBehandlingsresultatService.replikerBehandlingsresultat(behandlinger.orginal, behandlinger.replika)

        val replikaResultat = behandlingsresultatRepository.findById(behandlinger.replika.id).get()

        listOf(
            replikaResultat.lovvalgsperioder,
            replikaResultat.avklartefakta,
            replikaResultat.vilkaarsresultater,
            replikaResultat.behandlingsresultatBegrunnelser,
            replikaResultat.kontrollresultater,
            replikaResultat.anmodningsperioder,
            replikaResultat.utpekingsperioder
        ).forEach {
            assertThat(it)
                .singleElement()
                .hasFieldOrPropertyWithValue("behandlingsresultat.id", behandlinger.replika.id)
        }
        assertThat(replikaResultat.avklartefakta.flatMap { it.registreringer })
            .singleElement()
            .matches { it.avklartefakta.id == replikaResultat.avklartefakta.first().id }

        assertThat(replikaResultat.vilkaarsresultater.flatMap { it.begrunnelser })
            .singleElement()
            .matches { it.vilkaarsresultat.id == replikaResultat.vilkaarsresultater.first().id }
    }

    @Test
    fun `medlem av folketrygden skal kun ha innvilget når Behandlingstype ikke er MANGLENDE_INNBETALING_TRYGDEAVGIFT`() {
        val behandlinger = lagFagsakMedBehandlinger()

        val behandlingsresultat = lagBehandlingsresultat(behandlinger.orginal)
        behandlingsresultatRepository.save(behandlingsresultat)
        replikerBehandlingsresultatService.replikerBehandlingsresultat(behandlinger.orginal, behandlinger.replika)

        val replikaResultat = behandlingsresultatRepository.findById(behandlinger.replika.id).get()

        assertThat(
            behandlingsresultat.toMap(
                // Filterer bort medlemskapsperioder som ikke er innvilget siden dette ikke replikeres
                medlemskapsperiodeFilter = { it.innvilgelsesresultat == InnvilgelsesResultat.INNVILGET }
            ).toJsonString(objectMapper)).isEqualTo(
            replikaResultat.toMap().toJsonString(objectMapper)
        )
    }

    private fun lagFagsakMedBehandlinger(): Behandlinger {
        Fagsak("MEL-test", null, Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.LOVVALG_AVKLART)
            .apply { leggTilRegisteringInfo() }
            .also { fsak ->
                fagsakRepository.save(fsak)

                val tidligsteInaktiveBehandling = Behandling.forTest {
                    fagsak = fsak
                    behandlingsfrist = LocalDate.now().plusYears(1)
                    status = Behandlingsstatus.AVSLUTTET
                    type = Behandlingstyper.FØRSTEGANG
                    tema = Behandlingstema.YRKESAKTIV
                }.also { behandlingRepository.save(it) }

                val behandlingsreplika = Behandling.forTest {
                    fagsak = fsak
                    behandlingsfrist = LocalDate.now().plusYears(1)
                    status = Behandlingsstatus.OPPRETTET
                    type = Behandlingstyper.FØRSTEGANG
                    tema = Behandlingstema.YRKESAKTIV
                }.also {
                    behandlingRepository.save(it)
                }
                return Behandlinger(tidligsteInaktiveBehandling, behandlingsreplika)
            }
    }

    fun lagBehandlingsresultat(tidligsteInaktiveBehandling: Behandling): Behandlingsresultat =
        Behandlingsresultat().apply {
            behandling = tidligsteInaktiveBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            utfallUtpeking = Utfallregistreringunntak.IKKE_GODKJENT
            utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT
            leggTilRegisteringInfo()
        }.also { br ->
            br.vedtakMetadata = VedtakMetadata().apply {
                behandlingsresultat = br
                vedtaksdato = Instant.parse("2002-02-11T09:37:30Z")
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                leggTilRegisteringInfo()
            }

            br.avklartefakta.add(
                Avklartefakta().apply {
                    behandlingsresultat = br
                    fakta = "fakta"
                    type = Avklartefaktatyper.ARBEIDSLAND
                    referanse = "referanse"
                }.also {
                    it.registreringer.add(
                        AvklartefaktaRegistrering().apply {
                            avklartefakta = it
                            begrunnelseKode = "AvklartefaktaRegistrering-begrunnelsekode"
                            leggTilRegisteringInfo()
                        })
                })

            br.vilkaarsresultater.add(
                Vilkaarsresultat().apply {
                    behandlingsresultat = br
                    begrunnelseFritekst = "fritekst"
                    begrunnelseFritekstEessi = "free text"
                    vilkaar = Vilkaar.BOSATT_I_NORGE
                    leggTilRegisteringInfo()
                }.also {
                    it.begrunnelser = setOf(VilkaarBegrunnelse().apply {
                        vilkaarsresultat = it
                        kode = "kode"
                        leggTilRegisteringInfo()
                    })
                })

            br.lovvalgsperioder.add(
                Lovvalgsperiode().apply {
                    behandlingsresultat = br
                    dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusMonths(2)
                })

            br.behandlingsresultatBegrunnelser.add(
                BehandlingsresultatBegrunnelse().apply {
                    behandlingsresultat = br
                    kode = "begrunnelsekode"
                }
            )

            br.kontrollresultater.add(
                Kontrollresultat().apply {
                    behandlingsresultat = br
                    begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
                })

            br.anmodningsperioder.add(
                Anmodningsperiode().apply {
                    behandlingsresultat = br
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusYears(1L)
                    lovvalgsland = Land_iso2.SE
                    unntakFraLovvalgsland = Land_iso2.NO
                    bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
                    unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                    tilleggsbestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1
                    setSendtUtland(true)
                    dekning = Trygdedekninger.FULL_DEKNING_EOSFO
                }.also {
                    it.anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
                        anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
                        anmodningsperiode = it
                        registrertDato = LocalDate.now()
                    }
                }
            )

            br.utpekingsperioder.add(
                Utpekingsperiode(
                    LocalDate.now(),
                    LocalDate.now().plusYears(1),
                    Land_iso2.SE,
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
                ).apply {
                    behandlingsresultat = br
                    medlPeriodeID = 1242L
                    sendtUtland = LocalDate.now()
                }
            )

            val medlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET)
            medlemskapsperiode.trygdeavgiftsperioder.add(
                lagTrygdeavgiftsperiode(grunnlagInntekstperiode = medlemskapsperiode)
            )
            br.addMedlemskapsperiode(medlemskapsperiode)
            br.leggTilMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT)
            br.leggTilMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT)
        }

    private fun Behandlingsresultat.leggTilMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat): Medlemskapsperiode =
        lagMedlemskapsperiode(innvilgelsesResultat).also {
            this.addMedlemskapsperiode(it)
        }

    private fun lagMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat): Medlemskapsperiode =
        Medlemskapsperiode().apply {
            innvilgelsesresultat = innvilgelsesResultat
            medlPeriodeID = 77L
            fom = LocalDate.now()
            tom = LocalDate.now()
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
            medlPeriodeID = 123L
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        }

    private fun lagTrygdeavgiftsperiode(grunnlagInntekstperiode: Medlemskapsperiode) = Trygdeavgiftsperiode(
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        trygdeavgiftsbeløpMd = Penger(500.0),
        trygdesats = BigDecimal(50),
        grunnlagInntekstperiode = Inntektsperiode().apply {
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(1000.0)
            isArbeidsgiversavgiftBetalesTilSkatt = false
        },
        grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        },
        grunnlagMedlemskapsperiode = grunnlagInntekstperiode
    )

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }

    fun Behandlingsresultat.toMap(
        medlemskapsperiodeFilter: (Medlemskapsperiode) -> Boolean = { true }
    ): Map<String, Any?> = mapOf(
        "fastsattAvLand" to fastsattAvLand,
        "begrunnelseFritekst" to begrunnelseFritekst,
        "innledningFritekst" to innledningFritekst,
        "trygdeavgiftFritekst" to trygdeavgiftFritekst,
        "nyVurderingBakgrunn" to nyVurderingBakgrunn,
        "fakturaserieReferanse" to fakturaserieReferanse,
        "avklartefakta" to avklartefakta.map {
            it.run {
                mapOf(
                    "type" to type,
                    "referanse" to referanse,
                    "subjekt" to subjekt,
                    "fakta" to fakta,
                    "begrunnelseFritekst" to begrunnelseFritekst,
                    "registreringer" to registreringer.map { r ->
                        r.run {
                            mapOf(
                                "begrunnelseKode" to begrunnelseKode,
                            )
                        }
                    }
                )
            }
        },
        "lovvalgsperioder" to lovvalgsperioder.map {
            it.run {
                mapOf(
                    "fom" to fom,
                    "tom" to tom,
                    "innvilgelsesresultat" to innvilgelsesresultat,
                    "medlemskapstype" to medlemskapstype,
                    "dekning" to dekning,
                    "bestemmelse" to bestemmelse,
                    "tilleggsbestemmelse" to tilleggsbestemmelse
                )
            }
        },
        "utpekingsperioder" to utpekingsperioder.map {
            it.run {
                mapOf(
                    "fom" to fom,
                    "tom" to tom,
                    "lovvalgsland" to lovvalgsland,
                    "bestemmelse" to bestemmelse,
                    "tilleggsbestemmelse" to tilleggsbestemmelse,
                )
            }
        },
        "vilkaarsresultater" to vilkaarsresultater.map { vr ->
            mapOf(
                "vilkaar" to vr.vilkaar,
                "oppfylt" to vr.isOppfylt,
                "begrunnelser" to vr.begrunnelser.map { mapOf("kode" to it.kode) }
            )
        },
        "kontrollresultater" to kontrollresultater.map { mapOf("begrunnelse" to it.begrunnelse) },
        "medlemskapsperioder" to medlemskapsperioder.filter { medlemskapsperiodeFilter(it) }.map { mp ->
            mp.run {
                mapOf(
                    "fom" to fom,
                    "tom" to tom,
                    "innvilgelsesresultat" to innvilgelsesresultat,
                    "trygdedekning" to trygdedekning,
                    "trygdeavgiftsperioder" to trygdeavgiftsperioder.map { tp ->
                        mapOf(
                            "grunnlagSkatteforholdTilNorge" to tp.grunnlagSkatteforholdTilNorge?.run {
                                mapOf<String, Any>(
                                    "fomDato" to fomDato,
                                    "tomDato" to tomDato,
                                    "skatteplikttype" to skatteplikttype
                                )
                            },
                            "grunnlagInntekstperiode" to tp.grunnlagInntekstperiode?.run {
                                mapOf(
                                    "fomDato" to fomDato,
                                    "tomDato" to tomDato,
                                    "type" to type,
                                    "avgiftspliktigMndInntekt" to avgiftspliktigMndInntekt.verdi,
                                    "arbeidsgiversavgiftBetalesTilSkatt" to isArbeidsgiversavgiftBetalesTilSkatt
                                )
                            },
                        )
                    }
                )
            }
        },
        "behandlingsresultatBegrunnelser" to behandlingsresultatBegrunnelser.map { mapOf("kode" to it.kode) },
        "årsavregning" to årsavregning?.run {
            mapOf(
                "aar" to aar,
                "tidligereFakturertBeloep" to tidligereFakturertBeloep,
                "beregnetAvgiftBelop" to beregnetAvgiftBelop,
                "tilFaktureringBeloep" to tilFaktureringBeloep,
            )
        },
        "trygdeavgiftType" to trygdeavgiftType
    )
}
