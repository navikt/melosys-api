package no.nav.melosys.saksflytapi.journalfoering

import no.nav.melosys.domain.brev.utkast.KopiMottakerUtkast
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import java.time.LocalDate

data class VedtakRequest(
    val behandlingsresultatTypeKode: Behandlingsresultattyper,
    val vedtakstype: Vedtakstyper,
    val fritekst: String?,
    val fritekstSed: String?,
    val mottakerinstitusjoner: Set<String>?,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
    val ektefelleFritekst: String?,
    val barnFritekst: String?,
    val trygdeavgiftFritekst: String?,
    val kopiMottakere: List<KopiMottaker>?,
    val kopiTilArbeidsgiver: Boolean?,
    val bestillersId: String,
    val nyVurderingBakgrunn: String?,
    val betalingsintervall: String?,
    val opphørtDato: LocalDate?
)

@JvmRecord
data class KopiMottaker(
    val rolle: Mottakerroller,
    val orgnr: String?,
    val aktørId: String?,
    val institusjonID: String?
) {
    fun tilUtkast(): KopiMottakerUtkast {
        return KopiMottakerUtkast(rolle, orgnr, aktørId, institusjonID)
    }

    companion object {
        fun av(kopiMottakerUtkast: KopiMottakerUtkast): KopiMottaker {
            return KopiMottaker(kopiMottakerUtkast.rolle, kopiMottakerUtkast.orgnr, kopiMottakerUtkast.aktørID, kopiMottakerUtkast.institusjonID)
        }
    }
}
