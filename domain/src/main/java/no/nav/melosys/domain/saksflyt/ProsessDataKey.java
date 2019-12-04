package no.nav.melosys.domain.saksflyt;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    ARBEIDSGIVER("arbeidsgiver"),
    ARKIV_ID("arkivId"),
    ARKIVSYSTEM("arkivsystem"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_NAVN("avsenderNavn"),
    AVSENDER_TYPE("avsenderType"),
    BEGRUNNELSEKODE("begrunnelsekode"),
    BEHANDLINGSRESULTATTYPE("behandlingsresultatType"),
    BEHANDLINGSRESULTAT_BEGRUNNELSER("behandlingsresultatBegrunnelse"),
    BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST("behandlingsresultatBegrunnelseFritekst"),
    BEHANDLINGSTEMA("behandlingstema"),
    BEHANDLINGSTYPE("behandlingstype"),
    BREVDATA("brevData"),
    BRUKER_ID("brukerID"),
    DOKUMENT_ID("dokumentID"),
    EESSI_MELDING("eessiMelding"),
    EESSI_MOTTAKER("eessiMottaker"),
    ER_OPPDATERT_SED("erOppdatertSed"),
    FYSISKE_VEDLEGG("fysiskeVedlegg"),
    GSAK_SAK_ID("gsakSakID"),
    HOVEDDOKUMENT_TITTEL("hoveddokumentTittel"),
    JFR_INGEN_VURDERING("ingenVurdering"),
    JOURNALPOST_ID("journalpostID"),
    LOGISKE_VEDLEGG_TITLER("logiskeVedleggTitler"),
    LOVVALGSBESTEMMELSE("lovvalgsbestemmelse"),
    LOVVALGSLAND("lovvalgsland"),
    MOTTAKER("mottaker"),
    OPPHOLDSLAND("oppholdsland"),
    OPPGAVE_ID("oppgaveID"),
    REPRESENTANT("representant"),
    REPRESENTANT_KONTAKTPERSON("representantKontakperson"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSNUMMER("saksnummer"),
    SED_DOKUMENT("sedDokument"),
    SKAL_SENDES_FORVALTNINGSMELDING("skalSendesForvaltningsmelding"),
    SKAL_TILORDNES("skalTilordnes"),
    STATSBORGERSKAP("statsborgerskap"),
    SØKNADSLAND("land"),
    SØKNADSPERIODE("søknadsperiode"),
    TEMA("tema"),
    UNNTAK_FRA_LOVVALGSBESTEMMELSE("unntakFraLovvalgsbestemmelse"),
    UNNTAK_FRA_LOVVALGSLAND("unntakFraLovvalgsland");

    private String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
