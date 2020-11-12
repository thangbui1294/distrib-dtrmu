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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration class is used for maintaining the configuration of various
 * input parameters
 *
 * @author Zhongyuan Xu
 *
 */

public class Config{
    
    //Thang
    private Map<String, Object1> objectModel;
    private Map<String, Integer> objectCount;
    private Map<String, Class1> classModel;
    private LinkedList<Rule> ruleModel;
    private LinkedList<Rule> minedRulesFromDT;
    private String seedString;
    // Set of up triples grouped by known rules from the case studies
    private Map<Rule, Set<Triple<String, String, String>>> upRelation;
    // adjacency map: specifies neighbors of each class with the corresponding association
    private Map<String, Set<Pair<String,String>>> adjacencyList;
    // list of all up triples
    private ArrayList<Triple<String, String, String>> upList;
    // set of all up triples
    private Set<Triple<String, String, String>> upSet;
    // list of all up triples grouped by class type and subject
    private Map<String, Map<String, Set<Pair<String, String>>>> upListMapOnSub;
    // list of all up triples grouped by class type and resource
    private Map<String, Map<String, Set<Pair<String, String>>>> upListMapOnRes;
    // list of all objects grouped by class type
    private Map<String, ArrayList<Object1>> objectList;
    // map of all class attribute domainsize
    private Map<String, Set<String>> classAttrDomainSizes;
    // input rule lists with its corresponding number of UPs
    ArrayList<Pair<Integer, Rule>> ruleListWithUP;
	// total num of fields of all objects
    private int totalFieldNum;
    
    // parameters read from config file
    private int subConstraintExtraDistance;
    private int resConstraintExtraDistance;
    private int totalConstraintLengthPathLimit;
    private int subConditionPathLimit;
    private int resConditionPathLimit;
    private int numConstraintLimit;
    private int removeConditionThreshold;
    private int limitConstraintSizeAll;
    private int limitConstraintSizeHalf;
    private boolean isOneConditionPerPathRestricted;
    private boolean compareCoveredUPNum;
    private boolean compareOriginalInputRule;
    private int[] policySize;
    private int numPoliciesPerSize;
    private String policyName;
    private String attributeDataPath;
    private String minedRulesFromDTPath;
    private String outputPath;
	private String outputFile;
    private boolean overassignmentDetection;
    private boolean underassignmentDetection;
    private double[] taus;
    private double[] alphas;
    private double[] ruleUnderAssignmentWeights;
    private double[] policyUnderAssignmentWeights;
    //private double noiseLevel;
    private String noiseDataFileInputPath;
    private double[] noiseLevels;
    
    private boolean batchOptimization;
    private int batchSize;
    
    private Set<Triple<String, String, String>> overAssigmentsCurrentlyAllowed;
    
    // field used for mining from log only
    private Set<Triple<String, String, String>> rejectedUPFromLog;
    private String logDataFileInputPath;
    private Set<Triple<String, String, String>> removedTuplesFromLog;
    private double[] logCompletenesses;
    private Set<Triple<String, String, String>> additionalTuplesFromMinedPolicies;
    
    // fields for optimization
    private Map<String, Map<String, FieldType>> classAtrributes;
    private Map<List<AtomicCondition>, Map<String, Set<String>>> conditionMeanings;
    private Map<AtomicCondition, Map<String, Set<String>>> atomicConditionMeanings;
    private Map<List<AtomicConstraint>, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>> constraintMeanings;
    private Map<AtomicConstraint, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>> atomicConstraintMeanings;
    // End Thang
    
    private HashSet<Triple<String, String, String>> overassignmentUP;
    
    private HashSet<Triple<String, String, String>> detectedOverassignmentUP;
    
    private HashSet<Triple<String, String, String>> underassignmentUP;
    
    private HashSet<Triple<String, String, String>> detectedUnderassignmentUP;
    
