package no.nav.melosys.service.dokument;

public record DokumentproduksjonsInfo(String dokgenMalnavn,
                                      String dokumentKategoriKode,
                                      String journalføringsTittel,
                                      String alternativTittel,
                                      String attestTittel) {


    public DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel, String alternativTittel) {
        this(dokgenMalnavn, dokumentKategoriKode, journalføringsTittel, alternativTittel, null);
    }

    public DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel) {
        this(dokgenMalnavn, dokumentKategoriKode, journalføringsTittel, null, null);
    }
}
