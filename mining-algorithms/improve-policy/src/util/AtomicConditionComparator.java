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

/**
 *
 * @author Thang
 */
public class AtomicConditionComparator implements Comparator<Pair<AttributePathType, AtomicCondition>> {
    @Override
    public int compare(Pair<AttributePathType, AtomicCondition> ac1, Pair<AttributePathType, AtomicCondition> ac2) {
        // size of set of values (descending)
        if (ac1.getSecond().getConstant().size() < ac2.getSecond().getConstant().size()){
            return 1;
        }
        if (ac1.getSecond().getConstant().size() > ac2.getSecond().getConstant().size()){
            return -1;
        }
        // path length
        if (ac1.getSecond().getPath().size() < ac2.getSecond().getPath().size()){
            return 1;
        }
        if (ac1.getSecond().getPath().size() > ac2.getSecond().getPath().size()){
            return -1;
        }
        // id attribute before other attributes
        if (ac1.getSecond().getPath().contains("id")  && !ac2.getSecond().getPath().contains("id")){
            return -1;
        }
        if (ac2.getSecond().getPath().contains("id")  && !ac1.getSecond().getPath().contains("id")){
            return 1;
        } 
        // compare using string representation of the conditions
        return ac1.getSecond().compareTo(ac2.getSecond());
    }
}
