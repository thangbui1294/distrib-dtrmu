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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.HashCodeBuilder;
//import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class represents an atomic condition defined in the policy language.
 * @author ThangBui
 */
public class AtomicCondition implements Serializable {
    private ArrayList<String> path;
    private Set<String> constant;
    private ConditionOperator op;
    private boolean isNegative;
    
    // Constructors
    public AtomicCondition(){
        
    }
    
    public AtomicCondition(ArrayList<String> path, Set<String> constant, ConditionOperator op){
        this.path = path;
        this.constant = constant;
        this.op = op;
    }
    
    public AtomicCondition(AtomicCondition ac){
        this.path = new ArrayList<String>(ac.getPath());
        this.constant = new HashSet<String>(ac.getConstant());
        this.op = ac.getConditionOperator();
    }
    
    // Accessor and mutator methods
    public void setPath(ArrayList<String> path){
        this.path = path;
    }
    public void setConstant(Set<String> constant){
        this.constant = constant;
    }
    public void setConditionOperator(ConditionOperator op){
        this.op = op;
    }
    public ArrayList<String> getPath(){
        return this.path;
    }
    public Set<String> getConstant(){
        return this.constant;
    }
    public ConditionOperator getConditionOperator(){
        return this.op;
    }    
    public void setIsNegative(Boolean b){
        this.isNegative = b;
    }
    public Boolean getIsNegative(){
        return this.isNegative;
    }
    
    @Override
    public boolean equals(Object ac){
        if (!(ac instanceof AtomicCondition)){
            return false;
        }
        AtomicCondition ac1 = (AtomicCondition)ac;
        if (!this.path.equals(ac1.getPath())){
            return false;
        }
        if (this.op != ac1.getConditionOperator()){
            return false;
        }
        if (!this.constant.containsAll(ac1.getConstant()) || !ac1.getConstant().containsAll(this.constant)){
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode(){
        HashCodeBuilder hashCode = new HashCodeBuilder(17, 37);
        hashCode.append(this.op);
        for (String c:this.constant){
            hashCode.append(c);
        }
        for (String p:this.path){
            hashCode.append(p);
        }
        return hashCode.hashCode();
    }
    
    @Override
    public String toString(){
        String s = "<";
        s = s + this.getPath().get(0);
        if (this.getPath().size() > 1){
            for (int j = 1; j < this.getPath().size(); j++){
                s = s + "." + this.getPath().get(j);
            }
        }
        s = s + " ";
        switch (this.getConditionOperator()){
            case IN:
                s = s + "in ";
                break;
            case CONTAINS:
                s = s + "contains ";
                break;
        }
        s = s + "{";
        if (!this.getConstant().isEmpty()){
            for (String constant:this.getConstant()){
                s = s + constant + ",";
            }
            s = s.substring(0, s.length() - 1);
        }
        s = s + "}";
        return s + ">";
    }
}
