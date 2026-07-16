package com.mesofi.mythclothmarket.crawler.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * Raw listing fields extracted directly from HTML before normalization.
 */
@Getter
@Setter
public class RawStoreListing {
    private String figurineRawName;
    private String price;
    private String discount;
    private String link;
    private String availability;
}
