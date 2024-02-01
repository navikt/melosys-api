package no.nav.melosys.tjenester.gui.dto.eessi

import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument

class SedDokumentDto {
    var rinaSaksnummer: String? = null
    var rinaDokumentID: String? = null
    var fnr: String? = null
    var lovvalgsperiode: Periode? = null
    var lovvalgsbestemmelse: String? = null
    var lovvalgslandKode: String? = null
    var isErEndring: Boolean = false
    var sedType: String? = null
    var bucType: String? = null

    constructor()

    private constructor(
        rinaSaksnummer: String,
        rinaDokumentID: String,
        fnr: String,
        lovvalgsperiode: Periode,
        lovvalgsbestemmelse: String?,
        lovvalgslandKode: String?,
        erEndring: Boolean,
        sedType: String?,
        bucType: String?
    ) {
        this.rinaSaksnummer = rinaSaksnummer
        this.rinaDokumentID = rinaDokumentID
        this.fnr = fnr
        this.lovvalgsperiode = lovvalgsperiode
        this.lovvalgsbestemmelse = lovvalgsbestemmelse
        this.lovvalgslandKode = lovvalgslandKode
        this.isErEndring = erEndring
        this.sedType = sedType
        this.bucType = bucType
    }

    companion object {
        @JvmStatic
        fun fra(dokument: SedDokument): SedDokumentDto {
            return SedDokumentDto(
                dokument.rinaSaksnummer,
                dokument.rinaDokumentID,
                dokument.fnr,
                dokument.lovvalgsperiode,
                if (dokument.lovvalgBestemmelse != null) dokument.lovvalgBestemmelse.kode else null,
                if (dokument.lovvalgslandKode != null) dokument.lovvalgslandKode.kode else null,
                dokument.erEndring,
                if (dokument.sedType != null) dokument.sedType.name else null,
                if (dokument.bucType != null) dokument.bucType.name else null
            )
        }
    }
}
