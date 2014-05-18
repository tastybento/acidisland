package com.wasteofplastic.acidisland;

import java.util.*;

/**
 * @author ben Provides a descending order sort
 */
public class MapUtil {
    public static <Key, Value extends Comparable<? super Value>> Map<Key, Value> sortByValue(Map<Key, Value> map) {
	List<Map.Entry<Key, Value>> list = new LinkedList<Map.Entry<Key, Value>>(map.entrySet());
	Collections.sort(list, new Comparator<Map.Entry<Key, Value>>() {
	    public int compare(Map.Entry<Key, Value> o1, Map.Entry<Key, Value> o2) {
		// Switch these two if you want ascending
		return (o2.getValue()).compareTo(o1.getValue());
	    }
	});

	Map<Key, Value> result = new LinkedHashMap<Key, Value>();
	for (Map.Entry<Key, Value> entry : list) {
	    result.put(entry.getKey(), entry.getValue());
	}
	return result;
    }
}
