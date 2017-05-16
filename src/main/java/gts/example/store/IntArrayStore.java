package gts.example.store;

import gts.example.DistinctStore;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

/**
 * naive boolean array store with 1 billion entries, synchronized access
 *
 * @since 5/15/17.
 */
public class IntArrayStore implements DistinctStore {

    private final static boolean bigArray[] = new boolean[1000000000];
    private final Object rwLock = new Object();
    private int size = 0, newUniqueCount = 0;
    private long dupeCount = 0l;

    @Override
    public boolean getSetPresent(Integer intVal) {
        synchronized (rwLock) {
            if (bigArray[intVal]) {
                dupeCount++;
                return true;
            } else {
                bigArray[intVal] = true;
                size++;
                newUniqueCount++;
                return false;
            }
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public Triple<Integer, Long, Integer> checkpoint() {
        Triple<Integer, Long, Integer> z = null;
        synchronized (rwLock) {
            z = new ImmutableTriple(newUniqueCount, dupeCount, size);
            newUniqueCount = 0;
            dupeCount = 0l;
        }
        return z;
    }
}
