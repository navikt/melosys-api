package no.nav.melosys.domain

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFailsWith

internal class FagsakTest {

    @Test
    fun getAktivBehandling() {
        val b1 = lagBehandling(Behandlingsstatus.AVSLUTTET)
        val b2 = lagBehandling(Behandlingsstatus.UNDER_BEHANDLING)
        val b3 = lagBehandling(Behandlingsstatus.AVSLUTTET)
        val behandlinger = listOf(b1, b2, b3)
        val fagsak = FagsakTestFactory.builder().behandlinger(behandlinger).build()

        val aktivBehandling = fagsak.finnAktivBehandling()

        aktivBehandling.shouldBe(b2)
    }

    @Test
    fun hentTidligsteInaktivBehandling_toInaktive() {
        val tidligsteInaktiveBehandling = lagBehandling(Behandlingsstatus.AVSLUTTET).apply {
            registrertDato = Instant.parse("2019-01-10T10:37:30.00Z")
        }
        val aktivBehandling = lagBehandling(Behandlingsstatus.UNDER_BEHANDLING)
        val seinesteInaktiveBehandling = lagBehandling(Behandlingsstatus.AVSLUTTET).apply {
            registrertDato = Instant.parse("2019-02-10T10:37:30.00Z")
        }
        val behandlinger = listOf(tidligsteInaktiveBehandling, aktivBehandling, seinesteInaktiveBehandling)
        val fagsak = FagsakTestFactory.builder().behandlinger(behandlinger).build()

        fagsak.hentTidligstInaktivBehandling().shouldBe(tidligsteInaktiveBehandling)
    }

    @Test
    fun getSistOppdaterteBehandling_medEnBehandling() {
        val behandling = Behandling().apply {
            endretDato = Instant.parse("2019-01-10T10:37:30.00Z")
        }
        val fagsak = FagsakTestFactory.builder().behandlinger(behandling).build()

        fagsak.hentSistOppdatertBehandling().shouldBe(behandling)
    }

    @Test
    fun getSistOppdaterteBehandling_medTreBehandlinger() {
        val sistOppdaterteBehandling = Behandling().apply {
            endretDato = Instant.parse("2019-01-10T10:37:30.00Z")
        }
        val behandling1 = Behandling().apply {
            endretDato = Instant.parse("2019-01-10T10:36:30.00Z")
        }
        val behandling2 = Behandling().apply {
            endretDato = Instant.parse("2019-01-09T10:37:30.00Z")
        }
        val fagsak = FagsakTestFactory.builder().behandlinger(
            listOf(
                sistOppdaterteBehandling,
                behandling1,
                behandling2
            )
        ).build()

        fagsak.hentSistOppdatertBehandling().shouldBe(sistOppdaterteBehandling)
    }

    @Test
    fun hentBehandlingerSortertPåRegistertDato_medToBehandlinger_sortertRiktig() {
        val behandling1 = Behandling().apply {
            setRegistrertDato(Instant.parse("2020-01-01T00:00:00Z"))
        }
        val behandling2 = Behandling().apply {
            setRegistrertDato(Instant.parse("2021-01-01T00:00:00Z"))
        }
        val fagsak = FagsakTestFactory.builder()
            .behandlinger(listOf(behandling1, behandling2))
            .build()

        val registrerteDatoer = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato()
            .map { it.getRegistrertDato() }

        registrerteDatoer.shouldBe(listOf(behandling2.registrertDato, behandling1.registrertDato))
    }

    @Test
    fun hentSistOppdatertBehandling_medToBehandlinger_returnerNyeste() {
        val behandling1 = Behandling().apply {
            setRegistrertDato(Instant.parse("2020-01-01T00:00:00Z"))
        }
        val behandling2 = Behandling().apply {
            setRegistrertDato(Instant.parse("2021-01-01T00:00:00Z"))
        }
        val fagsak = FagsakTestFactory.builder().behandlinger(listOf(behandling1, behandling2)).build()

        fagsak.hentSistRegistrertBehandling().getRegistrertDato().shouldBe(behandling2.getRegistrertDato())
    }

    @Test
    fun getSistOppdaterteBehandling_ingenBehandlinger_kasterException() {
        val fagsak = FagsakTestFactory.lagFagsak()

        assertFailsWith<FunksjonellException> { fagsak.hentSistOppdatertBehandling() }
            .shouldHaveMessage("Finner ikke behandlinger for fagsak ${fagsak.saksnummer}")
    }

