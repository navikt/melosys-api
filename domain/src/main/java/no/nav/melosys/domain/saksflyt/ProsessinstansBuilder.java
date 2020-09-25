package no.nav.melosys.domain.saksflyt;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class ProsessinstansBuilder {
    private ProsessType type;
    private Behandling behandling;
    private Object begrunnelser;
    private String begrunnelseFritekst;
    private String ytterligereInformasjonSed;
    private Set<String> eessiMottakere;
    private MelosysEessiMelding eessiMelding;

    public ProsessinstansBuilder medType(ProsessType type) {
        this.type = type;
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

    public ProsessinstansBuilder medYtterligereinformasjonSed(String ytterligereInformasjonSed) {
        this.ytterligereInformasjonSed = ytterligereInformasjonSed;
        return this;
    }

    public ProsessinstansBuilder medEessiMottakere(Set<String> eessiMottakere) {
        this.eessiMottakere = eessiMottakere;
        return this;
    }

    public ProsessinstansBuilder medEessiMelding(MelosysEessiMelding eessiMelding) {
        this.eessiMelding = eessiMelding;
        return this;
    }

    public Prosessinstans build() {
        Prosessinstans prosessinstans =  new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(type);

        if (begrunnelser != null) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, begrunnelser);
        }
        if (StringUtils.isNotEmpty(begrunnelseFritekst)) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, begrunnelseFritekst);
        }
        if (StringUtils.isNotEmpty(ytterligereInformasjonSed)) {
            prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, ytterligereInformasjonSed);
        }
        if (!CollectionUtils.isEmpty(eessiMottakere)) {
            prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, eessiMottakere);
        }
        if (eessiMelding != null) {
            prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        }
        return prosessinstans;
    }
}