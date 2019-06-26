package no.nav.melosys.tjenester.gui.dto.eessi;

import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;

public class SedDokumentDto {

    private String rinaSaksnummer;
    private String rinaDokumentID;
    private String fnr;
    private Periode lovvalgsperiode;
    private String lovvalgsbestemmelse;
    private String lovvalgslandKode;
    private boolean erEndring;
    private String sedType;
    private String bucType;
    
    public SedDokumentDto() {
    }

    private SedDokumentDto(String rinaSaksnummer, String rinaDokumentID, String fnr, Periode lovvalgsperiode, String lovvalgsbestemmelse, String lovvalgslandKode, boolean erEndring, String sedType, String bucType) {
        this.rinaSaksnummer = rinaSaksnummer;
        this.rinaDokumentID = rinaDokumentID;
        this.fnr = fnr;
        this.lovvalgsperiode = lovvalgsperiode;
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
        this.lovvalgslandKode = lovvalgslandKode;
        this.erEndring = erEndring;
        this.sedType = sedType;
        this.bucType = bucType;
    }

    public static SedDokumentDto fra(SedDokument dokument) {
        return new SedDokumentDto(dokument.getRinaSaksnummer(),
            dokument.getRinaDokumentID(),
            dokument.getFnr(),
            dokument.getLovvalgsperiode(),
            dokument.getLovvalgBestemmelse() != null ? dokument.getLovvalgBestemmelse().getKode() : null,
            dokument.getLovvalgslandKode() != null ? dokument.getLovvalgslandKode().getKode() : null,
            dokument.getErEndring(),
            dokument.getSedType() != null ? dokument.getSedType().name() : null,
            dokument.getBucType() != null ? dokument.getBucType().name() : null
        );
    }

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public String getRinaDokumentID() {
        return rinaDokumentID;
    }

    public String getFnr() {
        return fnr;
    }

    public Periode getLovvalgsperiode() {
        return lovvalgsperiode;
    }

    public String getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public String getLovvalgslandKode() {
        return lovvalgslandKode;
    }

    public boolean isErEndring() {
        return erEndring;
    }

    public String getSedType() {
        return sedType;
    }

    public String getBucType() {
        return bucType;
    }
}