    public Config(){
        // Thang
        objectModel = new HashMap<String, Object1>();
        objectCount = new HashMap<String, Integer>();
        classModel = new HashMap<String, Class1>();
        ruleModel = new LinkedList<Rule>();
        minedRulesFromDT = new LinkedList<Rule>();
        upRelation = new HashMap<Rule, Set<Triple<String, String, String>>>();
        adjacencyList = new HashMap<String, Set<Pair<String,String>>>();
        upList = new ArrayList<Triple<String, String, String>>();
        upListMapOnSub = new HashMap<String, Map<String, Set<Pair<String, String>>>>();
        upListMapOnRes = new HashMap<String, Map<String, Set<Pair<String, String>>>>();
        objectList = new HashMap<String, ArrayList<Object1>>();
        classAttrDomainSizes = new HashMap<String, Set<String>>();
        ruleListWithUP = new ArrayList<Pair<Integer, Rule>>();
        
        overassignmentUP = new HashSet<Triple<String, String, String>>();
        underassignmentUP = new HashSet<Triple<String, String, String>>();
        
        detectedOverassignmentUP = new HashSet<Triple<String, String, String>>();
        detectedUnderassignmentUP = new HashSet<Triple<String, String, String>>();
        
        overAssigmentsCurrentlyAllowed = new HashSet<Triple<String, String, String>>();
        
        rejectedUPFromLog = new HashSet<Triple<String, String, String>>();
        removedTuplesFromLog = new HashSet<Triple<String, String, String>>();
        additionalTuplesFromMinedPolicies = new HashSet<Triple<String, String, String>>();
        
        classAtrributes = new HashMap<String, Map<String, FieldType>>() ;
        conditionMeanings = new HashMap<List<AtomicCondition>, Map<String, Set<String>>>() ;
        atomicConditionMeanings = new HashMap<AtomicCondition, Map<String, Set<String>>>();
        constraintMeanings = new HashMap<List<AtomicConstraint>, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>>(50);
        atomicConstraintMeanings = new HashMap<AtomicConstraint, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>>(1000);
    }
    
    
    public HashSet<Triple<String, String, String>> getOverassignmentUP() {
        return overassignmentUP;
    }
    
    public void setOverassignmentUP(
            HashSet<Triple<String, String, String>> overassignmentUP) {
        this.overassignmentUP = overassignmentUP;
    }
    
    public HashSet<Triple<String, String, String>> getUnderassignmentUP() {
        return underassignmentUP;
    }
    
    public void setUnderassignmentUP(
            HashSet<Triple<String, String, String>> underassignmentUP) {
        this.underassignmentUP = underassignmentUP;
    }
    
    public HashSet<Triple<String, String, String>> getDetectedOverassignmentUP() {
        return detectedOverassignmentUP;
    }
    
    public void setDetectedOverassignmentUP(
            HashSet<Triple<String, String, String>> detectedOverassignmentUP) {
        this.detectedOverassignmentUP = detectedOverassignmentUP;
    }
    
    public HashSet<Triple<String, String, String>> getDetectedUnderassignmentUP() {
        return detectedUnderassignmentUP;
    }
    
    public void setDetectedUnderassignmentUP(
            HashSet<Triple<String, String, String>> detectedUnderassignmentUP) {
        this.detectedUnderassignmentUP = detectedUnderassignmentUP;
    }
    
    
    public Map<String, Set<String>> getClassAttrDomainSizes(){
        return this.classAttrDomainSizes;
    }
    
    public void setClassAttrDomainSizes(Map<String, Set<String>> map){
        this.classAttrDomainSizes = map;
    }
    
    public ArrayList<Pair<Integer, Rule>> getRuleListWithUP(){
        return this.ruleListWithUP;
    } 
    
    public void setRuleListWithUP(ArrayList<Pair<Integer, Rule>> list){
        this.ruleListWithUP = list;
    }
    
    //Thang
    // class model
    public Map<String, Class1> getClassModel(){
        return this.classModel;
    }
    
    public void setClassModel(Map<String, Class1> classModel){
        this.classModel = classModel;
    }
    
    // object model
    public Map<String, Object1> getObjectModel(){
        return this.objectModel;
    }
    
