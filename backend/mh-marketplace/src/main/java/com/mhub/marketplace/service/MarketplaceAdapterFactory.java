package com.mhub.marketplace.service;

import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.marketplace.adapter.MarketplaceAdapter;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MarketplaceAdapterFactory {
    private final Map<MarketplaceType, MarketplaceAdapter> adapters;
    public MarketplaceAdapterFactory(List<MarketplaceAdapter> adapterList) { this.adapters = adapterList.stream().collect(Collectors.toMap(MarketplaceAdapter::getMarketplaceType, Function.identity())); }
    public MarketplaceAdapter getAdapter(MarketplaceType type) { MarketplaceAdapter a = adapters.get(type); if (a == null) throw new UnsupportedOperationException("No adapter for: " + type); return a; }
}
