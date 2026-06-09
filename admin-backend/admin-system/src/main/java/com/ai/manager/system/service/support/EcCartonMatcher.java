package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcCarton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class EcCartonMatcher {

    private EcCartonMatcher() {
    }

    /**
     * 在可旋转放置前提下，找到单品尺寸均可容纳、且体积最小（最贴近）的纸箱。
     */
    public static EcCarton findBestFit(List<EcCarton> cartons,
                                       BigDecimal productLengthCm,
                                       BigDecimal productWidthCm,
                                       BigDecimal productHeightCm) {
        if (cartons == null || cartons.isEmpty()) {
            return null;
        }
        double[] productDims = toSortedDims(productLengthCm, productWidthCm, productHeightCm);
        if (productDims == null) {
            return null;
        }

        EcCarton best = null;
        double bestVolume = Double.MAX_VALUE;
        double bestExcess = Double.MAX_VALUE;

        for (EcCarton carton : cartons) {
            double[] cartonDims = toSortedDims(carton.getLengthCm(), carton.getWidthCm(), carton.getHeightCm());
            if (cartonDims == null || !fits(productDims, cartonDims)) {
                continue;
            }
            double volume = cartonDims[0] * cartonDims[1] * cartonDims[2];
            double excess = (cartonDims[0] - productDims[0])
                    + (cartonDims[1] - productDims[1])
                    + (cartonDims[2] - productDims[2]);
            if (volume < bestVolume - 1e-9
                    || (Math.abs(volume - bestVolume) < 1e-9 && excess < bestExcess - 1e-9)) {
                best = carton;
                bestVolume = volume;
                bestExcess = excess;
            }
        }
        return best;
    }

    private static boolean fits(double[] productDims, double[] cartonDims) {
        return productDims[0] <= cartonDims[0]
                && productDims[1] <= cartonDims[1]
                && productDims[2] <= cartonDims[2];
    }

    private static double[] toSortedDims(BigDecimal length, BigDecimal width, BigDecimal height) {
        if (length == null || width == null || height == null) {
            return null;
        }
        if (length.signum() <= 0 || width.signum() <= 0 || height.signum() <= 0) {
            return null;
        }
        double[] dims = {length.doubleValue(), width.doubleValue(), height.doubleValue()};
        Arrays.sort(dims);
        return dims;
    }

    public static List<EcCarton> preferFactoryCartons(List<EcCarton> all, Long factoryId) {
        if (factoryId == null || all == null) {
            return all == null ? List.of() : all;
        }
        List<EcCarton> sameFactory = new ArrayList<>();
        List<EcCarton> others = new ArrayList<>();
        for (EcCarton carton : all) {
            if (factoryId.equals(carton.getFactoryId())) {
                sameFactory.add(carton);
            } else {
                others.add(carton);
            }
        }
        if (!sameFactory.isEmpty()) {
            return sameFactory;
        }
        return others;
    }
}
