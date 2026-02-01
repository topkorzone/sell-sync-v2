package com.mhub.erp.service;

import com.mhub.core.domain.enums.ErpType;
import com.mhub.erp.adapter.ErpAdapter;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ErpAdapterFactory {
    private final Map<ErpType, ErpAdapter> adapters;
    public ErpAdapterFactory(List<ErpAdapter> list) { this.adapters = list.stream().collect(Collectors.toMap(ErpAdapter::getErpType, Function.identity())); }
    public ErpAdapter getAdapter(ErpType type) { ErpAdapter a = adapters.get(type); if (a == null) throw new UnsupportedOperationException("No adapter for ERP: " + type); return a; }
}
