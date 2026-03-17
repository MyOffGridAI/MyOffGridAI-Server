package com.myoffgridai.models.dto;

/**
 * Known GGUF quantization types with human-readable labels and quality rankings.
 *
 * <p>Each enum constant encodes a quantization method used in GGUF model files.
 * The {@link #rank} determines quality ordering (higher is better), and the
 * {@link #label} provides a user-friendly description for display in the UI.</p>
 *
 * <p>Ranking order (lowest to highest quality):
 * {@code IQ1_S < IQ2_XS < Q2_K < Q3_K_XS < Q3_K_S < Q3_K_M < Q4_K_S < Q4_K_M
 * < Q5_K_S < Q5_K_M < Q6_K < Q8_0 < F16 = BF16 < F32}</p>
 */
public enum QuantizationType {

    /** 1-bit importance quantization — extreme compression, lowest quality */
    IQ1_S(1, "Extreme \u2014 smallest size, lowest quality"),

    /** 2-bit importance quantization — extreme compression */
    IQ2_XS(2, "Extreme \u2014 very small, very low quality"),

    /** 2-bit k-quant — very small file, very low quality */
    Q2_K(3, "Lowest \u2014 very small, very low quality"),

    /** 3-bit k-quant extra-small — small file, poor quality */
    Q3_K_XS(4, "Very low \u2014 small, poor quality"),

    /** 3-bit k-quant small — small file, reduced quality */
    Q3_K_S(5, "Low \u2014 small, reduced quality"),

    /** 3-bit k-quant medium — small file, acceptable quality */
    Q3_K_M(6, "Low-medium \u2014 small, acceptable quality"),

    /** 4-bit k-quant small — balanced size and quality */
    Q4_K_S(7, "Medium \u2014 balanced, good quality"),

    /** 4-bit k-quant medium — balanced, most popular choice */
    Q4_K_M(8, "Medium \u2014 balanced (most popular)"),

    /** 5-bit k-quant small — larger file, good quality */
    Q5_K_S(9, "Medium-high \u2014 larger, good quality"),

    /** 5-bit k-quant medium — larger file, very good quality */
    Q5_K_M(10, "Medium-high \u2014 larger, very good quality"),

    /** 6-bit k-quant — large file, excellent quality */
    Q6_K(11, "High \u2014 large, excellent quality"),

    /** 8-bit quantization — very large, near-lossless */
    Q8_0(12, "Very high \u2014 very large, near-lossless"),

    /** Half-precision 16-bit float — full precision */
    F16(13, "Full \u2014 half-precision float"),

    /** Brain float 16 — full precision (same rank as F16) */
    BF16(13, "Full \u2014 brain float 16"),

    /** Full 32-bit float — maximum precision, largest files */
    F32(14, "Maximum \u2014 full-precision float");

    private final int rank;
    private final String label;

    QuantizationType(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    /**
     * Returns the quality rank (higher is better).
     *
     * @return the quality rank
     */
    public int getRank() {
        return rank;
    }

    /**
     * Returns the human-readable quality label.
     *
     * @return the quality label
     */
    public String getLabel() {
        return label;
    }
}
