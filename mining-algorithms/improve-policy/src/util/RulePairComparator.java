/**
 * Mining Relationship-Based Access Control Policies
 * Copyright (C) 2020 Thang Bui
 * Copyright (C) 2020 Scott D. Stoller
 * Copyright (c) 2020 Stony Brook University
 * Copyright (c) 2020 Research Foundation of SUNY
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package util;

import java.util.Comparator;

public class RulePairComparator implements Comparator<Pair<Rule, Rule>> {
    
    @Override
    public int compare(Pair<Rule, Rule> p1, Pair<Rule, Rule> p2) {  
        QualityValue maxFirstPair;
        QualityValue minFirstPair;
        QualityValue maxSecondPair;
        QualityValue minSecondPair;
        if (p1.getFirst().getQuality().compareTo(p1.getSecond().getQuality()) >= 0){
            maxFirstPair = p1.getFirst().getQuality();
            minFirstPair = p1.getSecond().getQuality();
        } 
        else{ 
            maxFirstPair = p1.getSecond().getQuality();
            minFirstPair = p1.getFirst().getQuality();
        }
        
        if (p2.getFirst().getQuality().compareTo(p2.getSecond().getQuality()) >= 0){
            maxSecondPair = p2.getFirst().getQuality();
            minSecondPair = p2.getSecond().getQuality();
        } 
        else{ 
            maxSecondPair = p2.getSecond().getQuality();
            minSecondPair = p2.getFirst().getQuality();
        }
        if (maxFirstPair.compareTo(maxSecondPair) > 0){
            return -1;
        }
        if (maxFirstPair.compareTo(maxSecondPair) < 0){
            return 1;
        }
        if (minFirstPair.compareTo(minSecondPair) > 0){
            return -1;
        }
        if (minFirstPair.compareTo(minSecondPair) < 0){
            return 1;
        }      
        return 0;
    }
}
