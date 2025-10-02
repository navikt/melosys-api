package no.nav.melosys.saksflytapi.domain;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    ARBEIDSGIVER_SKAL_HA_KOPI("arbeidsgiverSkalHaKopi"),
    ARKIVSYSTEM("arkivsystem"),
    ARKIV_ID("arkivId"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_LAND("avsenderLand"),
    AVSENDER_NAVN("avsenderNavn"),
    AVSENDER_TYPE("avsenderType"),
    BEHANDLINGSRESULTATTYPE("behandlingsresultatType"),
    BEGRUNNELSE_FRITEKST("behandlingsresultatBegrunnelseFritekst"),
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
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    INSTITUSJON_ID("institusjonsID"),
    JFR_INGEN_VURDERING("ingenVurdering"),
    JOURNALPOST_ID("journalpostID"),
    LOGISKE_VEDLEGG_TITLER("logiskeVedleggTitler"),
    LOVVALGSBESTEMMELSE("lovvalgsbestemmelse"),
    LOVVALGSLAND("lovvalgsland"),
    MOTTAKER("mottaker"),
    MOTTAKSKANAL_ER_ELEKTRONISK("mottakskanalErElektronisk"),
    MOTTATT_DATO("mottattDato"),
    MOTTATT_SOKNAD_ID("mottattSoknadID"),
    OPPGAVE_ID("oppgaveID"),
    OPPHOLDSLAND("oppholdsland"),
    ORGNR("orgnr"),
    PERSON_IDENT("personIdent"),
    PRODUSERBART_BREV("produserbartBrev"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSBEHANDLER_NAVN("saksbehandlerNavn"),
    SAKSNUMMER("saksnummer"),
    FAKTURASERIE_REFERANSE("fakturaserieReferanse"),
    BETALINGSSTATUS("betalingsstatus"),
    FAKTURANUMMER("fakturanummer"),
    SAKSSTATUS("saksstatus"),
    SAKSTYPE("sakstype"),
    SAKSTEMA("sakstema"),
    SED_DOKUMENT("sedDokument"),
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
    YTTERLIGERE_INFO_SED("ytterligereInformasjonSed"),
    PROCESS_PARENT_ID("parentId"),
    FORVALTNINGSMELDING_MOTTAKER("forvaltningsmeldingMottaker"),
    GJELDER_ÅR("gjelderÅr"),
    IDENTIFIKATOR("identifikator"),
    ÅRSAK_TYPE("årsakType"),
    OPPRINNELIG_BEH("opprinneligBeh");

    private final String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
