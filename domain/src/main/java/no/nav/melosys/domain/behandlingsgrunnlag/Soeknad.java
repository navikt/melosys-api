package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Arbeidsinntekt;

public class Soeknad extends BehandlingsgrunnlagData {

    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();


    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

}