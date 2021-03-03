package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.data.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.LoennOgGodtgjoerelse;

public class Soeknad extends BehandlingsgrunnlagData {

    public LoennOgGodtgjoerelse loennOgGodtgjoerelse = new LoennOgGodtgjoerelse();

    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();
}