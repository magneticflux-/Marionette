package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.google.common.collect.LinkedHashMultiset;

import java.util.Iterator;

/**
 * Created by Mitchell on 6/22/2016.
 */
public class SMBNoveltyBehaviorList {
    private final LinkedHashMultiset<MarioBrosData> behaviorList = LinkedHashMultiset.create();

    public SMBNoveltyBehaviorList() {
    }

    public LinkedHashMultiset<MarioBrosData> getBehaviorList() {
        return behaviorList;
    }


    public boolean add(MarioBrosData marioBrosData, int maxDuplicateCount, int maxEntryCount) {
        if (behaviorList.count(marioBrosData) < maxDuplicateCount) {
            boolean toReturn = behaviorList.add(marioBrosData);

            Iterator<MarioBrosData> iterator = behaviorList.iterator();
            while (behaviorList.entrySet().size() > maxEntryCount) {
                iterator.next();
                iterator.remove();
            }

            return toReturn;
        } else
            return false;
    }
}
