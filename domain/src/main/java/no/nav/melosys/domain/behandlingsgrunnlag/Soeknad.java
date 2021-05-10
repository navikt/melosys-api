package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.data.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Utenlandsoppdraget;

public class Soeknad extends BehandlingsgrunnlagData {

    public LoennOgGodtgjoerelse loennOgGodtgjoerelse = new LoennOgGodtgjoerelse();

    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

    public Utenlandsoppdraget utenlandsoppdraget = new Utenlandsoppdraget();
}