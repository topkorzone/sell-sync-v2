package com.mhub.core.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderItemMappingRequest {
    private UUID erpItemId;
    private String erpProdCd;
    private String erpWhCd;
}
