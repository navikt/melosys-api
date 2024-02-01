package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast
import no.nav.melosys.domain.brev.utkast.KopiMottakerUtkast
import no.nav.melosys.domain.brev.utkast.Utkast.FritekstVedlegg
import no.nav.melosys.domain.brev.utkast.Utkast.Saksvedlegg
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import no.nav.melosys.service.dokument.brev.SaksvedleggDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import java.time.LocalDate
import java.util.*
import java.util.function.Function

@JvmRecord
data class BrevbestillingRequest(
    @JvmField val produserbardokument: Produserbaredokumenter,
    @JvmField val mottaker: Mottakerroller,
    @JvmField val orgNr: String,
    @JvmField val institusjonID: String,
    @JvmField val orgnrNorskMyndighet: List<String>,
    @JvmField val innledningFritekst: String,
    @JvmField val manglerFritekst: String,
    @JvmField val begrunnelseFritekst: String,
    @JvmField val ektefelleFritekst: String,
    @JvmField val barnFritekst: String,
    @JvmField val trygdeavgiftFritekst: String,
    @JvmField val kontaktpersonNavn: String,
    @JvmField val kopiMottakere: List<KopiMottakerDto>,
    @JvmField val fritekstTittel: String,
    @JvmField val fritekst: String,
    @JvmField val distribusjonstype: Distribusjonstype,
    @JvmField val kontaktopplysninger: Boolean,
    @JvmField val nyVurderingBakgrunn: String,
    @JvmField val saksvedlegg: List<SaksvedleggDto>,
    @JvmField val fritekstvedlegg: List<FritekstvedleggDto>,
    @JvmField val dokumentTittel: String,
    @JvmField val saksbehandlerNrToIdent: String,
    val begrunnelseKode: String?,
    val ytterligereInformasjon: String?,
    val opphoerDato: LocalDate?
) {
    @JvmOverloads
    fun tilBrevbestillingDto(bestillersId: String? = SubjectHandler.getInstance().userID): BrevbestillingDto {
        return BrevbestillingDto(
            this.produserbardokument,
            this.mottaker,
            this.orgNr,
            this.orgnrNorskMyndighet,
            this.institusjonID,
            this.innledningFritekst,
            this.manglerFritekst,
            this.begrunnelseFritekst,
            this.ektefelleFritekst,
            this.barnFritekst,
            this.trygdeavgiftFritekst,
            this.kontaktpersonNavn,
            this.kopiMottakere,
            bestillersId,
            this.fritekstTittel,
            this.fritekst,
            this.distribusjonstype,
            this.kontaktopplysninger,
            this.nyVurderingBakgrunn,
            this.saksvedlegg,
            this.fritekstvedlegg,
            this.dokumentTittel,
            this.saksbehandlerNrToIdent,
            this.begrunnelseKode,
            this.ytterligereInformasjon,
            null,
            null,
            null,
            null,
            null,
            this.opphoerDato
        )
    }

    fun tilUtkast(): BrevbestillingUtkast {
        return BrevbestillingUtkast(
            this.produserbardokument,
            this.mottaker,
            this.orgNr,
            this.orgnrNorskMyndighet,
            this.institusjonID,
            this.innledningFritekst,
            this.manglerFritekst,
            this.begrunnelseFritekst,
            this.ektefelleFritekst,
            this.barnFritekst,
            this.trygdeavgiftFritekst,
            this.kontaktpersonNavn,
            konverterListeTil(this.kopiMottakere) { obj: KopiMottakerDto -> obj.tilUtkast() },
            this.fritekstTittel,
            this.fritekst,
            this.distribusjonstype,
            this.kontaktopplysninger,
            this.nyVurderingBakgrunn,
            konverterListeTil(this.saksvedlegg) { obj: SaksvedleggDto -> obj.tilUtkast() },
            konverterListeTil(this.fritekstvedlegg) { obj: FritekstvedleggDto -> obj.tilUtkast() },
            this.dokumentTittel,
            this.saksbehandlerNrToIdent
        )
    }

    private fun <T, R> konverterListeTil(liste: List<T>, mapper: Function<T, R>): List<R> {
        return Optional.ofNullable(liste)
            .orElseGet { emptyList() }
            .stream()
            .map(mapper)
            .toList()
    }

    companion object {
        @JvmStatic
        fun av(utkast: BrevbestillingUtkast): BrevbestillingRequest {
            return BrevbestillingRequest(
                utkast.produserbardokument,
                utkast.mottaker,
                utkast.orgnr,
                utkast.institusjonID,
                utkast.orgnrNorskMyndighet,
                utkast.innledningFritekst,
                utkast.manglerFritekst,
                utkast.begrunnelseFritekst,
                utkast.ektefelleFritekst,
                utkast.barnFritekst,
                utkast.trygdeavgiftFritekst,
                utkast.kontaktpersonNavn,
                utkast.kopiMottakere.stream()
                    .map { kopiMottakerUtkast: KopiMottakerUtkast? -> KopiMottakerDto.av(kopiMottakerUtkast) }
                    .toList(),
                utkast.fritekstTittel,
                utkast.fritekst,
                utkast.distribusjonstype,
                utkast.kontaktopplysninger,
                utkast.nyVurderingBakgrunn,
                utkast.saksVedlegg.stream().map { saksvedlegg: Saksvedlegg? -> SaksvedleggDto.av(saksvedlegg) }
                    .toList(),
                utkast.fritekstVedlegg.stream()
                    .map { fritekstVedlegg: FritekstVedlegg? -> FritekstvedleggDto.av(fritekstVedlegg) }
                    .toList(),
                utkast.dokumentTittel,
                utkast.saksbehandlerNrToIdent,
                null,
                null,
                null
            )
        }
    }
}
