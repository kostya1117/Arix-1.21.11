package malte0811.ferritecore.mixin.accessors;

import java.util.BitSet;

public interface BitSetDVSAccess extends DiscreteVSAccess {
    BitSet getStorage();

    int getXMin();

    int getYMin();

    int getZMin();

    int getXMax();

    int getYMax();

    int getZMax();
}
