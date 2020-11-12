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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents classes in policy
 * @author ThangBui
 */
public class Rule implements Serializable, Comparable<Rule>{
    private Class1 subjectType;
    private ArrayList<AtomicCondition> subjectCondition;
    private Class1 resourceType;
    private ArrayList<AtomicCondition> resourceCondition;
    private ArrayList<AtomicConstraint> constraint;
    private Set<String> actions;
    private List<Triple<String, String, String>> coveredUP;
    private QualityValue quality;
    
    // Constructors
    public Rule(){        
    }
    public Rule(Class1 subjectType, ArrayList<AtomicCondition> subjectCondition, Class1 resourceType, ArrayList<AtomicCondition> resourceCondition, ArrayList<AtomicConstraint> constraint, Set<String> actions){
        this.subjectType = subjectType;
        this.subjectCondition = subjectCondition;
        this.resourceType = resourceType;
        this.resourceCondition = resourceCondition;
        this.constraint = constraint;
        this.actions = actions;
    }
    public Rule(Rule r1){
        this.subjectCondition = new ArrayList<AtomicCondition>();
        this.resourceCondition = new ArrayList<AtomicCondition>();
        this.constraint = new ArrayList<AtomicConstraint>();
        this.subjectType = r1.getSubjectType();
        this.resourceType = r1.getResourceType();
        for (AtomicCondition ac:r1.getSubjectCondition()){
            this.subjectCondition.add(new AtomicCondition(ac));
        }    
        for (AtomicCondition ac:r1.getResourceCondition()){
            this.resourceCondition.add(new AtomicCondition(ac));
        }  
        for (AtomicConstraint ac:r1.getConstraint()){
            this.constraint.add(new AtomicConstraint(ac));
        }
        this.actions = new HashSet<String>(r1.getActions());
        if (r1.getCoveredUP() == null){
            this.coveredUP = null;
        }
        else{
            this.coveredUP = new ArrayList<Triple<String, String, String>>(r1.getCoveredUP());
        }
        if (r1.getQuality() == null){
            this.quality = null;
        }
        else{
            this.quality = new QualityValue(r1.getQuality().firstComponent, r1.getQuality().secondComponent, r1.getQuality().thirdComponent);
        }
               
    }
    
    // Accessor and Mutator methods
    public void setSubjectType(Class1 subjectType){
        this.subjectType = subjectType;
    }
    public void setSubjectCondition(ArrayList<AtomicCondition> subCondition){
        this.subjectCondition = subCondition;
    }
    public void setResourceType(Class1 resType){
        this.resourceType = resType;
    }
    public void setResourceCondition(ArrayList<AtomicCondition> resCondition){
        this.resourceCondition = resCondition;
    }
    public void setConstraint(ArrayList<AtomicConstraint> constraint){
        this.constraint = constraint;
    }
    public void setActions(Set<String> actions){
        this.actions = actions;
    }
    public Class1 getSubjectType(){
        return this.subjectType;
    }
    public ArrayList<AtomicCondition> getSubjectCondition(){
        return this.subjectCondition;
    }
    public Class1 getResourceType(){
        return this.resourceType;
    }
    public ArrayList<AtomicCondition> getResourceCondition(){
        return this.resourceCondition;
    }
    public ArrayList<AtomicConstraint> getConstraint(){
        return this.constraint;
    }
    public Set<String> getActions(){
        return this.actions;
    }
    public List<Triple<String, String, String>> getCoveredUP(){
        return this.coveredUP;
    }
    public void setCoveredUP(List<Triple<String, String, String>> coveredUP){
        this.coveredUP = coveredUP;
    }
    public QualityValue getQuality(){
        return this.quality;
    }
    public void setQuality(QualityValue value){
        this.quality = value;
    }
    
