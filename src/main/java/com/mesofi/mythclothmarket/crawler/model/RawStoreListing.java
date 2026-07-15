package com.mesofi.mythclothmarket.crawler.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RawStoreListing {
    private String figurineRawName;
    private String price;
    private String discount;
    private String link;
    private String availability;
}
