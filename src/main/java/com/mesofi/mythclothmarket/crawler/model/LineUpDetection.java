package com.mesofi.mythclothmarket.crawler.model;

/**
 * Represents the result of extracting lineup information from a raw product
 * name.
 * <p>
 * A {@code LineUpDetection} contains both the detected {@link LineUp} and the
 * normalized product name with the lineup prefix removed. This allows callers
 * to parse the input only once and reuse both pieces of information during
 * subsequent matching or processing.
 * <p>
 * For example, given the raw name:
 * 
 * <pre>
 * Saint Seiya Myth Cloth EX - Cygnus Hyoga (Final Bronze Cloth) ~Original Color Edition~
 * </pre>
 * 
 * the resulting detection would contain:
 * <ul>
 * <li>{@code lineUp = LineUp.MYTH_CLOTH_EX}</li>
 * <li>{@code normalizedName = "Cygnus Hyoga (Final Bronze Cloth) ~Original Color Edition~"}</li>
 * </ul>
 *
 * @param lineUp
 *            the detected lineup extracted from the raw product name
 * @param normalizedName
 *            the product name with the lineup prefix removed
 */
public record LineUpDetection(LineUp lineUp, String normalizedName) {
}
