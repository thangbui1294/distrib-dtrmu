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
import java.util.ArrayList;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class represents an atomic constraint defined in the policy language
 * @author ThangBui
 */
public class AtomicConstraint implements Serializable, Comparable{
    private ArrayList<String> subPath;
    private ArrayList<String> resPath;
    ConstraintOperator op;
    boolean isNegative;
    
    // Constructors
    public AtomicConstraint(){
        
    }
    public AtomicConstraint(ArrayList<String> subPath, ArrayList<String> resPath, ConstraintOperator op){
        this.subPath = subPath;
        this.resPath = resPath;
        this.op = op;
    }
    public AtomicConstraint(AtomicConstraint ac){
        this.subPath = new ArrayList<String>(ac.getSubPath());
        this.resPath = new ArrayList<String>(ac.getResPath());
        this.op = ac.getConstraintOperator();
        this.isNegative = ac.getIsNegative();
    }
    
    // Accessor and Mutator methods
    public void setSubPath(ArrayList<String> subPath){
        this.subPath = subPath;
    }
    public void setResPath(ArrayList<String> resPath){
        this.resPath = resPath;
    }
    public void setConstraintOperator(ConstraintOperator op){
        this.op = op;
    }
    public ArrayList<String> getSubPath(){
        return this.subPath;
    }
    public ArrayList<String> getResPath(){
        return this.resPath;
    }
    public ConstraintOperator getConstraintOperator(){
        return this.op;
    }
    public void setIsNegative(Boolean b){
        this.isNegative = b;
    }
    public Boolean getIsNegative(){
        return this.isNegative;
    }
    
    @Override
    public String toString(){
        String s = "";
        s = s + this.getSubPath().get(0);
        if (this.getSubPath().size() > 1){
            for (int j = 1; j < this.getSubPath().size(); j++){
                s = s + "." + this.getSubPath().get(j);
            }
        }
        switch (this.getConstraintOperator()){
            case IN:
                s = s + " in ";
                break;
            case CONTAINS:
                s = s + " contains ";
                break;
            case EQUALS_VALUE:
                s = s + " = ";
                break;
            case SUPSETEQ:
                s = s + " supseteq ";
                break;
            case SUBSETEQ:
                s = s + " subseteq ";
                break;
            case EQUALS_SET:
                s = s + " = ";
                break;
        }
        s = s + this.getResPath().get(0);
        if (this.getResPath().size() > 1){
            for (int j = 1; j < this.getResPath().size(); j++){
                s = s + "." + this.getResPath().get(j);
            }
        }
        return s;
    }
    
    @Override
    public boolean equals(Object ac){
            if (!(ac instanceof AtomicConstraint)){
                return false;
            }
            AtomicConstraint ac1 = (AtomicConstraint)ac;
            return (this.op == ac1.getConstraintOperator() && this.subPath.equals(ac1.getSubPath()) && this.resPath.equals(ac1.getResPath()) && this.isNegative == ac1.getIsNegative());
    }
    
    @Override
    public int hashCode(){
        HashCodeBuilder hashCode = new HashCodeBuilder(17, 37);
        hashCode.append(this.op);
        for (String s: this.subPath){
            hashCode.append(s);
        }
        for (String s: this.resPath){
            hashCode.append(s);
        }
        hashCode.append(this.isNegative);
        return hashCode.toHashCode();
    }
    
    @Override
    public int compareTo(Object ac){
        return this.toString().compareTo(ac.toString());
    }
}
