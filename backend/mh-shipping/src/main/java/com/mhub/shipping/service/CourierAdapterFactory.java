package com.mhub.shipping.service;

import com.mhub.core.domain.enums.CourierType;
import com.mhub.shipping.adapter.CourierAdapter;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CourierAdapterFactory {
    private final Map<CourierType, CourierAdapter> adapters;
    public CourierAdapterFactory(List<CourierAdapter> list) { this.adapters = list.stream().collect(Collectors.toMap(CourierAdapter::getCourierType, Function.identity())); }
    public CourierAdapter getAdapter(CourierType type) { CourierAdapter a = adapters.get(type); if (a == null) throw new UnsupportedOperationException("No adapter for courier: " + type); return a; }
}
