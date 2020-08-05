package no.nav.melosys.service.vilkaar;

import java.util.ArrayList;
import java.util.List;

public class VilkaarDto {
    private String vilkaar;
    private boolean oppfylt;
    private List<String> begrunnelseKoder;
    private String begrunnelseFritekst;
    private String begrunnelseFritekstEngelsk;

    public VilkaarDto() {
        this.begrunnelseKoder = new ArrayList<>();
    }

    public String getVilkaar() {
        return vilkaar;
    }

    public void setVilkaar(String vilkaar) {
        this.vilkaar = vilkaar;
    }

    public boolean isOppfylt() {
        return oppfylt;
    }

    public void setOppfylt(boolean oppfylt) {
        this.oppfylt = oppfylt;
    }

    public List<String> getBegrunnelseKoder() {
        return begrunnelseKoder;
    }

    public void setBegrunnelseKoder(List<String> begrunnelseKoder) {
        this.begrunnelseKoder = begrunnelseKoder;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public String getBegrunnelseFritekstEngelsk() {
        return begrunnelseFritekstEngelsk;
    }

    public void setBegrunnelseFritekstEngelsk(String begrunnelseFritekstEngelsk) {
        this.begrunnelseFritekstEngelsk = begrunnelseFritekstEngelsk;
    }
}
