package no.nav.melosys.domain.saksflyt;

/**
 * Property keys brukt i saksflyt
 */
public enum ProsessDataKey {

    AKTØR_ID("aktoerID"),
    ARBEIDSGIVER("arbeidsgiver"),
    ARKIVSYSTEM("arkivsystem"),
    ARKIV_ID("arkivId"),
    AVSENDER_ID("avsenderID"),
    AVSENDER_LAND("avsenderLand"),
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
    DISTRIBUERBAR_JOURNALPOST_ID("distribuerbarJournalpostID"),
    DISTRIBUER_MOTTAKER_LAND("distribuerMottakerLand"),
    DOKUMENT_ID("dokumentID"),
    EESSI_MELDING("eessiMelding"),
    EESSI_MOTTAKERE("eessiMottakere"),
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
    MOTTATT_DATO("mottattDato"),
    MOTTATT_SOKNAD_ID("mottattSoknadID"),
    OPPGAVE_ID("oppgaveID"),
    OPPHOLDSLAND("oppholdsland"),
    REPRESENTANT("representant"),
    REPRESENTANT_KONTAKTPERSON("representantKontakperson"),
    REPRESENTANT_REPRESENTERER("representantRepresenterer"),
    REVURDER_BEGRUNNELSE("revurderBegrunnelse"),
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
    UNNTAK_FRA_LOVVALGSLAND("unntakFraLovvalgsland"),
    UTPEKING_AVVIS("utpekingAvvis"),
    UTPEKT_LAND("utpektLand"),
    VARSLE_UTLAND("varsleUtland"),
    VEDTAKSTYPE("vedtakstype"),
    YTTERLIGERE_INFO_SED("ytterligereInformasjonSed");

    private String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
