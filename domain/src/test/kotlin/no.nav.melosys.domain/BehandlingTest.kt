package no.nav.melosys.domain

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

        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling, utgangspunktDato
        )
        behandlingsfrist.shouldBe(utgangspunktDato.plusWeeks(6))
    }

    @Test
    fun utledBehandlingsfrist_8Uker() {
        val behandling = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling, utgangspunktDato
        )
        behandlingsfrist.shouldBe(utgangspunktDato.plusWeeks(8))
    }

    @Test
    fun utledBehandlingsfrist_70dager() {
        val behandling = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            type = Behandlingstyper.KLAGE
        }

        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling, utgangspunktDato
        )
        behandlingsfrist.shouldBe(utgangspunktDato.plusDays(70))
    }

    @Test
    fun utledBehandlingsfrist_90dager() {
        val behandling_soknadsbehandlinger = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandling_anmodning_unntak = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandling_attester_fra_andre_trygdeavtaleland = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.REGISTRERING_UNNTAK
            type = Behandlingstyper.FØRSTEGANG
        }

        val behandling_henvendelser = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.HENVENDELSE
        }

        val behandlingsfrist_soknadsbehandlinger = Behandling.utledBehandlingsfrist(
            behandling_soknadsbehandlinger, utgangspunktDato
        )
        val behandlingsfrist_anmodning_unntak = Behandling.utledBehandlingsfrist(
            behandling_anmodning_unntak, utgangspunktDato
        )
        val behandlingsfrist_attester_fra_andre_trygdeavtaleland = Behandling.utledBehandlingsfrist(
            behandling_attester_fra_andre_trygdeavtaleland, utgangspunktDato
        )
        val behandlingsfrist_henvendelser = Behandling.utledBehandlingsfrist(
            behandling_henvendelser, utgangspunktDato
        )

        behandlingsfrist_soknadsbehandlinger.shouldBe(utgangspunktDato.plusDays(90))
        behandlingsfrist_anmodning_unntak.shouldBe(utgangspunktDato.plusDays(90))
        behandlingsfrist_attester_fra_andre_trygdeavtaleland.shouldBe(utgangspunktDato.plusDays(90))
        behandlingsfrist_henvendelser.shouldBe(utgangspunktDato.plusDays(90))
    }

    @Test
    fun utledBehandlingsfrist_180dager() {
        val behandling_utstasjonering = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.builder().tema(Sakstemaer.UNNTAK).build()
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
            type = Behandlingstyper.NY_VURDERING
        }
        val behandling_ovrige = Behandling.buildWithDefaults {
            fagsak = FagsakTestFactory.builder().tema(Sakstemaer.UNNTAK).build()
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
            type = Behandlingstyper.NY_VURDERING
        }
        val behandlingsfrist = Behandling.utledBehandlingsfrist(
            behandling_utstasjonering, utgangspunktDato
        )
        val behandlingsfrist_ovrige = Behandling.utledBehandlingsfrist(
            behandling_ovrige, utgangspunktDato
        )

        behandlingsfrist.shouldBe(utgangspunktDato.plusDays(180))
        behandlingsfrist_ovrige.shouldBe(utgangspunktDato.plusDays(180))
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
