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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import util.FieldType.Multiplicity;

/**
 * This class represents value of a field in attribute data
 * @author Thang
 */
public class FieldValue implements Serializable{
    
    private boolean bool;
    private String objId;
    private Set<String> setObjId;
    private boolean isUnknown;
    
    // Accesssor and Mutator methods
    public void setBool(boolean bool){
        this.bool = bool;
    }
    public boolean getBool(){
        return this.bool;
    }
    public void setIsUnknown(boolean isUnknown){
        this.isUnknown = isUnknown;
    }
    public boolean getIsUnknown(){
        return this.isUnknown;
    }
    public void setObjId(String objId){
        this.objId = objId;
    }
    public String getObjId(){
        return this.objId;
    }
    public void setSetObjId(Set<String> setObjId){
        this.setObjId = setObjId;
    }
    public Set<String> getSetObjId(){
        return this.setObjId;
    }
    
    /**
     * This method adds an element to the setObjId
     * @param e 
     */
    public void addToSetObjId(String e){
        this.setObjId.add(e);
    }
    // Printer
    public String print(String fieldName, Class1 class1){
        Map<String, FieldType> attributes = new HashMap<String, FieldType>();
        if (class1.getParentClass() != null){
            attributes = class1.getAllAttributes();
        }
        else attributes = class1.getAttributes();
        Multiplicity mul = attributes.get(fieldName).getMultiplicity();
        if (attributes.get(fieldName).getIsBoolean()){
            return "" + this.bool;
        }
        else if ((mul == Multiplicity.ONE || mul == Multiplicity.OPTIONAL) && this.objId != null){
            return this.objId;
        }
        else if (mul == Multiplicity.MANY && this.setObjId != null){
            String s = "{";
            for (String e:this.setObjId){
                s += e + ", ";
            }
            s = s.substring(0, s.length()-2);
            s += "}";
            return s;
        }
        return "null";
    }
}
