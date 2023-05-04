package no.nav.melosys.service.oppgave

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.service.oppgave.OppgaveGosysMapping.Companion.NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING

class OppgaveGosysMappingTest {

    private val oppgaveGosysMapping = OppgaveGosysMapping(FakeUnleash())

    @Test
    fun `skal kun ha ett treff på alle mulige kombinasjoner av sakstype, sakstema, behandlingstype og behandlingstema`() {
        oppgaveGosysMapping.rows.forEach { row ->
            row.behandlingstema.forEach { behandlingstema ->
                row.behandlingstype.forEach { behandlingstyper ->
                    oppgaveGosysMapping.rows.filter {
                        it.sakstype == row.sakstype &&
                            it.sakstema == row.sakstema &&
                            behandlingstyper in it.behandlingstype &&
                            behandlingstema in it.behandlingstema
                    }.shouldHaveSize(1)
                }
            }
        }
    }

    @Test
    fun `sjekk at gyldige melosys kombinasjoner funger når vi lager gosys oppgave`() {
        GyldigeKombinasjoner.rows.forEach {
            oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
        }
    }

    @Test
    fun `tillat disse kun for migrering`() {
        val ulovligKobo = { mapper: OppgaveGosysMapping, behandlingstype: Behandlingstyper ->
            mapper.finnOppgave(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                Behandlingstema.TRYGDETID,
                behandlingstype
            )
        }
        OppgaveGosysMapping(FakeUnleash()).apply {
            shouldThrow<IllegalStateException> {
                ulovligKobo(this, Behandlingstyper.FØRSTEGANG)
            }.message.shouldBe("Fant ikke oppgave mapping for sakstype:EU_EOS, sakstema:MEDLEMSKAP_LOVVALG, behandlingstema:TRYGDETID, behandlingstype:FØRSTEGANG")

            shouldThrow<IllegalStateException> {
                ulovligKobo(this, Behandlingstyper.NY_VURDERING)
            }.message.shouldBe("Fant ikke oppgave mapping for sakstype:EU_EOS, sakstema:MEDLEMSKAP_LOVVALG, behandlingstema:TRYGDETID, behandlingstype:NY_VURDERING")
        }

        OppgaveGosysMapping(FakeUnleash().apply { enable(NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING) }).apply {
            ulovligKobo(this, Behandlingstyper.FØRSTEGANG).oppgaveBehandlingstema.shouldBe(OppgaveBehandlingstema.EU_EOS_FORESPORSEL_OM_TRYGDETID)
            ulovligKobo(this, Behandlingstyper.NY_VURDERING).oppgaveBehandlingstema.shouldBe(OppgaveBehandlingstema.EU_EOS_FORESPORSEL_OM_TRYGDETID)
        }
    }
    @Test
    fun `kombo TRYGDEAVTALE,  UNNTAK, HENVENDELSE, FORESPØRSEL_TRYGDEMYNDIGHET skal gi tema Unntak fra medlemskap`() {
        val oppgave = oppgaveGosysMapping.finnOppgave(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            Behandlingstyper.HENVENDELSE
        )

        oppgave.apply {
            oppgaveBehandlingstema.shouldBeNull()
            tema.shouldBe(Tema.UFM)
            oppgaveType.shouldBe(Oppgavetyper.BEH_SAK_MK)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.TOMT)
        }
    }
}