    @Test
    fun getAktivBehandling_ingenAktive() {
        val b1 = lagBehandling(Behandlingsstatus.AVSLUTTET)
        val b2 = lagBehandling(Behandlingsstatus.AVSLUTTET)
        val behandlinger = listOf(b1, b2)
        val fagsak = FagsakTestFactory.builder().behandlinger(behandlinger).build()

        val aktivBehandling = fagsak.finnAktivBehandling()

        aktivBehandling.shouldBeNull()
    }

    @Test
    fun getAktivBehandling_feilTilstand() {
        val b1 = lagBehandling(Behandlingsstatus.AVVENT_DOK_PART)
        val b2 = lagBehandling(Behandlingsstatus.UNDER_BEHANDLING)
        val behandlinger = listOf(b1, b2)
        val fagsak = FagsakTestFactory.builder().behandlinger(behandlinger).build()

        assertFailsWith<TekniskException> { fagsak.hentAktivBehandling() }
            .shouldHaveMessage("Det finnes mer enn en aktiv behandling for sak ${fagsak.saksnummer}")
    }

    @Test
    fun getBruker() {
        val a1 = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
            aktørId = "123"
        }
        val a2 = Aktoer().apply {
            rolle = Aktoersroller.ARBEIDSGIVER
            aktørId = "456"
        }
        val fagsak = FagsakTestFactory.builder().aktører(setOf(a1, a2)).build()

        val bruker = fagsak.hentBruker()

        bruker.shouldBe(a1)
    }

    @Test
    fun getBruker_ingen() {
        val a2 = Aktoer().apply {
            rolle = Aktoersroller.ARBEIDSGIVER
            aktørId = "456"
        }
        val fagsak = FagsakTestFactory.builder().aktører(a2).build()

        val bruker = fagsak.hentBruker()

        bruker.shouldBeNull()
    }

    @Test
    fun getBruker_flere() {
        val a1 = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
            aktørId = "123"
        }
        val a2 = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
            aktørId = "456"
        }
        val fagsak = FagsakTestFactory.builder().aktører(setOf(a1, a2)).build()

        assertFailsWith<TekniskException> { fagsak.hentBruker() }
            .shouldHaveMessage("Det finnes mer enn en aktør med rollen Bruker for sak ${fagsak.saksnummer}")
    }

    @Test
    fun finnFullmektig_arbeidsgiver_funker() {
        val a1 = Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
            aktørId = "123"
        }
        val a2 = Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            aktørId = "456"
        }
        val fagsak = FagsakTestFactory.builder().aktører(setOf(a1, a2)).build()

        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)

        fullmektig.shouldBe(a2)
    }

    @Test
    fun finnFullmektig_bruker_funker() {
        val a1 = Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
            aktørId = "123"
        }
        val a2 = Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            aktørId = "456"
        }
        val fagsak = FagsakTestFactory.builder().aktører(setOf(a1, a2)).build()

        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD)

        fullmektig.shouldBe(a1)
    }

    @Test
    fun hentMyndighetLandkode_forventGyldigLandkode() {
        val aktoer = Aktoer().apply {
            rolle = Aktoersroller.TRYGDEMYNDIGHET
            institusjonID = "SE:gfr"
        }
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()

        val resultat = fagsak.hentMyndighetLandkode()

        resultat.shouldBe(Land_iso2.SE)
    }

    @Test
    fun hentMyndighetLandkode_aktoerIkkeMyndighet_forventTekniskException() {
        val aktoer = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
            institusjonID = "SE:gfr"
        }
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()

        assertFailsWith<TekniskException> { fagsak.hentMyndighetLandkode() }
            .shouldHaveMessage("Finner ingen aktør med rolle TRYGDEMYNDIGHET for fagsak ${fagsak.saksnummer}")
    }

    @Test
    fun harAktørMedRolleTypeArbeidsgiver_arbeidsgiverFinnes_forventTrue() {
        val aktoer = Aktoer().apply {
            rolle = Aktoersroller.ARBEIDSGIVER
        }
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()

        fagsak.harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER).shouldBeTrue()
    }

    @Test
    fun harAktørMedRolleTypeArbeidsgiver_kunBruker_forventFalse() {
        val aktoer = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
        }
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()

        fagsak.harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER).shouldBeFalse()
    }

    private fun lagBehandling(behandlingsstatus: Behandlingsstatus): Behandling =
        Behandling().apply { status = behandlingsstatus }
}
