package ksh.tryptobackend.investmentround.domain.model;

import java.util.ArrayList;
import java.util.List;

public class EmergencyFundings {

    private final List<EmergencyFunding> fundings;

    public EmergencyFundings(List<EmergencyFunding> fundings) {
        this.fundings = fundings != null ? new ArrayList<>(fundings) : new ArrayList<>();
    }

    void add(EmergencyFunding funding) {
        fundings.add(funding);
    }

    public Long latestId() {
        return fundings.stream()
                .map(EmergencyFunding::id)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElseThrow(() -> new IllegalStateException("no persisted emergency funding to reference"));
    }

    public List<EmergencyFunding> values() {
        return List.copyOf(fundings);
    }

    public int size() {
        return fundings.size();
    }
}
