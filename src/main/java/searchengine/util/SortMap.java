package searchengine.util;

import searchengine.model.Page;

import java.util.*;

public class SortMap {
    public static LinkedHashMap<Page, Float> sortPageFloatLinkedHashMapByValueDesc(LinkedHashMap linkedHashMap) {
        List<Map.Entry<Page, Float>> entryList = new ArrayList<>(linkedHashMap.entrySet());


        entryList.sort(Map.Entry.<Page, Float>comparingByValue().reversed());

        LinkedHashMap<Page, Float> sortedByValue = new LinkedHashMap<>();
        for (Map.Entry<Page, Float> entry : entryList) {
            sortedByValue.put(entry.getKey(), entry.getValue());
        }

        sortedByValue.forEach((k, v) -> System.out.println(k.getPath() + ": " + v));
        return sortedByValue;
    }
}
