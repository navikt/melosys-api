package no.nav.melosys.domain.mottatteopplysninger.data

import java.time.LocalDate


class ArbeidsgiversBekreftelse {
    var arbeidsgiverBekrefterUtsendelse: Boolean? = null
    var arbeidstakerAnsattUnderUtsendelsen: Boolean? = null
    var erstatterArbeidstakerenUtsendte: Boolean? = null
    var arbeidstakerTidligereUtsendt24Mnd: Boolean? = null
    var arbeidsgiverBetalerArbeidsgiveravgift: Boolean? = null
    var trygdeavgiftTrukketGjennomSkatt: Boolean? = null
    var trygdeavgiftTrukketGjennomSkattDato: LocalDate? = null
}

