package no.nav.melosys.saksflytapi.domain;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    ARBEIDSGIVER("arbeidsgiver"),
    ARBEIDSGIVER_SKAL_HA_KOPI("arbeidsgiverSkalHaKopi"),
    ARKIVSYSTEM("arkivsystem"),
    ARKIV_ID("arkivId"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_LAND("avsenderLand"),
    AVSENDER_NAVN("avsenderNavn"),
    AVSENDER_TYPE("avsenderType"),
    BEHANDLINGSRESULTATTYPE("behandlingsresultatType"),
    BEHANDLINGSRESULTAT_BEGRUNNELSER("behandlingsresultatBegrunnelse"),
    BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST("behandlingsresultatBegrunnelseFritekst"),
    BEHANDLINGSTEMA("behandlingstema"),
    BEHANDLINGSTYPE("behandlingstype"),
    BEHANDLINGSÅRSAKTYPE("behandlingsårsakType"),
    BEHANDLINGSÅRSAK_FRITEKST("behandlingsårsakFritekst"),
    BREVBESTILLING("brevbestilling"),
    BETALINGSINTERVALL("betalingsIntervall"),
    BREVDATA("brevData"),
    BRUKER_ID("brukerID"),
    CORRELATION_ID_SAKSFLYT("correlationId"),
    DISTRIBUERBAR_JOURNALPOST_ID("distribuerbarJournalpostID"),
    DISTRIBUER_MOTTAKER_LAND("distribuerMottakerLand"),
    DISTRIBUSJONSTYPE("distribusjonstype"),
    DOKUMENT_ID("dokumentID"),
    EESSI_MELDING("eessiMelding"),
    EESSI_MOTTAKERE("eessiMottakere"),
    ER_OPPDATERT_SED("erOppdatertSed"),
    FYSISKE_VEDLEGG("fysiskeVedlegg"),
    FULLMEKTIG("fullmektig"),
    FULLMAKTER("fullmakter"),
    FULLMEKTIG_KONTAKTPERSON("fullmektigKontaktperson"),
    FULLMEKTIG_KONTAKT_ORGNR("fullmektigKontaktOrgnr"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    INSTITUSJON_ID("institusjonsID"),
    JFR_INGEN_VURDERING("ingenVurdering"),
    JOURNALPOST_ID("journalpostID"),
    LOGISKE_VEDLEGG_TITLER("logiskeVedleggTitler"),
    LOVVALGSBESTEMMELSE("lovvalgsbestemmelse"),
    LOVVALGSLAND("lovvalgsland"),
    MOTTAKER("mottaker"),
    MOTTATT_DATO("mottattDato"),
    MOTTATT_SOKNAD_ID("mottattSoknadID"),
    OPPGAVE_ID("oppgaveID"),
    OPPHOLDSLAND("oppholdsland"),
    ORGNR("orgnr"),
    PERSON_IDENT("personIdent"),
    PRODUSERBART_BREV("produserbartBrev"),
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    REPRESENTANT("representant"),
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    REPRESENTANT_KONTAKTPERSON("representantKontakperson"),
    @Deprecated(since = "melosys.fullmektig.trygdeavgift")
    REPRESENTANT_REPRESENTERER("representantRepresenterer"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSBEHANDLER_NAVN("saksbehandlerNavn"),
    SAKSNUMMER("saksnummer"),
    FAKTURASERIE_REFERANSE("fakturaserieReferanse"),
    SAKSSTATUS("saksstatus"),
    SAKSTYPE("sakstype"),
    SAKSTEMA("sakstema"),
    SED_DOKUMENT("sedDokument"),
    SKAL_SENDES_FORVALTNINGSMELDING("skalSendesForvaltningsmelding"),
    SKAL_TILORDNES("skalTilordnes"),
    STATSBORGERSKAP("statsborgerskap"),
    SØKNADSLAND("land"),
    SØKNADSPERIODE("søknadsperiode"),
    TEMA("tema"),
    UNNTAK_FRA_LOVVALGSBESTEMMELSE("unntakFraLovvalgsbestemmelse"),
    UNNTAK_FRA_LOVVALGSLAND("unntakFraLovvalgsland"),
    UTPEKING_AVVIS("utpekingAvvis"),
    UTPEKT_LAND("utpektLand"),
    VARSLE_UTLAND("varsleUtland"),
    VEDLEGG_SED("vedleggTilSed"),
    VIRKSOMHET_ORGNR("virksomhetOrgnr"),
    YTTERLIGERE_INFO_SED("ytterligereInformasjonSed");

    private final String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
