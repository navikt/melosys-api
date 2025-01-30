package no.nav.melosys.service.saksbehandling

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.MockKAdditionalAnswerScope
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaksbehandlingReglerTest {
    @MockK
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    private lateinit var saksbehandlingRegler: SaksbehandlingRegler
    private val unleash = FakeUnleash()


    @BeforeEach
    fun setUp() {
        unleash.resetAll()
        saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository, unleash)
    }

    fun harUtsendtArbeidsTakerKunNorgeFlytParametere() =
        listOf(
            Arguments.of(
                true,
                Behandlingstema.ARBEID_KUN_NORGE,
                Land_iso2.NO,
                true
            ),
            Arguments.of(
                true,
                Behandlingstema.UTSENDT_SELVSTENDIG,
                Land_iso2.NO,
                true
            ),
            Arguments.of(
                true,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Land_iso2.NO,
                true
            ),
            Arguments.of(
                false,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Land_iso2.NO,
                false
            ),
            Arguments.of(
                true,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Land_iso2.FI,
                false
            ),
            Arguments.of(
                true,
                Behandlingstema.YRKESAKTIV,
                Land_iso2.NO,
                false
            ),
        )

    fun testHarIngenFlytParametere() =
        listOf(
            Arguments.of(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.TRYGDEAVGIFT,
                Behandlingstyper.HENVENDELSE,
                Behandlingstema.IKKE_YRKESAKTIV,
                true
            ),
            Arguments.of(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.HENVENDELSE,
                Behandlingstema.IKKE_YRKESAKTIV,
                true
            ),
            Arguments.of(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.KLAGE,
                Behandlingstema.IKKE_YRKESAKTIV,
                true
            ),
            Arguments.of(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
                Behandlingstema.IKKE_YRKESAKTIV,
                false
            ),
            Arguments.of(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                true
            ),
            Arguments.of(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.IKKE_YRKESAKTIV,
                false
            ),
            Arguments.of(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.YRKESAKTIV,
                false
            ),
            Arguments.of(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.IKKE_YRKESAKTIV,
                false
            ),
            Arguments.of(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
                Behandlingstema.YRKESAKTIV,
                false,
            ),
            Arguments.of(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.IKKE_YRKESAKTIV,
                false
            ),
            Arguments.of(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.ARBEID_KUN_NORGE,
                false
            ),
        )


    @ParameterizedTest
    @MethodSource("testHarIngenFlytParametere")
    fun testHarIngenFlyt(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        expected: Boolean
    ) {
        val result = saksbehandlingRegler.harIngenFlyt(sakstype, sakstema, behandlingstype, behandlingstema)

        result.shouldBe(expected)
    }

    @ParameterizedTest
    @MethodSource("harUtsendtArbeidsTakerKunNorgeFlytParametere")
    fun harUtsendtArbeidsTakerKunNorgeFlyt(
        erSakstypeEøs: Boolean,
        behandlingstema: Behandlingstema,
        land: Land_iso2,
        expected: Boolean,
    ) {
        val result = saksbehandlingRegler.harUtsendtArbeidsTakerKunNorgeFlyt(erSakstypeEøs, behandlingstema, land)

        result.shouldBe(expected)
    }

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("tidligereBehandlingSkalIkkeReplikeresData")
    fun tidligereBehandlingSkalIkkeReplikeres(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        behandlingHolder: BehandlingHolder
    ) {
        val behandlingReplikeringsRegler = behandlingHolder.setup(behandlingsresultatRepository, unleash)


        val result = behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(
            behandlingHolder.lagFagsak(sakstype, sakstema), behandlingstype, behandlingstema
        )


        result.shouldBe(false)
    }

    private fun tidligereBehandlingSkalIkkeReplikeresData() = listOf(
        arguments(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.HENVENDELSE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(Behandlingstyper.ENDRET_PERIODE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingsresultattyper.HENLEGGELSE
                )
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.HENVENDELSE,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingsresultattyper.AVVIST_KLAGE
                )
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingsresultattyper.AVVIST_KLAGE
                )
            }
        ),
        arguments(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(Behandlingstyper.FØRSTEGANG, Behandlingstema.YRKESAKTIV)
            }
        ),
        arguments(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingsresultattyper.OPPHØRT
                )
            }
        ),
    )

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("tidligereBehandlingSkalReplikeresData")
    fun tidligereBehandlingSkalReplikeres(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        behandlingHolder: BehandlingHolder
    ) {
        val behandlingReplikeringsRegler = behandlingHolder.setup(behandlingsresultatRepository, unleash)


        val result = behandlingReplikeringsRegler.skalTidligereBehandlingReplikeres(
            behandlingHolder.lagFagsak(sakstype, sakstema), behandlingstype, behandlingstema
        )


        result.shouldBe(true)
    }

    private fun tidligereBehandlingSkalReplikeresData() = listOf(
        arguments(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingsresultattyper.AVVIST_KLAGE
                )
            }
        ),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                )
            }
        ),
        arguments(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
            Behandlingstema.YRKESAKTIV,
            BehandlingHolder().apply {
                add(
                    Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingsresultattyper.DELVIS_OPPHØRT
                )
            }
        ),
    )

    @ParameterizedTest(name = "behandlingsresultatType:{0} - FunnetBehandlingID:{2}")
    @MethodSource("behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData")
    fun finnBehandlingSomKanReplikeres_forventetBehandling(
        resultatTypeFraRepo: Behandlingsresultattyper?,
        behandlinger: List<Behandling>,
        expectedBehandlingID: Long?
    ) {
        every { behandlingsresultatRepository.findById(any()) } returns lagBehandlingsresultat(resultatTypeFraRepo)
        val saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository, unleash)

        val behandling = saksbehandlingRegler.finnBehandlingSomKanReplikeres(behandlinger)


        behandling?.id.shouldBe(expectedBehandlingID)
    }

    private fun behandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyperData(): List<Arguments> {
        val behandlingFørstegangAvsluttet = Behandling().apply {
            id = 0
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            status = Behandlingsstatus.AVSLUTTET
            fagsak = FagsakTestFactory.lagFagsak()
        }

        return listOf(
            arguments(
                Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
                listOf(behandlingFørstegangAvsluttet),
                null
            ),
            arguments(
                null,
                listOf(behandlingFørstegangAvsluttet),
                null
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.KLAGE
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = FagsakTestFactory.lagFagsak()
                    }
                ),
                null
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(behandlingFørstegangAvsluttet),
                0L
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.KLAGE
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = FagsakTestFactory.lagFagsak()
                    },
                    Behandling().apply {
                        id = 1
                        type = Behandlingstyper.FØRSTEGANG
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = FagsakTestFactory.lagFagsak()
                    }
                ),
                1L
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.FØRSTEGANG
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.UNDER_BEHANDLING
                        fagsak = FagsakTestFactory.lagFagsak()
                    },
                    Behandling().apply {
                        id = 1
                        type = Behandlingstyper.KLAGE
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = FagsakTestFactory.lagFagsak()
                    },
                    Behandling().apply {
                        id = 2
                        type = Behandlingstyper.NY_VURDERING
                        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                        status = Behandlingsstatus.AVSLUTTET
                        fagsak = FagsakTestFactory.lagFagsak()
                    }
                ),
                2L
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(Behandling().apply {
                    id = 0
                    type = Behandlingstyper.HENVENDELSE
                    tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = FagsakTestFactory.lagFagsak()
                }),
                null
            ),
            arguments(
                Behandlingsresultattyper.IKKE_FASTSATT,
                listOf(
                    Behandling().apply {
                        id = 0
                        type = Behandlingstyper.FØRSTEGANG
                        tema = Behandlingstema.YRKESAKTIV
                        status = Behandlingsstatus.AVSLUTTET
                        registrertDato = Instant.now().minus(90, ChronoUnit.DAYS)
                        fagsak = FagsakTestFactory.builder().apply {
                            type = Sakstyper.FTRL
                        }.build()
                    },
                    Behandling().apply {
                        id = 1
                        type = Behandlingstyper.NY_VURDERING
                        tema = Behandlingstema.YRKESAKTIV
                        status = Behandlingsstatus.AVSLUTTET
                        registrertDato = Instant.now().minus(60, ChronoUnit.DAYS)
                        fagsak = FagsakTestFactory.builder().apply {
                            type = Sakstyper.FTRL
                        }.build()
                    },
                    Behandling().apply {
                        id = 2
                        type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                        tema = Behandlingstema.YRKESAKTIV
                        status = Behandlingsstatus.AVSLUTTET
                        registrertDato = Instant.now().minus(30, ChronoUnit.DAYS)
                        fagsak = FagsakTestFactory.builder().apply {
                            type = Sakstyper.FTRL
                        }.build()
                    }
                ).sortedByDescending { it.registrertDato },
                2L
            ),
        )
    }

    @ParameterizedTest()
    @MethodSource("harRegistreringUnntakFraMedlemskapFlytData")
    fun harRegistreringUnntakFraMedlemskapFlyt_forventetResultat(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        forventetResultat: Boolean
    ) {
        saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(
            sakstype, sakstema, behandlingstema
        ).shouldBe(forventetResultat)
    }

    private fun harRegistreringUnntakFraMedlemskapFlytData() = listOf(
        arguments(Sakstyper.EU_EOS, Sakstemaer.UNNTAK, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR, true),
        arguments(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.UNNTAK,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            true
        ),
        arguments(Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK, true),
        arguments(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR,
            false
        ),
        arguments(Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR, false)
    )

    class BehandlingHolder {
        private val behandlingerMedType: ArrayList<Pair<Behandling, Behandlingsresultattyper?>> = ArrayList()

        fun setup(behandlingsresultatRepository: BehandlingsresultatRepository, unleash: FakeUnleash): SaksbehandlingRegler {
            setupMock { id: Long, behandlingsresultattype: Behandlingsresultattyper? ->
                every { behandlingsresultatRepository.findById(id) } returns lagBehandlingsresultat(
                    behandlingsresultattype
                )
            }
            return SaksbehandlingRegler(behandlingsresultatRepository, unleash)
        }

        fun add(
            type: Behandlingstyper,
            tema: Behandlingstema,
            behandlingsresultattype: Behandlingsresultattyper? = null
        ) {
            behandlingerMedType.add(Pair(Behandling().apply {
                this.tema = tema
                this.type = type
                this.status = Behandlingsstatus.AVSLUTTET
            }, behandlingsresultattype))
        }

        fun lagFagsak(sakstype: Sakstyper, sakstema: Sakstemaer): Fagsak {
            val fagsak = FagsakTestFactory.builder().apply {
                type = sakstype
                tema = sakstema
            }.build()
            behandlinger(fagsak).map { it.first }.forEach {
                fagsak.leggTilBehandling(it)
            }
            return fagsak
        }

        override fun toString(): String {
            return behandlingerMedType.map { (behandling, behandlingsresultattyper) ->
                "(${behandling.type}, ${behandling.tema}) - ${behandlingsresultattyper ?: "ikke i db"}"
            }.toString()
        }

        private fun behandlinger(fagsak: Fagsak): List<Pair<Behandling, Behandlingsresultattyper?>> {
            val date = LocalDate.of(2000, 1, 1).atStartOfDay()
            return behandlingerMedType.mapIndexed { index, it ->
                it.first.id = index.toLong()
                it.first.registrertDato = date.plusDays(index.toLong()).toInstant(ZoneOffset.UTC)
                it.first.fagsak = fagsak
                it
            }
        }

        private fun setupMock(function: (Long, Behandlingsresultattyper?) -> MockKAdditionalAnswerScope<Optional<Behandlingsresultat>, Optional<Behandlingsresultat>>) {
            var i: Long = 0
            behandlingerMedType.forEach {
                function(i++, it.second)
            }

        }
    }
}

fun lagBehandlingsresultat(resultatTypeFraRepo: Behandlingsresultattyper?): Optional<Behandlingsresultat> {
    if (resultatTypeFraRepo == null) return Optional.empty()
    return Optional.of(Behandlingsresultat().apply {
        id = 1L
        type = resultatTypeFraRepo
    })
}

