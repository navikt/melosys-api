package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

public class BrevmalResponse {

    private final MottakerDto mottaker;
    private final List<BrevmalTypeDto> brevTyper;

    public BrevmalResponse(MottakerDto mottaker, List<BrevmalTypeDto> brevTyper) {
        this.mottaker = mottaker;
        this.brevTyper = brevTyper;
    }

    public MottakerDto getMottaker() {
        return mottaker;
    }

    public List<BrevmalTypeDto> getBrevTyper() {
        return brevTyper;
    }
}
