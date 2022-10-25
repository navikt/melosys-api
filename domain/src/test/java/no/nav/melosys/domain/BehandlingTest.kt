package no.nav.melosys.domain

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.List
import java.util.Set

internal class BehandlingTest {
    @Test
    fun erAktiv_underBehandling_ja() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.VURDER_DOKUMENT
        }
        behandling.erAktiv().shouldBe(true)
    }

    @Test
    fun erAktiv_avsluttet_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.AVSLUTTET
        }
        behandling.erAktiv().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erUnderBehandling_ja() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
        behandling.erRedigerbar().shouldBe(true)
    }

    @Test
    fun erRedigerbar_erAvsluttet_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.AVSLUTTET
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erMidlertidigLovvalgsbeslutning_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erIverksetterVedtak_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.IVERKSETTER_VEDTAK
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erAnmodningOmUnntakSendt_nei() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
        }
        behandling.erRedigerbar().shouldBe(false)
    }

    @Test
    fun erRedigerbar_erAnmodningOmUnntakSendtIkkeYrkesaktiv_ja() {
        val behandling = Behandling().apply {
            status = Behandlingsstatus.ANMODNING_UNNTAK_SENDT
            tema = Behandlingstema.IKKE_YRKESAKTIV
        }
        behandling.erRedigerbar().shouldBe(true)
    }

    @Test
    fun utledFristForBehandling_8Uker() {
        val behandlingsfrist = Behandling.utledFristForBehandling(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            Behandlingstyper.FØRSTEGANG
        )
        behandlingsfrist.shouldBe(LocalDate.now().plusWeeks(8))
    }

    @Test
    fun utledFristForBehandling_70dager() {
        val behandlingsfrist = Behandling.utledFristForBehandling(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            Behandlingstyper.KLAGE
        )
        behandlingsfrist.shouldBe(LocalDate.now().plusDays(70))
    }

    @Test
    fun utledFristForBehandling_90dager() {
        val behandlingsfrist_soknadsbehandlinger = Behandling.utledFristForBehandling(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstyper.FØRSTEGANG
        )
        val behandlingsfrist_anmodning_unntak = Behandling.utledFristForBehandling(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
            Behandlingstyper.FØRSTEGANG
        )
        val behandlingsfrist_attester_fra_andre_trygdeavtaleland = Behandling.utledFristForBehandling(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.REGISTRERING_UNNTAK,
            Behandlingstyper.FØRSTEGANG
        )
        val behandlingsfrist_henvendelser = Behandling.utledFristForBehandling(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstyper.HENVENDELSE
        )
        
        behandlingsfrist_soknadsbehandlinger.shouldBe(LocalDate.now().plusDays(90))
        behandlingsfrist_anmodning_unntak.shouldBe(LocalDate.now().plusDays(90))
        behandlingsfrist_attester_fra_andre_trygdeavtaleland.shouldBe(LocalDate.now().plusDays(90))
        behandlingsfrist_henvendelser.shouldBe(LocalDate.now().plusDays(90))
    }

    @Test
    fun utledFristForBehandling_180dager() {
        val behandlingsfrist = Behandling.utledFristForBehandling(
            Sakstemaer.UNNTAK,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            Behandlingstyper.NY_VURDERING
        )
        val behandlingsfrist_ovrige = Behandling.utledFristForBehandling(
            Sakstemaer.UNNTAK,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
            Behandlingstyper.NY_VURDERING
        )

        behandlingsfrist.shouldBe(LocalDate.now().plusDays(180))
        behandlingsfrist_ovrige.shouldBe(LocalDate.now().plusDays(180))
    }

    @Test
    fun saksopplysningerEksistererIkke_eksisterer_false() {
        val saksopplysning1 = Saksopplysning().apply {
            type = SaksopplysningType.PERSHIST
        }
        val saksopplysning2 = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
        }
        val behandling = Behandling().apply {
            saksopplysninger = setOf(saksopplysning1, saksopplysning2)
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
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PDL_PERSOPL
        }
        val behandling = Behandling().apply {
            saksopplysninger = Set.of(saksopplysning)
        }
        behandling.manglerSaksopplysningerAvType(
            List.of(
                SaksopplysningType.PDL_PERS_SAKS,
                SaksopplysningType.PERSHIST
            )
        ).shouldBe(true)
    }
}
