package no.nav.melosys.integrasjon.doksys;

import org.w3c.dom.Element;

public final class Dokumentbestilling {
    private final DokumentbestillingMetadata metadata;
    private final Element brevData;

    public Dokumentbestilling(DokumentbestillingMetadata metadata, Element brevXml) {
        this.metadata = metadata;
        this.brevData = brevXml;
    }

    DokumentbestillingMetadata getMetadata() {
        return metadata;
    }

    public Element getBrevData() {
        return brevData;
    }
}