    public void setObjectModel(Map<String, Object1> objectModel){
        this.objectModel = objectModel;
    }
    
    public Map<String, Integer> getObjectCount(){
        return this.objectCount;
    }
    
    public void setObjectCount(Map<String, Integer> objectCount){
        this.objectCount = objectCount;
    }
    
    // rule model
    public LinkedList<Rule> getRuleModel(){
        return this.ruleModel;
    }
    
    public void setRuleModel(LinkedList<Rule> ruleModel){
        this.ruleModel = ruleModel;
    }
    
    //mined rules from decision tree learning
    public LinkedList<Rule> getMinedRulesFromDT(){
        return this.minedRulesFromDT;
    }
    
    public void setMinedRulesFromDT(LinkedList<Rule> rules){
        this.minedRulesFromDT = rules;
    }
    
    // seed String
    public String getSeedString(){
        return this.seedString;
    }
    
    public void setSeedString(String seedString){
        this.seedString = seedString;
    }
    
    // upRelation
    public Map<Rule, Set<Triple<String,String,String>>> getUPRelation(){
        return this.upRelation;
    }
    
    public void setUPRelation(Map<Rule, Set<Triple<String,String,String>>> upRelation){
        this.upRelation = upRelation;
    }
    
    // adjacency list
    public Map<String, Set<Pair<String,String>>> getAdjacencyList(){
        return this.adjacencyList;
    }
    
    public void setAdjacencyList(Map<String, Set<Pair<String,String>>> aList){
        this.adjacencyList = aList;
    }
    
    // ups list
    public ArrayList<Triple<String, String, String>> getUPList(){
        return this.upList;
    }
    
    public void setUPList(ArrayList<Triple<String, String, String>> upList){
        this.upList = upList;
    }
    
    // ups set
    public Set<Triple<String, String, String>> getUPSet(){
        return this.upSet;
    }
    
    public void setUPSet(Set<Triple<String, String, String>> upSet){
        this.upSet = upSet;
    }
    
    // upListMapOnSub
    public Map<String, Map<String, Set<Pair<String, String>>>> getUPListMapOnSub(){
        return this.upListMapOnSub;
    }
    
    public void setUPListMapOnSub(Map<String, Map<String, Set<Pair<String, String>>>> upList){
        this.upListMapOnSub = upList;
    }
    
    // upListMapOnRes
    public Map<String, Map<String, Set<Pair<String, String>>>> getUPListMapOnRes(){
        return this.upListMapOnRes;
    }
    
    public void setUPListMapOnRes(Map<String, Map<String, Set<Pair<String, String>>>> upList){
        this.upListMapOnRes = upList;
    }
    
    // subject List
    public Map<String, ArrayList<Object1>> getObjectList(){
        return this.objectList;
    }
    
    public void setObjectList(Map<String, ArrayList<Object1>> objectList){
        this.objectList = objectList;
    }
	
	// total field num
	public int getTotalFieldNum() {
		return totalFieldNum;
	}

	public void setTotalFieldNum(int totalFieldNum) {
		this.totalFieldNum = totalFieldNum;
	}
    
    // config parameters
    public int getSubConstraintExtraDistance(){
        return this.subConstraintExtraDistance;
    }
    
    public void setSubConstraintExtraDistance(int param){
        this.subConstraintExtraDistance = param;
    }
    
    public int getResConstraintExtraDistance(){
        return this.resConstraintExtraDistance;
    }
    
    public void setResConstraintExtraDistance(int param){
        this.resConstraintExtraDistance = param;
    }
    
    public int getTotalConstraintLengthPathLimit(){
        return this.totalConstraintLengthPathLimit;
    }
    
    public void setTotalConstraintLengthPathLimit(int param){
        this.totalConstraintLengthPathLimit = param;
    }
    
    public int getSubConditionPathLimit(){
        return this.subConditionPathLimit;
    }
    
    public void setSubConditionPathLimit(int param){
        this.subConditionPathLimit = param;
    }
    
