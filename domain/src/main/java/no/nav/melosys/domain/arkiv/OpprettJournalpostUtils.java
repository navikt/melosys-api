package no.nav.melosys.domain.arkiv;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.eessi.SedType;

public final class OpprettJournalpostUtils {

    private static final String ARKIV = "ARKIV";
    private static final String SENTRAL_UTSKRIFT = "S";
    private static final String MEDLEMSKAP_OG_AVGIFT = "4530";
    private static final String DOKUMENT_KATEGORI_SED = "SED";
    private static final String UNNTAK_FRA_MEDLEMSKAP = "UFM";
    private static final String UTENLANDSK_ORGANISASJON = "UTL_ORG";

    private OpprettJournalpostUtils() {
        throw new IllegalStateException("Utility");
    }

    public static OpprettJournalpost lagJournalpostForSendingAvSedSomBrev(
        Long gsakSaksnummer, String brukerFnr, SedType sedType, byte[] sedPdf,
        String institusjonID, String institusjonNavn, String institusjonLand, List<FysiskDokument> vedlegg) {

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagFysiskDokument(sedType, sedPdf));
        opprettJournalpost.setVedlegg(vedlegg);
        opprettJournalpost.setArkivSakId(gsakSaksnummer.toString());
        opprettJournalpost.setMottaksKanal(SENTRAL_UTSKRIFT);
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(UNNTAK_FRA_MEDLEMSKAP);

        opprettJournalpost.setKorrespondansepartId(institusjonID);
        opprettJournalpost.setKorrespondansepartNavn(institusjonNavn);
        opprettJournalpost.setKorrespondansepartLand(institusjonLand);
        opprettJournalpost.setKorrespondansepartIdType(UTENLANDSK_ORGANISASJON);
        opprettJournalpost.setBrukerId(brukerFnr);

        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());

        return opprettJournalpost;
    }

    public static DokumentVariant lagArkivVariant(byte[] pdf) {
        DokumentVariant dokumentVariant = new DokumentVariant();
        dokumentVariant.setVariantFormat(ARKIV);
        dokumentVariant.setFiltype(DokumentVariant.Filtype.PDFA);
        dokumentVariant.setData(pdf);
        return dokumentVariant;
    }

    private static FysiskDokument lagFysiskDokument(SedType sedType, byte[] sedPdf) {
        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DOKUMENT_KATEGORI_SED);
        fysiskDokument.setTittel(hentTittelForSedType(sedType));
        fysiskDokument.setBrevkode(sedType.name());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagArkivVariant(sedPdf)));
        return fysiskDokument;
    }

    private static String hentTittelForSedType(SedType sedType) {
        switch (sedType) {
            case A002:
                return "Delvis eller fullt avslag på søknad om unntak";
            case A008:
                return "Melding om relevant informasjon";
            case A011:
                return "Innvilgelse av søknad om unntak";
            default:
                throw new IllegalArgumentException("Kan ikke opprette journalpost av sed-type " + sedType);
        }
    }
}
