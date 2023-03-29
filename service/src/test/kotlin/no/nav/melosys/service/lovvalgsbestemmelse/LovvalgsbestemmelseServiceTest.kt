package no.nav.melosys.service.lovvalgsbestemmelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test

internal class LovvalgsbestemmelseServiceTest {

    private val lovvalgsbestemmelseService = LovvalgsbestemmelseService()

    @Test
    fun henLovvalgsperioder_medlemskaplovvalgYrkesaktivGB_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.GB
        ).shouldBe(
            setOf(
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_5,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_1,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART9
            )
        )
    }

    @Test
    fun henLovvalgsperioder_medlemskaplovvalgIkkeyrkesaktivGB_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.IKKE_YRKESAKTIV, Land_iso2.GB
        ).shouldBe(
            setOf(
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART5_4,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_2,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_5,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART9
            )
        )
    }

    @Test
    fun henLovvalgsperioder_unntakRegistreringUnntakGB_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK, Land_iso2.GB
        ).shouldBe(
            setOf(
                Lovvalgsbestemmelser_trygdeavtale_gb.UK,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART5_4,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_10,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_1,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART9
            )
        )
    }

    @Test
    fun henLovvalgsperioder_medlemskaplovvalgYrkesaktivUS_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.US
        ).shouldBe(
            setOf(
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_5,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9
            )
        )
    }

    @Test
    fun henLovvalgsperioder_unntakAnmodningomunntakhovedregelCA_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstemaer.UNNTAK, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL, Land_iso2.CA
        ).shouldBe(
            setOf(
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART9,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11
            )
        )
    }

    @Test
    fun henLovvalgsperioder_medlemskaplovvalgYrkesaktivFR_tomListe() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.FR
        ).shouldBe(
            setOf()
        )
    }

    @Test
    fun henLovvalgsperioder_medlemskaplovvalgRegistreringunntakGB_ugyldigKombinasjon() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.REGISTRERING_UNNTAK, Land_iso2.GB
            )
        }.message.shouldContain("Kan ikke mappe lovvalgsbestemmelser")
    }

    @Test
    fun henLovvalgsperioder_medlemskaplovvalgYkresaktivNO_støtterIkkeLandkode() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.NO
            )
        }.message.shouldContain("Støtter ikke mapping til lovvalgsbestemmelse for land")
    }
}
