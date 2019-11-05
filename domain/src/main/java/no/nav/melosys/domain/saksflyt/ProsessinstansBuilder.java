package no.nav.melosys.domain.saksflyt;

import no.nav.melosys.domain.Behandling;
import org.apache.commons.lang3.StringUtils;

public class ProsessinstansBuilder {
    private ProsessType type;
    private ProsessSteg steg;
    private Behandling behandling;
    private Object begrunnelser;
    private String begrunnelseFritekst;

    public ProsessinstansBuilder medType(ProsessType type) {
        this.type = type;
        return this;
    }

    public ProsessinstansBuilder medSteg(ProsessSteg steg) {
        this.steg = steg;
        return this;
    }

    public ProsessinstansBuilder medBehandling(Behandling behandling) {
        this.behandling = behandling;
        return this;
    }

    public ProsessinstansBuilder medBegrunnelser(Object begrunnelser) {
        this.begrunnelser = begrunnelser;
        return this;
    }

    public ProsessinstansBuilder medBegrunnelseFritekst(String fritekst) {
        this.begrunnelseFritekst = fritekst;
        return this;
    }

    public Prosessinstans build() {
        Prosessinstans pi =  new Prosessinstans();
        pi.setBehandling(behandling);
        pi.setType(type);
        pi.setSteg(steg);

        if (begrunnelser != null) {
            pi.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, begrunnelser);
        }
        if (StringUtils.isNotEmpty(begrunnelseFritekst)) {
            pi.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, begrunnelseFritekst);
        }
        return pi;
    }
}