    public int getResConditionPathLimit(){
        return this.resConditionPathLimit;
    }
    
    public void setResConditionPathLimit(int param){
        this.resConditionPathLimit = param;
    }
    
    public int getNumConstraintLimit(){
        return this.numConstraintLimit;
    }
    
    public void setNumConstraintLimit(int param){
        this.numConstraintLimit = param;
    }
    
    public int getRemoveConditionThreshold(){
        return this.removeConditionThreshold;
    }
    
    public void setRemoveConditionThreshold(int param){
        this.removeConditionThreshold = param;
    }
    
    public int getLimitConstraintSizeAll(){
        return this.limitConstraintSizeAll;
    }
    
    public void setLimitConstraintSizeAll(int param){
        this.limitConstraintSizeAll = param;
    }
    
    public int getLimitConstraintSizeHalf(){
        return this.limitConstraintSizeHalf;
    }
    
    public void setLimitConstraintSizeHalf(int param){
        this.limitConstraintSizeHalf = param;
    }
    
    public boolean getIsOneConditionPerPathRestricted(){
        return this.isOneConditionPerPathRestricted;
    }
    
    public void setIsOneConditionPerPathRestricted(boolean param){
        this.isOneConditionPerPathRestricted = param;
    }
    
    public boolean getCompareCoveredUPNum() {
		return this.compareCoveredUPNum;
	}

    public void setCompareCoveredUPNum(boolean compareCoveredUPNum) {
		this.compareCoveredUPNum = compareCoveredUPNum;
	}
    
	public boolean getCompareOriginalInputRule() {
		return compareOriginalInputRule;
	}

	public void setCompareOriginalInputRule(boolean compareOriginalInputRule) {
		this.compareOriginalInputRule = compareOriginalInputRule;
	}
    
    public int[] getPolicySize(){
        return this.policySize;
    }
    
    public void setPolicySize(int[] param){
        this.policySize = param;
    }
    
    public int getNumPoliciesPerSize(){
        return this.numPoliciesPerSize;
    }
    
    public void setNumPoliciesPerSize(int param){
        this.numPoliciesPerSize = param;
    }
    
    public String getPolicyName(){
        return this.policyName;
    }
    
    public void setPolicyName(String param){
        this.policyName = param;
    }
    
    public String getAttributeDataPath(){
        return this.attributeDataPath;
    }
    
    public void setAttributeDataPath(String param){
        this.attributeDataPath = param;
    }
    
    public String getMinedRulesFromDTPath(){
        return this.minedRulesFromDTPath;
    }
    
    public void setMinedRulesFromDTPath(String path){
        this.minedRulesFromDTPath = path;
    }
    
    public String getOutputPath(){
        return this.outputPath;
    }
    
    public void setOutputPath(String out){
        this.outputPath = out;
    }
	
	public String getOutputFile(){
        return this.outputFile;
    }
    
    public void setOutputFile(String out){
        this.outputFile = out;
    }
    
    public boolean getOverassignmentDetection(){
        return this.overassignmentDetection;
    }
    
    public void setOverassignmentDetection(boolean b){
        this.overassignmentDetection = b;
    }
    
    public boolean getUnderassignmentDetection(){
        return this.underassignmentDetection;
    }
    
    public void setUnderassignmentDetection(boolean b){
        this.underassignmentDetection = b;
    }
    
    public Set<Triple<String, String, String>> getOverAssigmentsCurrentlyAllowed(){
        return this.overAssigmentsCurrentlyAllowed;
    }
    
    public void setOverAssigmentsCurrentlyAllowed(Set<Triple<String, String, String>> tupleSet){
        this.overAssigmentsCurrentlyAllowed = tupleSet;
    }
    
    public Set<Triple<String, String, String>> getRejectedUPFromLog(){
        return this.rejectedUPFromLog;
    }
    
    public void setRejectedUPFromLog(Set<Triple<String, String, String>> rejectedUPs){
        this.rejectedUPFromLog = rejectedUPs;
    }
    
    public double[] getTaus(){
        return this.taus;
    }
    
