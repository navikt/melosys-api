package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.data.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Arbeidsinntekt;

public class Soeknad extends BehandlingsgrunnlagData {

    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();


    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

}