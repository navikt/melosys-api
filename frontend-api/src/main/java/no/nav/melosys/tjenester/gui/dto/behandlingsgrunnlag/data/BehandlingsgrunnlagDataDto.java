package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.data;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.*;

public class BehandlingsgrunnlagDataDto {
    private Soeknadsland soeknadsland = new Soeknadsland();
    private Periode periode = new Periode();
    private OpplysningerOmBrukeren personOpplysninger = new OpplysningerOmBrukeren();
    private List<ArbeidUtland> arbeidUtland = new ArrayList<>();
    private List<ForetakUtland> foretakUtland = new ArrayList<>();
    private OppholdUtland oppholdUtland = new OppholdUtland();
    private ArbeidNorge arbeidNorge = new ArbeidNorge();
    private SelvstendigArbeid selvstendigArbeid = new SelvstendigArbeid();
    private JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
    private List<MaritimtArbeid> maritimtArbeid = new ArrayList<>();
    private Bosted bosted = new Bosted();

    public BehandlingsgrunnlagDataDto() {
    }

    public BehandlingsgrunnlagDataDto(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        this.soeknadsland = behandlingsgrunnlagData.soeknadsland;
        this.periode = behandlingsgrunnlagData.periode;
        this.personOpplysninger = behandlingsgrunnlagData.personOpplysninger;
        this.arbeidUtland = behandlingsgrunnlagData.arbeidUtland;
        this.foretakUtland = behandlingsgrunnlagData.foretakUtland;
        this.oppholdUtland = behandlingsgrunnlagData.oppholdUtland;
        this.arbeidNorge = behandlingsgrunnlagData.arbeidNorge;
        this.selvstendigArbeid = behandlingsgrunnlagData.selvstendigArbeid;
        this.juridiskArbeidsgiverNorge = behandlingsgrunnlagData.juridiskArbeidsgiverNorge;
        this.maritimtArbeid = behandlingsgrunnlagData.maritimtArbeid;
        this.bosted = behandlingsgrunnlagData.bosted;
    }

    public Soeknadsland getSoeknadsland() {
        return soeknadsland;
    }

    public Periode getPeriode() {
        return periode;
    }

    public OpplysningerOmBrukeren getPersonOpplysninger() {
        return personOpplysninger;
    }

    public List<ArbeidUtland> getArbeidUtland() {
        return arbeidUtland;
    }

    public List<ForetakUtland> getForetakUtland() {
        return foretakUtland;
    }

    public OppholdUtland getOppholdUtland() {
        return oppholdUtland;
    }

    public ArbeidNorge getArbeidNorge() {
        return arbeidNorge;
    }

    public SelvstendigArbeid getSelvstendigArbeid() {
        return selvstendigArbeid;
    }

    public JuridiskArbeidsgiverNorge getJuridiskArbeidsgiverNorge() {
        return juridiskArbeidsgiverNorge;
    }

    public List<MaritimtArbeid> getMaritimtArbeid() {
        return maritimtArbeid;
    }

    public Bosted getBosted() {
        return bosted;
    }
}
