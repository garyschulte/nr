package gts.example;

import org.apache.commons.lang3.tuple.Triple;

/**
 * @since 5/15/17.
 */
public interface DistinctStore {
    /**
     * given an integer determine if it has been sent previously and if not add it to the list
     * @return whether the integer was present already
     */
    boolean getSetPresent(Integer intVal);

    /**
     * get the current size of the list of distinct numbers sent
     */
    int getSize();

    /**
     * return a triple representing thethe number of new unique (left) values and dupes (middle) since this method was last called,
     * and the current store size (right)
     *
     */
    Triple<Integer, Long, Integer> checkpoint();

}
