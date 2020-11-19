package io.horizontalsystems.bitcoincash.blocks;

import java.math.BigInteger;

public class AsertAlgorithm {
    public static final BigInteger TARGET_SPACING_BIGINT = BigInteger.valueOf(10L * 60L);  // 10 minutes per block.
    public static final int MAX_BITS = 0x1d00ffff;
    public static final String MAX_BITS_STRING = "1d00ffff";
    public static final BigInteger MAX_TARGET = Utils.decodeCompactBits(MAX_BITS);

    /**
     * Compute aserti-2d DAA target
     */
    public static BigInteger computeAsertTarget(BigInteger refTarget, BigInteger referenceBlockAncestorTime, BigInteger referenceBlockHeight,
                                                BigInteger evalBlockTime, BigInteger evalBlockHeight) {
        Utils.checkState(evalBlockHeight.compareTo(referenceBlockHeight) >= 0, "");

        BigInteger heightDiff = evalBlockHeight.subtract(referenceBlockHeight);
        //referenceBlockAncestorTime is the timestamp of the ancestor of the anchor block
        BigInteger timeDiff = evalBlockTime.subtract(referenceBlockAncestorTime);
        //used by asert. two days in seconds.
        BigInteger halfLife = BigInteger.valueOf(2L * 24L * 60L * 60L);
        BigInteger rbits = BigInteger.valueOf(16L);
        BigInteger radix = BigInteger.ONE.shiftLeft(rbits.intValue());

        BigInteger target = refTarget;
        BigInteger exponent;
        BigInteger heightDiffWithOffset = heightDiff.add(BigInteger.ONE);
        BigInteger targetHeightOffsetMultiple = TARGET_SPACING_BIGINT.multiply(heightDiffWithOffset);
        exponent = timeDiff.subtract(targetHeightOffsetMultiple);
        exponent = exponent.shiftLeft(rbits.intValue());
        exponent = exponent.divide(halfLife);
        BigInteger numShifts = exponent.shiftRight(rbits.intValue());
        exponent = exponent.subtract(numShifts.shiftLeft(rbits.intValue()));

        BigInteger factor = BigInteger.valueOf(195766423245049L).multiply(exponent);
        factor = factor.add(BigInteger.valueOf(971821376L).multiply(exponent.pow(2)));
        factor = factor.add(BigInteger.valueOf(5127L).multiply(exponent.pow(3)));
        factor = factor.add(BigInteger.valueOf(2L).pow(47));
        factor = factor.shiftRight(48);
        target = target.multiply(radix.add(factor));

        if(numShifts.compareTo(BigInteger.ZERO) < 0) {
            target = target.shiftRight(-numShifts.intValue());
        } else {
            target = target.shiftLeft(numShifts.intValue());
        }

        target = target.shiftRight(16);

        if(target.equals(BigInteger.ZERO)) {
            return BigInteger.valueOf(Utils.encodeCompactBits(BigInteger.ONE));
        }
        if(target.compareTo(MAX_TARGET) > 0) {
            return new BigInteger(MAX_BITS_STRING, 16);
        }

        return BigInteger.valueOf(Utils.encodeCompactBits(target));
    }

    public static BigInteger computeAsertTarget(int referenceBlockBits, BigInteger referenceBlockAncestorTime, BigInteger referenceBlockHeight,
                                                BigInteger evalBlockTime, BigInteger evalBlockHeight) {
        BigInteger refTarget = Utils.decodeCompactBits(referenceBlockBits);
        return computeAsertTarget(refTarget, referenceBlockAncestorTime, referenceBlockHeight, evalBlockTime, evalBlockHeight);
    }
}
