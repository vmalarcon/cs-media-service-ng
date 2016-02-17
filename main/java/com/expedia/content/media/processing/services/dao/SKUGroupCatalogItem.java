package com.expedia.content.media.processing.services.dao;

import java.sql.Timestamp;

/**
 * Represents the SKUGroupCatalogItem in LCM.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class SKUGroupCatalogItem {

    private final String name;
    private final int catalogItemStatusTypeID;
    private final double latitude;
    private final double longitude;
    private final boolean fuzzyLatLongBool;
    private final int structureTypeID;
    private final String airportCode ;
    private final boolean chargeDepositBool;
    private final String emailAddress;
    private final int merchandisingTypeID;
    private final Timestamp catalogItemUpdateDate;
    private final String catalogItemLastUpdatedBy;
    private final Timestamp skuGroupUpdateDate;
    private final String skuGroupLastUpdatedBy;
    private final int priceLevelTypeID;

    public SKUGroupCatalogItem(String name, int catalogItemStatusTypeID, double latitude, double longitude, boolean fuzzyLatLongBool, int structureTypeID,
                               String airportCode, boolean chargeDepositBool, String emailAddress, int merchandisingTypeID, Timestamp catalogItemUpdateDate,
                               String catalogItemLastUpdatedBy, Timestamp skuGroupUpdateDate, String skuGroupLastUpdatedBy, int priceLevelTypeID) {
        this.name = name;
        this.catalogItemStatusTypeID = catalogItemStatusTypeID;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fuzzyLatLongBool = fuzzyLatLongBool;
        this.structureTypeID = structureTypeID;
        this.airportCode = airportCode;
        this.chargeDepositBool = chargeDepositBool;
        this.emailAddress = emailAddress;
        this.merchandisingTypeID = merchandisingTypeID;
        this.catalogItemUpdateDate = catalogItemUpdateDate;
        this.catalogItemLastUpdatedBy = catalogItemLastUpdatedBy;
        this.skuGroupUpdateDate = skuGroupUpdateDate;
        this.skuGroupLastUpdatedBy = skuGroupLastUpdatedBy;
        this.priceLevelTypeID = priceLevelTypeID;
    }
}
