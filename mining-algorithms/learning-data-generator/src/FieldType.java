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
 
package learningdatagenerator;

import java.io.Serializable;

/**
 * This class represents a field type used in class model
 * @author ThangBui
 */

public class FieldType implements Serializable{
    public enum Multiplicity{
        ONE, MANY, OPTIONAL
    }
    // if isBoolean==TRUE, the field type is boolean
    private boolean isBoolean;
    // if isBoolean==FALSE, the field type is the specified class.
    private Class1 type;
    private Multiplicity mul;
    
    // Constructor
    public FieldType(boolean isBoolean, Class1 type, Multiplicity mul){
        this.isBoolean = isBoolean;
        this.type = type;
        this.mul = mul;
    }
    
    // Accessor and mutator methods
    public void setIsBoolean(boolean isBoolean){
        this.isBoolean = isBoolean;
    }
    public boolean getIsBoolean(){
        return this.isBoolean;
    }
    public void setType(Class1 type){
        this.type = type;
    }
    public Class1 getType(){
        return this.type;
    }
    public void setMultiplicity(Multiplicity mul){
        this.mul = mul;
    }
    public Multiplicity getMultiplicity(){
        return this.mul;
    }
    
    @Override
    public String toString(){
        if (this.isBoolean){
            return "Boolean";
        }
        switch (this.mul) {
            case ONE:
                return this.type.getClassName();
            case OPTIONAL:
                return this.type.getClassName()+ "?";
            default:
                return this.type.getClassName() + "*";
        }
    }
}


