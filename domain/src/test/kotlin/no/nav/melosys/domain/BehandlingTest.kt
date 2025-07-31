package no.nav.melosys.domain

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BehandlingTest {
    var utgangspunktDato: LocalDate = LocalDate.now()

    @Test
    fun erAktiv_underBehandling_ja() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.VURDER_DOKUMENT
        }
        behandling.erAktiv().shouldBe(true)
    }

    @Test
    fun erAktiv_avsluttet_nei() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.AVSLUTTET
        }
        behandling.erAktiv().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erUnderBehandling_ja() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        behandling.erRedigerbar().shouldBe(true)
    }

    @Test
    fun erRedigerbar_erAvsluttet_nei() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.AVSLUTTET
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erMidlertidigLovvalgsbeslutning_nei() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erIverksetterVedtak_nei() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.IVERKSETTER_VEDTAK
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erAnmodningOmUnntakSendt_nei() {
        val behandling = Behandling.buildWithDefaults {
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erAnmodningOmUnntakSendtIkkeYrkesaktiv_ja() {
        val behandling = Behandling.buildWithDefaults {
            status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
            tema = Behandlingstema.IKKE_YRKESAKTIV
        }
        behandling.erRedigerbar().shouldBe(true)
    }

    @Test
    fun utledBehandlingsfrist_4Uker() {
        val behandling = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }

        val behandlingsfrist = behandling.utledBehandlingsfrist(utgangspunktDato)
        behandlingsfrist.shouldBe(utgangspunktDato.plusWeeks(6))
    }

    @Test
    fun utledBehandlingsfrist_8Uker() {
        val behandling = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandlingsfrist = behandling.utledBehandlingsfrist(utgangspunktDato)
        behandlingsfrist.shouldBe(utgangspunktDato.plusWeeks(8))
    }

    @Test
    fun utledBehandlingsfrist_70dager() {
        val behandling = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.KLAGE
        }

        val behandlingsfrist = behandling.utledBehandlingsfrist(utgangspunktDato)
        behandlingsfrist.shouldBe(utgangspunktDato.plusDays(70))
    }

    @Test
    fun utledBehandlingsfrist_90dager() {
        withClue("søknadsbehandlinger") {
            Behandling.buildWithDefaults {
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
            }.utledBehandlingsfrist(utgangspunktDato) shouldBe utgangspunktDato.plusDays(90)
        }

        withClue("anmodning unntak") {
            Behandling.buildWithDefaults {
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                type = Behandlingstyper.FØRSTEGANG
            }.utledBehandlingsfrist(utgangspunktDato) shouldBe utgangspunktDato.plusDays(90)
        }

        withClue("attester fra andre trygdeavtaleland") {
            Behandling.buildWithDefaults {
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.REGISTRERING_UNNTAK
                type = Behandlingstyper.FØRSTEGANG
            }.utledBehandlingsfrist(utgangspunktDato) shouldBe utgangspunktDato.plusDays(90)
        }

        withClue("henvendelser") {
            Behandling.buildWithDefaults {
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.HENVENDELSE
            }.utledBehandlingsfrist(utgangspunktDato) shouldBe utgangspunktDato.plusDays(90)
        }
    }

    @Test
    fun utledBehandlingsfrist_180dager() {
        withClue("Utstasjonering") {
            Behandling.buildWithDefaults {
                fagsak = FagsakTestFactory.builder().tema(Sakstemaer.UNNTAK).build()
                tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
                type = Behandlingstyper.NY_VURDERING
            }.utledBehandlingsfrist(utgangspunktDato) shouldBe utgangspunktDato.plusDays(180)
        }

        withClue("Øvrige") {
            Behandling.buildWithDefaults {
                fagsak = FagsakTestFactory.builder().tema(Sakstemaer.UNNTAK).build()
                tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
                type = Behandlingstyper.NY_VURDERING
            }.utledBehandlingsfrist(utgangspunktDato) shouldBe utgangspunktDato.plusDays(180)
        }
    }

    @Test
    fun saksopplysningerEksistererIkke_eksisterer_false() {
        val behandling = Behandling.buildWithDefaults {
            saksopplysninger = mutableSetOf(
                Saksopplysning().apply {
                    type = SaksopplysningType.PERSHIST
                }, Saksopplysning().apply {
                    type = SaksopplysningType.PERSOPL
                })
        }
        behandling.manglerSaksopplysningerAvType(
            listOf(
                SaksopplysningType.PDL_PERSOPL,
                SaksopplysningType.PERSOPL
            )
        ).shouldBe(false)
    }

    @Test
    fun saksopplysningerEksistererIkke_eksistererIkke_true() {
        val behandling = Behandling.buildWithDefaults {
            saksopplysninger = mutableSetOf(
                Saksopplysning().apply {
                    type = SaksopplysningType.PDL_PERSOPL
                }
            )
        }
        behandling.manglerSaksopplysningerAvType(
            listOf(
                SaksopplysningType.PDL_PERS_SAKS,
                SaksopplysningType.PERSHIST
            )
        ).shouldBe(true)
    }
}
