package no.nav.melosys.domain.arkiv

import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.msm.AltinnDokument
import java.nio.charset.StandardCharsets
import java.util.*

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
        fun lagFysiskHovedDokumentAltinn(
            altinnDokument: AltinnDokument,
            mottatteOpplysninger: MottatteOpplysninger
        ) = FysiskDokument().apply {
            dokumentKategori = DOKUMENT_KATEGORI_SOKNAD
            tittel = hentTittelForAltinnDokument(altinnDokument.dokumentType)
            val innhold = Base64.getDecoder().decode(altinnDokument.innhold)
            dokumentVarianter = listOf(
                DokumentVariant.lagDokumentVariant(innhold),
                DokumentVariant.lagDokumentVariant(
                    mottatteOpplysninger.originalData.toByteArray(StandardCharsets.UTF_8),
                    DokumentVariant.Filtype.XML,
                    DokumentVariant.VariantFormat.ORIGINAL
                )
            )
        }

        @JvmStatic
        fun lagFysiskDokumentAltinn(altinnDokument: AltinnDokument) = FysiskDokument().apply {
            dokumentKategori = DOKUMENT_KATEGORI_SOKNAD
            tittel = hentTittelForAltinnDokument(altinnDokument.dokumentType)
            val innhold = Base64.getDecoder().decode(altinnDokument.innhold)
            addDokumentVariant(DokumentVariant.lagDokumentVariant(innhold))
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

        private fun hentTittelForAltinnDokument(dokumentType: AltinnDokument.AltinnDokumentType): String = when (dokumentType) {
            AltinnDokument.AltinnDokumentType.SOKNAD -> "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
            AltinnDokument.AltinnDokumentType.FULLMAKT -> "Fullmakt"
        }
    }
}
