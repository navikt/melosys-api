package no.nav.melosys.domain.dokument.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;

public class LovvalgBestemmelseXmlAdapter extends XmlAdapter<String, LovvalgBestemmelse> {
    @Override
    public LovvalgBestemmelse unmarshal(String s) throws Exception {
        return LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(s);
    }

    @Override
    public String marshal(LovvalgBestemmelse lovvalgBestemmelse) throws Exception {
        return lovvalgBestemmelse.name();
    }
}
