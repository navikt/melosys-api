package no.nav.melosys.service.lovvalgsbestemmelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test

internal class LovvalgsbestemmelseServiceTest {

    private val lovvalgsbestemmelseService = LovvalgsbestemmelseService()

    @Test
    fun henLovvalgsperioder_eueosIkkeyrkesaktiv_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.EU_EOS, null, Behandlingstema.IKKE_YRKESAKTIV, null
        ).shouldBe(
            setOf(
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            )
        )
    }

    @Test
    fun henLovvalgsperioder_trygdeavtaleMedlemskaplovvalgYrkesaktivGB_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.GB
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
    fun henLovvalgsperioder_trygdeavtaleMedlemskaplovvalgIkkeyrkesaktivGB_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.IKKE_YRKESAKTIV, Land_iso2.GB
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
    fun henLovvalgsperioder_trygdeavtaleUnntakRegistreringUnntakGB_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK, Land_iso2.GB
        ).shouldBe(
            setOf(
                Lovvalgsbestemmelser_trygdeavtale_gb.UK,
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
    fun henLovvalgsperioder_trygdeavtaleMedlemskaplovvalgYrkesaktivUS_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.US
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
    fun henLovvalgsperioder_trygdeavtaleUnntakAnmodningomunntakhovedregelCA_liste() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL, Land_iso2.CA
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
    fun henLovvalgsperioder_trygdeavtaleMedlemskaplovvalgYrkesaktivFR_tomListe() {
        lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.FR
        ).shouldBe(
            setOf()
        )
    }

    @Test
    fun henLovvalgsperioder_eueosYrkesaktiv_ugyldigBehandlingstema() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstyper.EU_EOS, null, Behandlingstema.YRKESAKTIV, null
            )
        }.message.shouldContain("Støtter ikke henting av lovvalgsbestemmelser for behandlingstema")
    }

    @Test
    fun henLovvalgsperioder_ftrlYrkesaktiv_ugyldigSakstype() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstyper.FTRL, null, Behandlingstema.YRKESAKTIV, null
            )
        }.message.shouldContain("Støtter ikke å hente lovvalgsbestemmelse for sakstype")
    }

    @Test
    fun henLovvalgsperioder_trygdeavtaleUnntakRegistreringunntakNull_manglerLand() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.REGISTRERING_UNNTAK, null
            )
        }.message.shouldContain("Sakstema og land kan ikke være null for sakstype trygdeavtale")
    }

    @Test
    fun henLovvalgsperioder_trygdeavtaleNullRegistreringunntakGb_manglerSakstema() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstyper.TRYGDEAVTALE, null, Behandlingstema.REGISTRERING_UNNTAK, Land_iso2.GB
            )
        }.message.shouldContain("Sakstema og land kan ikke være null for sakstype trygdeavtale")
    }

    @Test
    fun henLovvalgsperioder_trygdeavtaleMedlemskaplovvalgRegistreringunntakGB_ugyldigKombinasjon() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.REGISTRERING_UNNTAK, Land_iso2.GB
            )
        }.message.shouldContain("Kan ikke mappe lovvalgsbestemmelser")
    }

    @Test
    fun henLovvalgsperioder_trygdeavtaleMedlemskaplovvalgYkresaktivNO_støtterIkkeLandkode() {
        shouldThrow<FunksjonellException> {
            lovvalgsbestemmelseService.hentLovvalgsbestemmelser(
                Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV, Land_iso2.NO
            )
        }.message.shouldContain("Støtter ikke mapping til lovvalgsbestemmelse for land")
    }
}