    // get WSC of a rule
    public int getWSC(){
        int result = 0;
        for (AtomicCondition ac:this.getSubjectCondition()){
            result+= ac.getConstant().size();
            result+= ac.getPath().size();
             if (ac.getIsNegative()){
                result += 1;
            }
        }
        for (AtomicCondition ac:this.getResourceCondition()){
            result+= ac.getConstant().size();
            result+= ac.getPath().size();
             if (ac.getIsNegative()){
                result += 1;
            }
        }
        for (AtomicConstraint ac:this.getConstraint()){
            result+= ac.getSubPath().size();
            result+= ac.getResPath().size();
             if (ac.getIsNegative()){
                result += 1;
            }
        }
        return result + this.getActions().size();
    }
    
    // get WSC of sub condition, res condition, and cons and return as an array
    public double[] getPartialWSC() {
    	double[] partialWSC = new double[3];
    	int result = 0;
    	for (AtomicCondition ac:this.getSubjectCondition()){
            result+= ac.getConstant().size();
            result+= ac.getPath().size();
        }
    	partialWSC[0] = result;
    	
    	result = 0;
    	for (AtomicCondition ac:this.getResourceCondition()){
            result+= ac.getConstant().size();
            result+= ac.getPath().size();
        }
    	partialWSC[1] = result;
    	
    	result = 0;
    	for (AtomicConstraint ac:this.getConstraint()){
            result+= ac.getSubPath().size();
            result+= ac.getResPath().size();
        }
    	partialWSC[2] = result;
    	
    	return partialWSC;
    }
    
    // get all subject attributes 
    public ArrayList<ArrayList<String>> getSubAttributes(){
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        for (AtomicCondition ac:this.subjectCondition){
            result.add(ac.getPath());
        }
        return result;
    }
    
    // get all resource attributes 
    public ArrayList<ArrayList<String>> getResAttributes(){
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        for (AtomicCondition ac:this.resourceCondition){
            result.add(ac.getPath());
        }
        return result;
    }
    
    /**
     * This method checks if this rule accepts the given UP relation.
     * @param tuple
     * @return
     */
    public boolean acceptUP(Triple<String, String, String> tuple){
        return getCoveredUP().contains(tuple);
    }
    
