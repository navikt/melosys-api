package no.nav.melosys.integrasjon.doksys;

public final class Dokumentbestilling {
    private final DokumentbestillingMetadata metadata;
    private final Object brevData;

    public Dokumentbestilling(DokumentbestillingMetadata metadata, Object brevXml) {
        this.metadata = metadata;
        this.brevData = brevXml;
    }

    public DokumentbestillingMetadata getMetadata() {
        return metadata;
    }

    public Object getBrevData() {
        return brevData;
    }
}
