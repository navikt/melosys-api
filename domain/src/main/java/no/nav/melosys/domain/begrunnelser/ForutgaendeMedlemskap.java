package no.nav.melosys.domain.begrunnelser;

import no.nav.melosys.domain.Kodeverk;

public enum ForutgaendeMedlemskap implements Kodeverk {
    IKKE_LOENNET_UTSEND_AG("IKKE_LOENNET_UTSEND_AG", "Mottok IKKE lønn fra utsendende arbeidsgiver opptjent i Norge forutgående måned."),
    IKKE_LOENNET_NORGE("IKKE_LOENNET_NORGE", "Mottok IKKE lønn fra annen arbeidsgiver opptjent i Norge forutgående måned."),
    UNNTATT_MEDLEMSKAP("UNNTATT_MEDLEMSKAP", "Har direkte forutgående medlemskap i MEDL."),
    MOTTAT_LOENN_UTL("MOTTAT_LOENN_UTL", "Mottok lønn opptjent i annet land forutgående måned."),
    IKKE_FOLKEREGISTRERT_NORGE("IKKE_FOLKEREGISTRERT_NORGE", "Har ikke TPS-adresse i Norge."),
    IKKE_ANSATT_UTSEND_AG("IKKE_ANSATT_UTSEND_AG", "Er IKKE registrert i Aa-registeret hos utsendende arbeidsgiver.");

    private String kode;
    private String beskrivelse;

    ForutgaendeMedlemskap(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() { return kode; }

    @Override
    public String getBeskrivelse() { return beskrivelse; }
}
