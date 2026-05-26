package no.nav.melosys.integrasjon.popp

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.Date

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PoppHentInntektRequest(
    val fnr: String,
    val fomAr: Int? = null,
    val tomAr: Int? = null,
    val inntektType: String? = null,
    val ekskluderInntektType: String? = null,
)

data class PoppHentInntektResponse(
    val inntekter: List<PoppInntektPost>? = null,
)

data class PoppInntektPost(
    val changeStamp: PoppChangeStamp? = null,
    val inntektId: Long? = null,
    val fnr: String? = null,
    val inntektAr: Int? = null,
    val kilde: String? = null,
    val kommune: String? = null,
    val piMerke: String? = null,
    val inntektType: String? = null,
    val inntektTypeDekode: String? = null,
    val belop: Long? = null,
)

data class PoppChangeStamp(
    val createdBy: String? = null,
    val createdDate: Date? = null,
    val updatedBy: String? = null,
    val updatedDate: Date? = null,
)

class PoppPersonIkkeFunnetException(message: String) : RuntimeException(message)
