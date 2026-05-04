package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import java.util.Map;
import ksh.tryptobackend.marketdata.domain.vo.MarketMetaEntry;

public interface MarketMetaQueryPort {

    Map<String, List<MarketMetaEntry>> findAll();
}
