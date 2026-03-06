package ksh.tryptobackend.regretanalysis.domain.vo;

public record AnalysisExchange(
    Long exchangeId,
    String name,
    String currency
) {

    public boolean isDomestic() {
        return "KRW".equals(currency);
    }
}
