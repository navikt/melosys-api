package no.nav.melosys.regler.api.lovvalg.rep.adapter;

import java.util.stream.Stream;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import no.nav.melosys.regler.api.lovvalg.rep.Kategori;

public class KategoriAdapter extends XmlAdapter<KategoriDto, Kategori> {

    @Override
    public Kategori unmarshal(KategoriDto kategoriDto) throws Exception {
        return Stream.of(Kategori.values())
            .filter(kategori -> kategori.beskrivelse.equals(kategoriDto.beskrivelse)
                    && kategori.alvorlighetsgrad == kategoriDto.alvorlighetsgrad)
            .findFirst().orElse(null);
    }

    @Override
    public KategoriDto marshal(Kategori kategori) throws Exception {
        return new KategoriDto(kategori.alvorlighetsgrad, kategori.beskrivelse);
    }
}
