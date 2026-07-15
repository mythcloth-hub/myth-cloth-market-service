package com.mesofi.mythclothmarket;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MarketPriceStore {
    private String figurineRawName;
    private String price;
    private String discount;
    private String link;
    private String availability;
}
