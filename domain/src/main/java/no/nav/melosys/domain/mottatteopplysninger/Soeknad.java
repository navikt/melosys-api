package no.nav.melosys.domain.mottatteopplysninger;

import no.nav.melosys.domain.mottatteopplysninger.data.ArbeidsgiversBekreftelse;
import no.nav.melosys.domain.mottatteopplysninger.data.ArbeidssituasjonOgOevrig;
import no.nav.melosys.domain.mottatteopplysninger.data.LoennOgGodtgjoerelse;
import no.nav.melosys.domain.mottatteopplysninger.data.Utenlandsoppdraget;

public class Soeknad extends MottatteOpplysningerData {

    public LoennOgGodtgjoerelse loennOgGodtgjoerelse = new LoennOgGodtgjoerelse();

    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

    public Utenlandsoppdraget utenlandsoppdraget = new Utenlandsoppdraget();

    public ArbeidssituasjonOgOevrig arbeidssituasjonOgOevrig = new ArbeidssituasjonOgOevrig();
}
