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
    BEHANDLINGSRESULTATTYPE("behandlingsresultatType"),
    BEHANDLINGSRESULTAT_BEGRUNNELSER("behandlingsresultatBegrunnelse"),
    BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST("behandlingsresultatBegrunnelseFritekst"),
    BEHANDLINGSTEMA("behandlingstema"),
    BEHANDLINGSTYPE("behandlingstype"),
    BREVBESTILLING("brevbestilling"),
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
    REPRESENTANT("representant"),
    REPRESENTANT_KONTAKTPERSON("representantKontakperson"),
    REPRESENTANT_REPRESENTERER("representantRepresenterer"),
    SAKSBEHANDLER("saksbehandler"),
    SAKSNUMMER("saksnummer"),
    SAKSTYPE("sakstype"),
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
    YTTERLIGERE_INFO_SED("ytterligereInformasjonSed"),
    DISTRIBUSJONSTYPE("distribusjonsType");

    private final String kode;

    ProsessDataKey(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
