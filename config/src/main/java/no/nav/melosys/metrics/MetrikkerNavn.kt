package no.nav.melosys.metrics

object MetrikkerNavn {
    private const val METRIKKER_NAMESPACE = "melosys"

    const val SAKER_OPPRETTET = "$METRIKKER_NAMESPACE.saker.opprettet"
    const val BEHANDLINGSTEMAER_OPPRETTET = "$METRIKKER_NAMESPACE.behandlingstemaer.opprettet"
    const val BEHANDLINGSTYPER_OPPRETTET = "$METRIKKER_NAMESPACE.behandlingstyper.opprettet"
    const val BEHANDLINGER_AVSLUTTET = "$METRIKKER_NAMESPACE.behandlinger.avsluttet"

    private const val PROSESSINSTANSER = "$METRIKKER_NAMESPACE.prosessinstanser"
    const val PROSESSINSTANSER_FEILET = "$PROSESSINSTANSER.feilet"
    const val PROSESSINSTANSER_OPPRETTET = "$PROSESSINSTANSER.opprettet"
    const val PROSESSINSTANSER_STEG_UTFØRT = "$PROSESSINSTANSER.steg.utfoert"
    const val PROSESSINSTANSER_STEG_FEILET = "$PROSESSINSTANSER.steg.feilet"

    const val UNNTAKSPERIODE_KONTROLL_TREFF = "$METRIKKER_NAMESPACE.unntakperiode.treffbegrunnelse"
    const val SVAR_AOU = "$METRIKKER_NAMESPACE.svar.aou"
    const val EVENTS_FEILET = "$METRIKKER_NAMESPACE.events.feilet"

    const val TAG_TEMA = "tema"
    const val TAG_TYPE = "type"
    const val TAG_STATUS = "status"
    const val TAG_BEGRUNNELSE = "begrunnelse"
    const val TAG_RESULTAT = "resultat"
    const val TAG_PROSESSINSTANSTYPE = "prosessinstanstype"
    const val TAG_PROSESSTEG = "prosessSteg"
}
