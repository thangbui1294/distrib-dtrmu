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

/**
 * This class represents a class object in class model
 * @author ThangBui
 */
public class Class1 implements Serializable{
    private String className;
    private Class1 parentClass;
    // map from field name to type and multiplicity
    private Map<String, FieldType> attributes;
    
    // Constructors
    public Class1(String className){
        this.className = className;
    }
    public Class1(String className, Class1 parentClass, Map<String, FieldType> attrs){
        this.className = className;
        this.parentClass = parentClass;
        this.attributes = attrs;
    }
    /**
     * This method returns all attributes of this class, included inherited attributes from parent classes
     * @return 
     */
    public Map<String, FieldType> getAllAttributes(){
        return getAllAttributesHelper(this);
    }
    
    
    public Map<String, FieldType> getAllAttributesHelper(Class1 class1){      
        Map<String, FieldType> results = new HashMap<String, FieldType>();
        if (class1.attributes != null){
            results.putAll(class1.attributes);
        }
        if (class1.parentClass != null){
            results.putAll(getAllAttributesHelper(class1.parentClass));
        }
        return results;
    }
    
    // Accessor and mutator methods
    public void setClassName(String className){
        this.className = className;
    }
    
    public String getClassName(){
        return this.className;
    }
    
    public void setParentClass(Class1 parent){
        this.parentClass = parent;
    }
    
    public Class1 getParentClass(){
        return this.parentClass;
    }
    
    public void setAttributes(Map<String, FieldType> attributes){
        this.attributes = attributes;
    }
    
    public Map<String, FieldType> getAttributes(){
        return this.attributes;
    }
    
    @Override
    public String toString(){
        String s = "class(" + this.className + "; ";
        if (this.parentClass == null){
            s = s + "; ";
        }
        else{
            s = s + this.parentClass.className + "; ";
        }
        if (this.attributes != null && this.attributes.size() > 0){
            for (Map.Entry<String, FieldType> entry:this.attributes.entrySet()){
                s+= entry.getKey() + ":" + entry.getValue() + "; ";
            }           
        }
        return s.substring(0, s.length() - 2) + ")";
    }
}
