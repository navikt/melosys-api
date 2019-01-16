
package no.nav.melosys.eux.model.nav;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;


@SuppressWarnings("unused")
@JsonInclude(Include.NON_NULL)
public class Utsendingsland {


    private List<Arbeidsgiver> arbeidsgiver;

    public List<Arbeidsgiver> getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(List<Arbeidsgiver> arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

}
