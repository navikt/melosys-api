package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public class TrygdeavgiftInfo {
    private long avgiftspliktigInntektMd;
    private boolean trygdeavgiftNav;
    private String avgiftskode;
    private String saerligeavgiftsgruppe;

    public TrygdeavgiftInfo(long avgiftspliktigInntektMd, boolean trygdeavgiftNav, String avgiftskode, String saerligeavgiftsgruppe) {
        this.avgiftspliktigInntektMd = avgiftspliktigInntektMd;
        this.trygdeavgiftNav = trygdeavgiftNav;
        this.avgiftskode = avgiftskode;
        this.saerligeavgiftsgruppe = saerligeavgiftsgruppe;
    }

    public long getAvgiftspliktigInntektMd() {
        return avgiftspliktigInntektMd;
    }

    public boolean isTrygdeavgiftNav() {
        return trygdeavgiftNav;
    }

    public String getAvgiftskode() {
        return avgiftskode;
    }

    public String getSaerligeavgiftsgruppe() {
        return saerligeavgiftsgruppe;
    }
}
