package com.MenuBank.MenuBank.category;

/**
 * Where a catalog record (product/category) came from: created by the merchant in
 * MenuBank or imported from an external sales channel. Mirrors OrderOrigin naming.
 */
public enum CatalogOrigin {
    MENUBANK,
    ANOTA_AI,
    IFOOD
}
