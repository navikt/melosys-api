package no.nav.melosys.domain.kodeverk;

import no.nav.melosys.exception.IkkeFunnetException;

public enum Vilkaar implements Kodeverk {

    INNGANGSVILKÅR_EOSFO("INNGANGSVILKAAR_EOSFO", "Vurdering av om bruker oppfyller inngangsvilkår av EU/EØS forordningen."),
    FO_883_2004_ART12_1("FO_883_2004_ART12_1", "Vurdering av om bruker oppfyller ART12.1 vilkår."),
    ART12_1_FORUTGÅENDE_MEDLEMSKAP("ART12_1_FORUTGAAENDE_MEDLEMSKAP", "Vurdering av om bruker oppfyller vilkår om forutgående medlemskap."),
    ART12_1_VESENTLIG_VIRKSOMHET("ART12_1_VESENTLIG_VIRKSOMHET", "Vurdering av om arbeidsgiver eller oppdragsgiver i Norge oppfyller vilkår om vesentlig virksomhet."),
    FO_883_2004_ART12_2("FO_883_2004_ART12_2", "Vurdering av om bruker oppfyller ART12.2 vilkår."),
    ART12_2_NORMALT_DRIVER_VIRKSOMHET("ART12_2_NORMALT_DRIVER_VIRKSOMHET", "Vurdering av om selvstendig næringsdrivende normalt driver virksomhet i Norge."),
    FO_883_2004_ART16_1("FO_883_2004_ART16_1", "Vurdering av om bruker oppfyller ART16.1 vilkår."),
    BOSATT_I_NORGE("BOSATT_I_NORGE", "Vurdering av om bruker oppfyller vilkår om å være bosatt i Norge.");

    private static final Vilkaar[] VALUES = values();

    private String kode;
    private String beskrivelse;

    Vilkaar(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public static Vilkaar forKode(String kode) throws IkkeFunnetException {
        for (Vilkaar type: VALUES) {
            if (type.getKode().equals(kode)) {
                return type;
            }
        }
        throw new IkkeFunnetException("VilkaarType med kode " + kode + " finnes ikke.");
    }
}
