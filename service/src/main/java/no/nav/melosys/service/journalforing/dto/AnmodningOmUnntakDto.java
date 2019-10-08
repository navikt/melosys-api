package no.nav.melosys.service.journalforing.dto;

import java.util.List;

public class AnmodningOmUnntakDto {
    private String lovvalgsbestemmelse;
    private List<String> unntakFraLovvalgsland;
    private String unntakFraLovvalgsbestemmelse;

    public String getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public void setLovvalgsbestemmelse(String lovvalgsbestemmelse) {
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
    }

    public List<String> getUnntakFraLovvalgsland() {
        return unntakFraLovvalgsland;
    }

    public void setUnntakFraLovvalgsland(List<String> unntakFraLovvalgsland) {
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
    }

    public String getUnntakFraLovvalgsbestemmelse() {
        return unntakFraLovvalgsbestemmelse;
    }

    public void setUnntakFraLovvalgsbestemmelse(String unntakFraLovvalgsbestemmelse) {
        this.unntakFraLovvalgsbestemmelse = unntakFraLovvalgsbestemmelse;
    }
}
