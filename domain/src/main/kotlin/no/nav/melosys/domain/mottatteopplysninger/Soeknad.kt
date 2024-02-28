package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.mottatteopplysninger.data.ArbeidsgiversBekreftelse
import no.nav.melosys.domain.mottatteopplysninger.data.ArbeidssituasjonOgOevrig
import no.nav.melosys.domain.mottatteopplysninger.data.LoennOgGodtgjoerelse
import no.nav.melosys.domain.mottatteopplysninger.data.Utenlandsoppdraget


class Soeknad : MottatteOpplysningerData() {
    var loennOgGodtgjoerelse: LoennOgGodtgjoerelse? = LoennOgGodtgjoerelse()
    var arbeidsgiversBekreftelse = ArbeidsgiversBekreftelse()
    var utenlandsoppdraget = Utenlandsoppdraget()
    var arbeidssituasjonOgOevrig = ArbeidssituasjonOgOevrig()
}
