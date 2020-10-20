package no.nav.melosys.service.journalforing.dto;

@Deprecated(forRemoval = true)
public class AnmodningOmUnntakDto {
    private String lovvalgsbestemmelse;
    private String unntakFraLovvalgsland;
    private String unntakFraLovvalgsbestemmelse;

    public String getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public void setLovvalgsbestemmelse(String lovvalgsbestemmelse) {
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
    }

    public String getUnntakFraLovvalgsland() {
        return unntakFraLovvalgsland;
    }

    public void setUnntakFraLovvalgsland(String unntakFraLovvalgsland) {
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
    }

    public String getUnntakFraLovvalgsbestemmelse() {
        return unntakFraLovvalgsbestemmelse;
    }

    public void setUnntakFraLovvalgsbestemmelse(String unntakFraLovvalgsbestemmelse) {
        this.unntakFraLovvalgsbestemmelse = unntakFraLovvalgsbestemmelse;
    }
}
