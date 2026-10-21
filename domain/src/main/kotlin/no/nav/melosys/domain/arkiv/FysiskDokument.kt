package no.nav.melosys.domain.arkiv

import no.nav.melosys.domain.eessi.SedType

class FysiskDokument : ArkivDokument() {

    var brevkode: String? = null
    var dokumentKategori: String? = null

    companion object {
        private const val DOKUMENT_KATEGORI_SED = "SED"
        private const val DOKUMENT_KATEGORI_SOKNAD = "SOK"

        @JvmStatic
        fun lagFysiskDokumentSed(sedType: SedType, sedPdf: ByteArray) = FysiskDokument().apply {
            dokumentKategori = DOKUMENT_KATEGORI_SED
            tittel = hentTittelForSedType(sedType)
            brevkode = sedType.name
            addDokumentVariant(DokumentVariant.lagDokumentVariant(sedPdf))
        }

        @JvmStatic
        fun lagFysiskDokument(bestilling: JournalpostBestilling) = FysiskDokument().apply {
            dokumentKategori = bestilling.dokumentKategori
            tittel = bestilling.tittel
            brevkode = bestilling.brevkode
            addDokumentVariant(DokumentVariant.lagDokumentVariant(bestilling.pdf))
        }

        @JvmStatic
        fun lagFysiskDokumentListeFraVedlegg(
            journalpostBestilling: JournalpostBestilling,
            vedleggListe: List<Vedlegg>?
        ): List<FysiskDokument>? = vedleggListe?.map { vedlegg ->
            FysiskDokument().apply {
                tittel = vedlegg.tittel
                brevkode = journalpostBestilling.brevkode
                dokumentKategori = journalpostBestilling.dokumentKategori
                addDokumentVariant(DokumentVariant.lagDokumentVariant(vedlegg.innhold))
            }
        }

        private fun hentTittelForSedType(sedType: SedType): String = when (sedType) {
            SedType.A002 -> "Delvis eller fullt avslag på søknad om unntak"
            SedType.A003 -> "Beslutning om lovvalg"
            SedType.A008 -> "Melding om relevant informasjon"
            SedType.A011 -> "Innvilgelse av søknad om unntak"
            else -> throw IllegalArgumentException("Kan ikke opprette journalpost av sed-type $sedType")
        }

        @JvmStatic
        fun lagFysiskDokumentDigitalSøknad(pdf: ByteArray, tittel: String) = FysiskDokument().apply {
            dokumentKategori = DOKUMENT_KATEGORI_SOKNAD
            this.tittel = tittel
            addDokumentVariant(DokumentVariant.lagDokumentVariant(pdf))
        }
    }
}
