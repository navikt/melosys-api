package no.nav.melosys.service.vilkaar;

import java.util.HashSet;
import java.util.Set;

public class VilkaarDto {
    private String vilkaar;
    private boolean oppfylt;
    private Set<String> begrunnelseKoder;
    private String begrunnelseFritekst;
    private String begrunnelseFritekstEngelsk;

    public VilkaarDto() {
        this.begrunnelseKoder = new HashSet<>();
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

    public Set<String> getBegrunnelseKoder() {
        return begrunnelseKoder;
    }

    public void setBegrunnelseKoder(Set<String> begrunnelseKoder) {
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