    public void setTaus(double[] t){
        this.taus = t;
    }
    
    public double[] getRuleUnderAssignmentWeights(){
        return this.ruleUnderAssignmentWeights;
    }
    
    public void setRuleUnderAssignmentWeights(double[] w){
        this.ruleUnderAssignmentWeights = w;
    }
    
    public double[] getPolicyUnderAssignmentWeights(){
        return this.policyUnderAssignmentWeights;
    }
    
    public void setPolicyUnderAssignmentWeights(double[] w){
        this.policyUnderAssignmentWeights = w;
    }
    
    public double[] getAlphas(){
        return this.alphas;
    }
    
    public void setAlphas(double[] a){
        this.alphas = a;
    }
    
    public String getLogDataFileInputPath(){
        return this.logDataFileInputPath;
    }
    
    public void setLogDataFileInputPath(String path){
        this.logDataFileInputPath = path;
    }
    
    public Set<Triple<String, String, String>> getRemovedTuplesFromLog(){
        return this.removedTuplesFromLog;
    }
    
    public void setRemovedTuplesFromLog(Set<Triple<String, String, String>> tuplesSet){
        this.removedTuplesFromLog = tuplesSet;
    }
    
    public Set<Triple<String, String, String>> getAdditionalTuplesFromMinedPolicies(){
        return this.additionalTuplesFromMinedPolicies;
    }
    
    public void setAdditionalTuplesFromMinedPolicies(Set<Triple<String, String, String>> tuplesSet){
        this.additionalTuplesFromMinedPolicies = tuplesSet;
    }
    
    public double[] getLogCompletenesses(){
        return this.logCompletenesses;
    }
    
    public void setLogCompletenesses(double[] lcs){
        this.logCompletenesses = lcs;
    }
    public double[] getNoiseLevels() {
    	return this.noiseLevels;
    }
    
    public void setNoiseLevels(double[] nls) {
    	this.noiseLevels = nls;
    }
    
    public String getNoiseDataFileInputPath() {
    	return this.noiseDataFileInputPath;
    }
    
    public void setNoiseDataFileInputPath(String noiseDataPath) {
    	this.noiseDataFileInputPath = noiseDataPath;
    }
    
    public boolean getBatchOptimization(){
        return this.batchOptimization;
    }
    
    public void setBatchOptimization(boolean b){
        this.batchOptimization = b;
    }
    
    public int getBatchSize(){
        return this.batchSize;
    }
    
    public void setBatchSize(int bs){
        this.batchSize = bs;
    }
    
    public Map<String, Map<String, FieldType>> getClassAttributes(){
        return this.classAtrributes;
    }
    
    public void setClassAttributes(Map<String, Map<String, FieldType>> sca){
        this.classAtrributes = sca;
    }
    
    public Map<List<AtomicCondition>, Map<String, Set<String>>> getConditionMeanings(){
        return this.conditionMeanings;
    }
    
    public void setConditionMeanings(Map<List<AtomicCondition>, Map<String, Set<String>>> cm){
        this.conditionMeanings = cm;
    }
    
    public Map<AtomicCondition, Map<String, Set<String>>> getAtomicConditionMeanings(){
        return this.atomicConditionMeanings;
    }
    
    public void setAtomicConditionMeanings(Map<AtomicCondition, Map<String, Set<String>>> acm){
        this.atomicConditionMeanings = acm;
    }
    
    public Map<List<AtomicConstraint>, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>> getConstraintMeanings(){
        return this.constraintMeanings;
    }
    
    public void setConstraintMeaning(Map<List<AtomicConstraint>, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>> csm){
        this.constraintMeanings = csm;
    }
    
    public Map<AtomicConstraint, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>> getAtomicConstraintMeanings(){
        return this.atomicConstraintMeanings;
    }
    
    public void setAtomicConstraintMeaning(Map<AtomicConstraint, Pair<Set<Pair<String, String>>, Set<Pair<String, String>>>> acsm){
        this.atomicConstraintMeanings = acsm;
    }
    //End Thang
}
