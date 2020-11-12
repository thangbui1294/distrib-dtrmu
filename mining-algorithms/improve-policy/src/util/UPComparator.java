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

/**
 *
 * @author ThangBui
 */

import java.util.Comparator;

public class UPComparator implements
        Comparator<Triple<String, String, String>> {
    private Config config;
    
    public UPComparator(Config config) {
        this.config = config;
    }
    
    @Override
    public int compare(Triple<String, String, String> up1,
            Triple<String, String, String> up2) {      
        // Compare using permCount
        int permCount1 = 0;
        int permCount2 = 0;
        for (Pair<String, String> perm:config.getUPListMapOnRes().get(config.getObjectModel().get(up1.getSecond()).getClass1()).get(up1.getSecond())){
            if (perm.getSecond().equals(up1.getThird())){
                permCount1++;
            }
        }
        for (Pair<String, String> perm:config.getUPListMapOnRes().get(config.getObjectModel().get(up2.getSecond()).getClass1()).get(up2.getSecond())){
            if (perm.getSecond().equals(up2.getThird())){
                permCount2++;
            }
        }
        if (permCount1 > permCount2){
            return -1;
        }
        if (permCount1 < permCount2){
            return 1;
        }
        
        // Compare using sub count
        int subCount1 = config.getUPListMapOnSub().get(config.getObjectModel().get(up1.getFirst()).getClass1()).get(up1.getFirst()).size();
        int subCount2 = config.getUPListMapOnSub().get(config.getObjectModel().get(up2.getFirst()).getClass1()).get(up2.getFirst()).size();
        if (subCount1 > subCount2){
            return -1;
        }
        if (subCount1 < subCount2){
            return 1;
        }      
        return up1.toString().compareTo(up2.toString()) * -1;
    }   
}