    @Override
    public String toString(){
        String s ="rule(";
        // subject
        if (this.subjectType != null){
            s = s + this.subjectType.getClassName() + "; ";
        }
        else {
            s = s + "; ";
        }
        if (!this.subjectCondition.isEmpty()){
            for (int i = 0; i < this.subjectCondition.size(); i++){
                if (i != 0){
                    s = s + " and ";
                }
                AtomicCondition ac = this.subjectCondition.get(i);
                
                s = s + ac.getPath().get(0);
                if (ac.getPath().size() > 1){
                    for (int j = 1; j < ac.getPath().size(); j++){
                        s = s + "." + ac.getPath().get(j);
                    }
                }
                s = s + " ";
                switch (ac.getConditionOperator()){
                    case IN:
                        s = s + "in ";
                        break;
                    case CONTAINS:
                        s = s + "contains ";
                        break;
                }
                s = s + "{";
                if (!ac.getConstant().isEmpty()){
                    for (String constant:ac.getConstant()){
                        s = s + constant + ",";
                    }
                    s = s.substring(0, s.length() - 1);
                }
                s = s + "}";     
                if (ac.getIsNegative()){
                    s = s + "(!=)";
                }
            }
            s = s + "; ";
        }
        else{
            s = s + "; ";
        }
        
        // resource
        if (this.resourceType != null){
            s = s + this.resourceType.getClassName() + "; ";
        }
        else {
            s = s + "; ";
        }
        if (!this.resourceCondition.isEmpty()){
            for (int i = 0; i < this.resourceCondition.size(); i++){
                if (i != 0){
                    s = s + " and ";
                }
                AtomicCondition ac = this.resourceCondition.get(i);
           
                s = s + ac.getPath().get(0);
                if (ac.getPath().size() > 1){
                    for (int j = 1; j < ac.getPath().size(); j++){
                        s = s + "." + ac.getPath().get(j);
                    }
                }
                s = s + " ";
                switch (ac.getConditionOperator()){
                    case IN:
                        s = s + "in ";
                        break;
                    case CONTAINS:
                        s = s + "contains ";
                        break;
                }
                s = s + "{";
                if (!ac.getConstant().isEmpty()){
                    for (String constant:ac.getConstant()){
                        s = s + constant + ",";
                    }
                    s = s.substring(0, s.length() - 1);
                }
                s = s + "}";
                if (ac.getIsNegative()){
                    s = s + "(!=)";
                }
            }
            s = s + "; ";
        }
        else{
            s = s + "; ";
        }
        
        // Constraint
        if (!this.constraint.isEmpty()){
            for (int i = 0; i < this.constraint.size(); i++){
                if (i != 0){
                    s = s + " and ";
                }
                AtomicConstraint ac = this.constraint.get(i);
  
                s = s + ac.getSubPath().get(0);
                if (ac.getSubPath().size() > 1){
                    for (int j = 1; j < ac.getSubPath().size(); j++){
                        s = s + "." + ac.getSubPath().get(j);
                    }
                }
                s = s + " ";
                switch (ac.getConstraintOperator()){
                    case IN:
                        s = s + "in ";
                        break;
                    case CONTAINS:
                        s = s + "contains ";
                        break;
                    case EQUALS_VALUE:
                        s = s + "= ";
                        break;
                    case SUPSETEQ:
                        s = s + "supseteq ";
                        break;
                    case SUBSETEQ:
                        s = s + "subseteq ";
                        break;
                    case EQUALS_SET:
                        s = s + "= ";
                        break;
                }
                s = s + ac.getResPath().get(0);
                if (ac.getResPath().size() > 1){
                    for (int j = 1; j < ac.getResPath().size(); j++){
                        s = s + "." + ac.getResPath().get(j);
                    }
                }
                if (ac.getIsNegative()){
                    s = s + "(!=)";
                }
            }
            s = s + "; ";
        }
        else{
            s = s + "; ";
        }
        
        // rule operations/actions
        s = s + "{";
        for (String action:this.actions){
            s = s + action + ", ";
        }
        s = s.substring(0, s.length() -2);
        return s + "})";
    }
    
    @Override
    public int compareTo(Rule r1) {  
        return this.getQuality().compareTo(r1.getQuality()) * -1;
    }
    
//    @Override
//    public boolean equals(Object r){
//        if (!(r instanceof Rule)){
//            return false;
//        }
//        Rule r1 = (Rule)r;
//        if (!this.getSubjectType().getClassName().equals(r1.getSubjectType().getClassName())){
//            return false;
//        }
//        if (!this.getSubjectCondition().containsAll(r1.getSubjectCondition()) || !r1.getSubjectCondition().containsAll(this.getSubjectCondition())){
//            return false;
//        }
//        if (!this.getResourceType().getClassName().equals(r1.getResourceType().getClassName())){
//            return false;
//        }
//        if (!this.getResourceCondition().containsAll(r1.getResourceCondition()) || !r1.getResourceCondition().containsAll(this.getResourceCondition())){
//            return false;
//        }
//        if (!this.getConstraint().containsAll(r1.getConstraint()) || !r1.getConstraint().containsAll(this.getConstraint())){
//            return false;
//        }
//        if (this.getActions().size() != r1.getActions().size() || !this.getActions().containsAll(r1.getActions()) || !r1.getActions().containsAll(this.getActions())){
//            return false;
//        }
//        return true;
//    }
//    
//    @Override
//    public int hashCode(){
//        return new HashCodeBuilder(17, 37).
//                append(this.subjectType).
//                append(this.resourceType).
//                append(this.subjectCondition).
//                append(this.resourceCondition).
//                append(this.constraint).
//                append(this.actions).
//                toHashCode();
//    }
}
