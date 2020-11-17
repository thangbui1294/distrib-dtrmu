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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Thang
 */
public class LearningDataGenerator {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Config config = new Config();
		if (args.length != 1){
			System.out.println("Missing config file argument");
			System.exit(0);
		}
        String configFile = args[0];
        readConfiguration(configFile, config);
        int[] sizes = config.getPolicySize();
        int policyNum = config.getNumPoliciesPerSize();
        String inputPath = config.getInputPath();
        
        for (int i = 0; i < sizes.length; i++){
            int size = sizes[i];
            long startCPUTime = Time.getCpuTime();
			int[] runPolicies = new int[policyNum];
			if (policyNum == 1){
				runPolicies[0] = config.getRunPolicy();
			}
			else{
				for (int j = 0; j < policyNum; j++){
					runPolicies[j] = j;
				}
			}
            for (int j:runPolicies) {
                String outputPath = config.getOutputPath() + "/" + config.getPolicyName()
                        + "_" + size + "/" + config.getPolicyName()
                        + "_" + size + "_" + j + "/";
                new File(outputPath).mkdirs();
                String policyName = config.getPolicyName()
                        + "_" + size + "_" + j;
                String inputFile = inputPath + "/" + config.getPolicyName()
                        + "_" + size + "_" + j + ".abac_txt";
                LearningDataGenerator.generateReBACData(inputFile, outputPath, policyName, configFile); 
            }
            System.out.println("ReBAC policy learning data files generation succeed for "
                    + config.getPolicyName() + "_" + size);
            
            long totalCPUTime = Time.getCpuTime() - startCPUTime;
            long totalCPUTimeMillis = (long) (totalCPUTime * 0.000001);
            long averageRunningTimeMillis = totalCPUTimeMillis/policyNum;
            String runningTime = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(averageRunningTimeMillis),
                    TimeUnit.MILLISECONDS.toSeconds(averageRunningTimeMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(averageRunningTimeMillis))
            );
            System.out.println("Average Running Time for " + config.getPolicyName() + "_" + size + ": " + runningTime);
			String timeOutputFile = config.getOutputTimePath() + "/output_" + config.getPolicyName()
                        + "_" + size + ".txt";
			try{
				FileWriter timeFileWrt = new FileWriter(timeOutputFile);
				BufferedWriter timeWriter = new BufferedWriter(timeFileWrt);
				timeWriter.write("Generating learning data step \n===============================\n");
				timeWriter.write("Average Running Time to generate learning data: " + runningTime + "\n\n");
				timeWriter.close();
				timeFileWrt.close();
				
			}
			catch (IOException e){
				System.out.println(e);
				System.exit(0);
			}
        }
        
    }
    public static void generateReBACData(String inputFileName, String outputPath, String policyName, String configFile){
        try{
            System.out.println("Generating data files for: "
                    + inputFileName);
            // csv file contains all objects
            String objectListFile = outputPath + policyName + "_objectList.csv";
            String acListFile = outputPath + policyName + "_acList.csv";
            
            Config config = new Config();
            readConfiguration(configFile, config);
            Parser.parseInputFile(inputFileName, config);
            System.out.println("Number of objects: " + config.getObjectModel().keySet().size());
            // set classIdValMap
            setClassIdValMap(config);
            
            // output the objectList file
            FileWriter olFileWrt = new FileWriter(objectListFile);
            BufferedWriter olWriter = new BufferedWriter(olFileWrt);
            olWriter.write("Ojbect_ID,Object_Type\n");
            for (String obj:config.getObjectModel().keySet()){
                olWriter.write(obj + "," + config.getObjectModel().get(obj).getClass1() + "\n");
            }
            olWriter.close();
            olFileWrt.close();
            
            // output the ac list file
            FileWriter aclFileWrt = new FileWriter(acListFile);
            BufferedWriter aclWriter = new BufferedWriter(aclFileWrt);
            aclWriter.write("Subject,Resource,Action\n");
            for (Triple<String, String, String> sp:config.getUPList()){
                aclWriter.write(sp.getFirst() + "," + sp.getSecond() + "," + sp.getThird() + "\n");
            }
            aclWriter.close();
            aclFileWrt.close();
            
            // output the attribute data file
            generateAttributeDataFile( outputPath, policyName, config);
        }
        catch (IOException e){
            System.out.println(e);
            System.exit(0);
        }
    }
    
    public static void generateAttributeDataFile(String outputPath, String policyName,  Config config){
        int maxSizeOfFile = 5000000;
        
        // we need to compute all possible atomic constraints for each pair of class types from sub-res pair
        Set<Pair<String, String>> subResPairs = generateSubResPair(config);
        Map<Pair<String, String>, Set<AtomicConstraint>> pairClassesConstraints = new HashMap<Pair<String, String>, Set<AtomicConstraint>>();
        for (Pair<String, String> subResPair:subResPairs){
            HashSet<AtomicConstraint> constraints = (HashSet<AtomicConstraint>) candidateConstraintByType(subResPair.getFirst(),
                    subResPair.getSecond(), config, config.getSubConstraintExtraDistance(), config.getResConstraintExtraDistance(),
                    config.getTotalConstraintLengthPathLimit());
            pairClassesConstraints.put(subResPair, constraints);
        }
        
        //compute all of possible atomic conditions for each class type and store them in classConditions map
        Map<String, ArrayList<ArrayList<String>>> classConditions = new HashMap<String, ArrayList<ArrayList<String>>>();
        int conditionPathLimit = 0;
        if (config.getSubConditionPathLimit() > config.getResConditionPathLimit()) {
            conditionPathLimit = config.getSubConditionPathLimit();
        } else {
            conditionPathLimit = config.getResConditionPathLimit();
        }
        for (String type:config.getClassModel().keySet()){
            ArrayList<ArrayList<String>> allPossiblePaths = getAllAttributePaths(type, config, conditionPathLimit);
            classConditions.put(type, allPossiblePaths);
        }
        
        // compute the map of all possible atomic conditions with its satisfied objects for each class.
        Map<String, Map<AtomicCondition, ArrayList<String>>> atomicConditionMeaningMap = new HashMap<String, Map<AtomicCondition, ArrayList<String>>>();
        Map<String, Map<AtomicCondition, ArrayList<String>>> atomicConditionUnknownMeaningMap = new HashMap<String, Map<AtomicCondition, ArrayList<String>>>();
        for (String type:classConditions.keySet()){
            if (config.getObjectList().get(type) != null){
                Map<AtomicCondition, ArrayList<String>> meaning = new HashMap<AtomicCondition, ArrayList<String>>();
                Map<AtomicCondition, ArrayList<String>> unknownMeaning = new HashMap<AtomicCondition, ArrayList<String>>();
                atomicConditionMeaningMap.put(type, meaning);
                atomicConditionUnknownMeaningMap.put(type, unknownMeaning);
                for (ArrayList<String> conditionPath:classConditions.get(type)){
                    // get all class types at the end of the paths
                    Triple<Boolean, String, Boolean> pathClass = getAttributePathClass(type, conditionPath, config.getClassModel(), config.getClassAttributes());
                    
                    if (pathClass.getSecond().equals("boolVal")){
                        // if the path is a boolean path, create new Atomic Condition object, and check if the sub satisfies it.
                        // true value
                        Set<String> newConstantTrue = new HashSet<String>();
                        newConstantTrue.add("true");
                        AtomicCondition newACTrue;
                        if (pathClass.getFirst()){
                            newACTrue = new AtomicCondition(conditionPath, newConstantTrue, ConditionOperator.IN);
                        }
                        else{
                            newACTrue = new AtomicCondition(conditionPath, newConstantTrue, ConditionOperator.CONTAINS);
                        }
                        // check all objects of the same type if they satisfy the condition
                        ArrayList<String> satisfiedObjects = new ArrayList<String>();
                        ArrayList<String> unknownValueObjects = new ArrayList<String>();
                        for (Object1 obj:config.getObjectList().get(type)){
                            Pair<Boolean, Boolean> checkCondition = checkSatisfyAtomicConditionWithUnknown(obj, newACTrue, config.getObjectModel(), config.getClassModel(), config);
                            if (checkCondition.getFirst()){
                                satisfiedObjects.add(obj.getId());
                            }
                            else if (checkCondition.getSecond()){
                                unknownValueObjects.add(obj.getId());
                            }
                        }
                        // put the satisfied object list to the map
                        atomicConditionMeaningMap.get(type).put(newACTrue, satisfiedObjects);
                        atomicConditionUnknownMeaningMap.get(type).put(newACTrue, unknownValueObjects);
                        
                        // false value
                        Set<String> newConstantFalse = new HashSet<String>();
                        newConstantFalse.add("false");
                        AtomicCondition newACFalse;
                        if (pathClass.getFirst()){
                            newACFalse = new AtomicCondition(conditionPath, newConstantFalse, ConditionOperator.IN);
                        }
                        else{
                            newACFalse = new AtomicCondition(conditionPath, newConstantFalse, ConditionOperator.CONTAINS);
                        }
                        // check all objects of the same type if they satisfy the condition
                        ArrayList<String> satisfiedObjects1 = new ArrayList<String>();
                        ArrayList<String> unknownValueObjects1 = new ArrayList<String>();
                        for (Object1 obj:config.getObjectList().get(type)){
                            Pair<Boolean, Boolean> checkCondition = checkSatisfyAtomicConditionWithUnknown(obj, newACFalse, config.getObjectModel(), config.getClassModel(), config);
                            if (checkCondition.getFirst()){
                                satisfiedObjects1.add(obj.getId());
                            }
                            else if (checkCondition.getSecond()){
                                unknownValueObjects1.add(obj.getId());
                            }
                        }
                        // put the satisfied object list to the map
                        atomicConditionMeaningMap.get(type).put(newACFalse, satisfiedObjects1);
                        atomicConditionUnknownMeaningMap.get(type).put(newACFalse, unknownValueObjects1);
                    }
                    else{
                        // case that the path is not boolean value, compute all possible values for the path,
                        // create 1 Atomic Condition object for each of the value, and check if the sub satisfies it.
                        
                        // first compute all possible values:
                        Set<String> possibleValues = config.getClassIdValMap().get(pathClass.getSecond());
                        for (String value:possibleValues){
                            Set<String> newConstant = new HashSet<String>();
                            newConstant.add(value);
                            ConditionOperator newOp;
                            if (pathClass.getFirst()){
                                newOp = ConditionOperator.IN;
                            }
                            else{
                                newOp = ConditionOperator.CONTAINS;
                            }
                            
                            AtomicCondition newAC = new AtomicCondition(conditionPath, newConstant, newOp);
                            // check all objects of the same type if they satisfy the condition
                            ArrayList<String> satisfiedObjects = new ArrayList<String>();
                            ArrayList<String> unknownValueObjects = new ArrayList<String>();
                            for (Object1 obj:config.getObjectList().get(type)){
                                Pair<Boolean, Boolean> checkCondition = checkSatisfyAtomicConditionWithUnknown(obj, newAC, config.getObjectModel(), config.getClassModel(), config);
                                if (checkCondition.getFirst()){
                                    satisfiedObjects.add(obj.getId());
                                }
                                else if (checkCondition.getSecond()){
                                    unknownValueObjects.add(obj.getId());
                                }
                            }
                            // put the satisfied object list to the map
                            atomicConditionMeaningMap.get(type).put(newAC, satisfiedObjects);
                            atomicConditionUnknownMeaningMap.get(type).put(newAC, unknownValueObjects);
                        }
                    }
                }
            }
        }
        
        
        try{
            int countEntry = 0;
            for (Pair<String, String> subResPair:subResPairs){
                // iterate all possible pairs of objects from subResPairs types to generate each data row.
                int currentFileOrder = 0;
                String attributeListFile = outputPath + policyName + "_attributeList_" + subResPair.getFirst() + "-" + subResPair.getSecond() + "_" + currentFileOrder + ".csv";
                FileWriter attrFileWrt = new FileWriter(attributeListFile);
                BufferedWriter attrWriter = new BufferedWriter(attrFileWrt);
                ArrayList<Object1> subObjects = config.getObjectList().get(subResPair.getFirst());
                ArrayList<Object1> resObjects = config.getObjectList().get(subResPair.getSecond());
                
                for (int i = 0; i < subObjects.size(); i++){
                    Object1 sub = subObjects.get(i);
                    for (int j = 0; j < resObjects.size(); j++){
                        Object1 res = resObjects.get(j);
                        attrWriter.write(sub.getId() + "," + res.getId());
                        countEntry+=2;
                        Pair<String, String> pair = new Pair(sub.getClass1(), res.getClass1());
                        // output constraint attributes
                        for (AtomicConstraint as: pairClassesConstraints.get(pair)){
                            attrWriter.write("," + as + ":");
                            Pair<Boolean, Boolean> checkValid = LearningDataGenerator.checkSatisfyConstraintWithUnknown(sub, res, as, config);
                            if (checkValid.getFirst()){
                                attrWriter.write("1");
                            }
                            else if (checkValid.getSecond()){
                                attrWriter.write("2");
                            }
                            else{
                                attrWriter.write("0");
                            }
                            countEntry++;
                        }
                        // output all possible subject conditions
                        for (ArrayList<String> conditionPath:classConditions.get(sub.getClass1())){
                            // get all class types at the end of the paths
                            Triple<Boolean, String, Boolean> pathClass = getAttributePathClass(sub.getClass1(), conditionPath, config.getClassModel(), config.getClassAttributes());
                            
                            if (pathClass.getSecond().equals("boolVal")){
                                // if the path is a boolean path, create new Atomic Condition object, and check if the sub satisfies it.
                                // true value
                                Set<String> newConstantTrue = new HashSet<String>();
                                newConstantTrue.add("true");
                                AtomicCondition newACTrue;
                                if (pathClass.getFirst()){
                                    newACTrue = new AtomicCondition(conditionPath, newConstantTrue, ConditionOperator.IN);
                                }
                                else{
                                    newACTrue = new AtomicCondition(conditionPath, newConstantTrue, ConditionOperator.CONTAINS);
                                }
                                if (pathClass.getThird()){
                                    attrWriter.write(",OPTIONALsub" + newACTrue + ":");
                                }
                                else{
                                    attrWriter.write(",sub" + newACTrue + ":");
                                }
                                if (atomicConditionMeaningMap.get(sub.getClass1()).get(newACTrue).contains(sub.getId())){
                                    attrWriter.write("1");
                                }
                                else if (atomicConditionUnknownMeaningMap.get(sub.getClass1()).get(newACTrue).contains(sub.getId())){
                                    attrWriter.write("2");
                                }
                                else{
                                    attrWriter.write("0");
                                }
                                
                                // false value
                                Set<String> newConstantFalse = new HashSet<String>();
                                newConstantFalse.add("false");
                                AtomicCondition newACFalse;
                                if (pathClass.getFirst()){
                                    newACFalse = new AtomicCondition(conditionPath, newConstantFalse, ConditionOperator.IN);
                                }
                                else{
                                    newACFalse = new AtomicCondition(conditionPath, newConstantFalse, ConditionOperator.CONTAINS);
                                }
                                if (pathClass.getThird()){
                                    attrWriter.write(",OPTIONALsub" + newACFalse + ":");
                                }
                                else {
                                    attrWriter.write(",sub" + newACFalse + ":");
                                }
                                if (atomicConditionMeaningMap.get(sub.getClass1()).get(newACFalse).contains(sub.getId())){
                                    attrWriter.write("1");
                                }
                                else if (atomicConditionUnknownMeaningMap.get(sub.getClass1()).get(newACFalse).contains(sub.getId())){
                                    attrWriter.write("2");
                                }
                                else{
                                    attrWriter.write("0");
                                }
                                countEntry++;
                            }
                            else{
                                // case that the path is not boolean value, compute all possible values for the path, 
                                // create 1 Atomic Condition object for each of the value, and check if the sub satisfies it.
                                
                                // first compute all possible values:
                                Set<String> possibleValues = config.getClassIdValMap().get(pathClass.getSecond());
                                for (String value:possibleValues){
                                    Set<String> newConstant = new HashSet<String>();
                                    newConstant.add(value);
                                    ConditionOperator newOp = null;
                                    if (pathClass.getFirst()){
                                        newOp = ConditionOperator.IN;
                                    }
                                    else{
                                        newOp = ConditionOperator.CONTAINS;
                                    }
                                    
                                    AtomicCondition newAC = new AtomicCondition(conditionPath, newConstant, newOp);
                                    if (pathClass.getThird()){
                                        attrWriter.write(",OPTIONALsub" + newAC + ":");
                                    }
                                    else{
                                        attrWriter.write(",sub" + newAC + ":");
                                    }
                                    if (atomicConditionMeaningMap.get(sub.getClass1()).get(newAC).contains(sub.getId())){
                                        attrWriter.write("1");
                                    }
                                    else if (atomicConditionUnknownMeaningMap.get(sub.getClass1()).get(newAC).contains(sub.getId())){
                                        attrWriter.write("2");
                                    }
                                    else{
                                        attrWriter.write("0");
                                    }
                                    countEntry++;
                                }
                            }
                        }
                        
                        // output all possible resource conditions
                        for (ArrayList<String> conditionPath:classConditions.get(res.getClass1())){
                            // get all class types at the end of the paths
                            Triple<Boolean, String, Boolean> pathClass = getAttributePathClass(res.getClass1(), conditionPath, config.getClassModel(), config.getClassAttributes());
                            
                            if (pathClass.getSecond().equals("boolVal")){
                                // if the path is a boolean path, create new Atomic Condition object, and check if the res satisfies it.
                                // true value
                                Set<String> newConstantTrue = new HashSet<String>();
                                newConstantTrue.add("true");
                                AtomicCondition newACTrue;
                                if (pathClass.getFirst()){
                                    newACTrue = new AtomicCondition(conditionPath, newConstantTrue, ConditionOperator.IN);
                                }
                                else{
                                    newACTrue = new AtomicCondition(conditionPath, newConstantTrue, ConditionOperator.CONTAINS);
                                }
                                if (pathClass.getThird()){
                                    attrWriter.write(",OPTIONALres" + newACTrue + ":");
                                }
                                else{
                                    attrWriter.write(",res" + newACTrue + ":");
                                }
                                if (atomicConditionMeaningMap.get(res.getClass1()).get(newACTrue).contains(res.getId())){
                                    attrWriter.write("1");
                                }
                                else if (atomicConditionUnknownMeaningMap.get(res.getClass1()).get(newACTrue).contains(res.getId())){
                                    attrWriter.write("2");
                                }
                                else{
                                    attrWriter.write("0");
                                }
                                
                                // false value
                                Set<String> newConstantFalse = new HashSet<String>();
                                newConstantFalse.add("false");
                                AtomicCondition newACFalse;
                                if (pathClass.getFirst()){
                                    newACFalse = new AtomicCondition(conditionPath, newConstantFalse, ConditionOperator.IN);
                                }
                                else{
                                    newACFalse = new AtomicCondition(conditionPath, newConstantFalse, ConditionOperator.CONTAINS);
                                }
                                if (pathClass.getThird()){
                                    attrWriter.write(",OPTIONALres" + newACFalse + ":");
                                }
                                else{
                                    attrWriter.write(",res" + newACFalse + ":");
                                }
                                if (atomicConditionMeaningMap.get(res.getClass1()).get(newACFalse).contains(res.getId())){
                                    attrWriter.write("1");
                                }
                                else if (atomicConditionUnknownMeaningMap.get(res.getClass1()).get(newACFalse).contains(res.getId())){
                                    attrWriter.write("2");
                                }
                                else{
                                    attrWriter.write("0");
                                }
                                countEntry++;
                            }
                            else{
                                // case that the path is not boolean value, compute all possible values for the path,
                                // create 1 Atomic Condition object for each of the value, and check if the res satisfies it.
                                
                                // first compute all possible values:
                                Set<String> possibleValues = config.getClassIdValMap().get(pathClass.getSecond());
                                
                                // testing
                                for (String value:possibleValues){
                                    Set<String> newConstant = new HashSet<String>();
                                    newConstant.add(value);
                                    ConditionOperator newOp;
                                    if (pathClass.getFirst()){
                                        newOp = ConditionOperator.IN;
                                    }
                                    else{
                                        newOp = ConditionOperator.CONTAINS;
                                    }
                                    
                                    AtomicCondition newAC = new AtomicCondition(conditionPath, newConstant, newOp);
                                    if (pathClass.getThird()){
                                        attrWriter.write(",OPTIONALres" + newAC + ":");
                                    }
                                    else{
                                        attrWriter.write(",res" + newAC + ":");
                                    }
                                    if (atomicConditionMeaningMap.get(res.getClass1()).get(newAC).contains(res.getId())){
                                        attrWriter.write("1");
                                    }
                                    else if (atomicConditionUnknownMeaningMap.get(res.getClass1()).get(newAC).contains(res.getId())){
                                        attrWriter.write("2");
                                    }
                                    else{
                                        attrWriter.write("0");
                                    }
                                    
                                    countEntry++;
                                }
                            }
                        }
                        attrWriter.write("\n");
                    }
                    if (countEntry > maxSizeOfFile && i != subObjects.size() - 1){
                        // split to another file
                        countEntry = 0;
                        currentFileOrder++;
                        String newAttributeListFile = outputPath + policyName + "_attributeList_" + subResPair.getFirst() + "-" + subResPair.getSecond() + "_" + currentFileOrder + ".csv";;
                        attrFileWrt.flush();
                        attrWriter.flush();
                        attrFileWrt = new FileWriter(newAttributeListFile);
                        attrWriter = new BufferedWriter(attrFileWrt);   
                    }
                    else if (i == subObjects.size() -1){
                        attrFileWrt.flush();
                        attrWriter.flush();
                    }
                }
                attrWriter.close();
                attrFileWrt.close();
            }
        }
        catch (IOException e){
            System.out.println(e);
            System.exit(0);
        }
    }
    
    
    private static void setClassIdValMap(Config config) {
        Map<String, HashSet<String>> classIdValMap = new HashMap<>();
        Map<String, Class1> classModel = config.getClassModel();
        Map<String, ArrayList<Object1>> classObjList = config.getObjectList();
        
        // initialization
        for (String className : classModel.keySet()) {
            if (!classIdValMap.containsKey(className)) {
                classIdValMap.put(className, new HashSet<>());
            }
        }
        
        // generate class id value map
        for (String className : classObjList.keySet()) {
            ArrayList<Object1> objList = classObjList.get(className);
            HashSet<String> idSet = new HashSet<>();
            
            for (Object1 obj : objList) {
                idSet.add(obj.getId());
            }
            
            HashSet<String> existingIdSet = classIdValMap.get(className);
            idSet.addAll(existingIdSet);
            
            Class1 currentClass = classModel.get(className);
            
            HashSet<String> setToAdd = null;
            while (currentClass != null) {
                if (classIdValMap.containsKey(currentClass.getClassName())) {
                    setToAdd = classIdValMap.get(currentClass.getClassName());
                    setToAdd.addAll(idSet);
                } else {
                    setToAdd = idSet;
                }
                classIdValMap.put(currentClass.getClassName(), setToAdd);
                currentClass = currentClass.getParentClass();
                setToAdd = null;
            }
        }
        
        config.setClassIdValMap(classIdValMap);
    }
    
    public static boolean checkSatisfyAtomicCondition(Object1 obj, AtomicCondition ac, Map<String, Object1> objects, Map<String, Class1> classes, Config config){

        Triple<Boolean, ArrayList<String>, Boolean> attrs = Parser.getAttributePathValues(obj, ac.getPath(), objects, classes, config);
        if (attrs.getThird()){
            // there is Unknown values
            if (ac.getConditionOperator() == ConditionOperator.CONTAINS && attrs.getSecond().containsAll(ac.getConstant())){
                return true;
            }
            return false;
        }
        if (attrs.getSecond().isEmpty()){
            if (ac.getIsNegative()){
                return true;
            }
            return false;
        }
        if (attrs.getFirst()){
            // boolean field
            if (ac.getConditionOperator() == ConditionOperator.IN && !ac.getConstant().contains(attrs.getSecond().get(0))){
                if (ac.getIsNegative()){
                    return true;
                }
                return false;
            }
            else if (ac.getConditionOperator() == ConditionOperator.CONTAINS){
                if (!attrs.getSecond().contains((String)ac.getConstant().toArray()[0])){
                    if (ac.getIsNegative()){
                        return true;
                    }
                    return false;
                }
            }
        }
        else{
            if (ac.getConditionOperator() == ConditionOperator.IN && !ac.getConstant().contains(attrs.getSecond().get(0))){
                if (ac.getIsNegative()){
                    return true;
                }
                return false;
            }
            else if (ac.getConditionOperator() == ConditionOperator.CONTAINS && !attrs.getSecond().containsAll(ac.getConstant())){
                if (ac.getIsNegative()){
                    return true;
                }
                return false;
            }
        }
        if (ac.getIsNegative()){
            return false;
        }
        return true;
    }
    
    public static Pair<Boolean, Boolean> checkSatisfyAtomicConditionWithUnknown(Object1 obj, AtomicCondition ac, Map<String, Object1> objects, Map<String, Class1> classes, Config config){
        // the first return boolean specifies if obj satisfies ac, the second return boolean sepcifies if the evaluated value is Unknown.
        // note that the first one can be True if when the second one is False.
        Triple<Boolean, ArrayList<String>, Boolean> attrs = Parser.getAttributePathValues(obj, ac.getPath(), objects, classes, config);
        if (attrs.getThird()){
            // there is Unknown values
            if (ac.getConditionOperator() == ConditionOperator.CONTAINS && attrs.getSecond().containsAll(ac.getConstant())){
                return new Pair(true, true);
            }
            return new Pair(false, true);
        }
        if (attrs.getSecond().isEmpty()){
            if (ac.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        if (attrs.getFirst()){
            // boolean field
            if (ac.getConditionOperator() == ConditionOperator.IN && !ac.getConstant().contains(attrs.getSecond().get(0))){
                if (ac.getIsNegative()){
                    return new Pair(true, false);
                }
                return new Pair(false, false);
            }
            else if (ac.getConditionOperator() == ConditionOperator.CONTAINS){
                if (!attrs.getSecond().contains((String)ac.getConstant().toArray()[0])){
                    if (ac.getIsNegative()){
                        return new Pair(true, false);
                    }
                    return new Pair(false, false);
                }
            }
        }
        else{
            if (ac.getConditionOperator() == ConditionOperator.IN && !ac.getConstant().contains(attrs.getSecond().get(0))){
                if (ac.getIsNegative()){
                    return new Pair(true, false);
                }
                return new Pair(false, false);
            }
            else if (ac.getConditionOperator() == ConditionOperator.CONTAINS && !attrs.getSecond().containsAll(ac.getConstant())){
                if (ac.getIsNegative()){
                    return new Pair(true, false);
                }
                return new Pair(false, false);
            }
        }
        if (ac.getIsNegative()){
            return new Pair(false, false);
        }
        return new Pair(true, false);
    }
    
    private static Triple<Boolean, String, Boolean> getAttributePathClass(String className, ArrayList<String> path, Map<String, Class1> classes, Map<String, Map<String, FieldType>> classAttributes){
        boolean isSinglePath = true;
        boolean isOptionalPath = false;
        String currentClassName = className;
        for (String s:path){
            if (s.equals("id")){
                return new Triple(isSinglePath, currentClassName, isOptionalPath);
            }
            else if (classAttributes.get(currentClassName).get(s).getIsBoolean()){
                return new Triple(isSinglePath, "boolVal", isOptionalPath);
            }
            else {
                if (classAttributes.get(currentClassName).get(s).getMultiplicity() == FieldType.Multiplicity.MANY){
                    isSinglePath = false;
                }
                if (classAttributes.get(currentClassName).get(s).getMultiplicity() == FieldType.Multiplicity.OPTIONAL){
                    isOptionalPath = true;
                }
                currentClassName = classAttributes.get(currentClassName).get(s).getType().getClassName();
            }
        }
        return new Triple(isSinglePath, currentClassName, isOptionalPath);
    }
    
    
    
    /**
     * This method check if 2 objects (subject and object) satisfy a rule constraint
     * @param subObj subject object
     * @param resObj resource object
     * @param cc atomic constraint to check with
     * @param config
     * @return true if subject and resource satisfy the constraint, false otherwise.
     */
    public static boolean checkSatisfyConstraint(Object1 subObj, Object1 resObj, AtomicConstraint cc, Config config){
        Map<String, Object1> objects = config.getObjectModel();
        Map<String, Class1> classes = config.getClassModel();
        Triple<Boolean, ArrayList<String>, Boolean> subAttrs = Parser.getAttributePathValues(subObj, cc.getSubPath(), objects, classes, config);
        Triple<Boolean, ArrayList<String>, Boolean> resAttrs = Parser.getAttributePathValues(resObj, cc.getResPath(), objects, classes, config);
        
        if (subAttrs.getThird() || resAttrs.getThird()){
            // case that one of the subject path objects or resource path objects has Unknown value(s)
            if (cc.getConstraintOperator() == ConstraintOperator.CONTAINS && !resAttrs.getSecond().isEmpty() && subAttrs.getSecond().contains(resAttrs.getSecond().get(0))){
                return true;
            }
            else if (cc.getConstraintOperator() == ConstraintOperator.IN && !subAttrs.getSecond().isEmpty() && resAttrs.getSecond().contains(subAttrs.getSecond().get(0))){
                return true;
            }
            return false;
        }
        // Note that for now, our algorithm does not attempt to infer constraints on booleans
        if (subAttrs.getSecond().isEmpty() || resAttrs.getSecond().isEmpty()){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        if (cc.getConstraintOperator() == ConstraintOperator.CONTAINS && !subAttrs.getSecond().contains(resAttrs.getSecond().get(0))){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.IN && !resAttrs.getSecond().contains(subAttrs.getSecond().get(0))){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.EQUALS_VALUE && !subAttrs.getSecond().get(0).equals(resAttrs.getSecond().get(0))){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.SUPSETEQ && !subAttrs.getSecond().containsAll(resAttrs.getSecond())){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.SUBSETEQ && !resAttrs.getSecond().containsAll(subAttrs.getSecond())){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.EQUALS_SET && !(subAttrs.getSecond().containsAll(resAttrs.getSecond())
                && resAttrs.getSecond().containsAll(subAttrs.getSecond()))){
            if (cc.getIsNegative()){
                return true;
            }
            return false;
        }
        
        if (cc.getIsNegative()){
                return false;
            }
        return true;
    }
    
    
    public static Pair<Boolean, Boolean> checkSatisfyConstraintWithUnknown(Object1 subObj, Object1 resObj, AtomicConstraint cc, Config config){
        // the first return boolean specifies if subObj and resObj satisfies cc, the second return boolean sepcifies if the evaluated value is Unknown.
        // note that the first one can be True if when the second one is False.
        Map<String, Object1> objects = config.getObjectModel();
        Map<String, Class1> classes = config.getClassModel();
        Triple<Boolean, ArrayList<String>, Boolean> subAttrs = Parser.getAttributePathValues(subObj, cc.getSubPath(), objects, classes, config);
        Triple<Boolean, ArrayList<String>, Boolean> resAttrs = Parser.getAttributePathValues(resObj, cc.getResPath(), objects, classes, config);
        
        if (subAttrs.getThird() || resAttrs.getThird()){
            // case that one of the subject path objects or resource path objects has Unknown value(s)
            if (cc.getConstraintOperator() == ConstraintOperator.CONTAINS && !resAttrs.getSecond().isEmpty() && subAttrs.getSecond().contains(resAttrs.getSecond().get(0))){
                return new Pair(true, true);
            }
            else if (cc.getConstraintOperator() == ConstraintOperator.IN && !subAttrs.getSecond().isEmpty() && resAttrs.getSecond().contains(subAttrs.getSecond().get(0))){
                return new Pair(true, true);
            }
            return new Pair(false, true);
        }
        // Note that for now, our algorithm does not attempt to infer constraints on booleans
        if (subAttrs.getSecond().isEmpty() || resAttrs.getSecond().isEmpty()){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        if (cc.getConstraintOperator() == ConstraintOperator.CONTAINS && !subAttrs.getSecond().contains(resAttrs.getSecond().get(0))){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.IN && !resAttrs.getSecond().contains(subAttrs.getSecond().get(0))){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.EQUALS_VALUE && !subAttrs.getSecond().get(0).equals(resAttrs.getSecond().get(0))){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.SUPSETEQ && !subAttrs.getSecond().containsAll(resAttrs.getSecond())){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.SUBSETEQ && !resAttrs.getSecond().containsAll(subAttrs.getSecond())){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        else if (cc.getConstraintOperator() == ConstraintOperator.EQUALS_SET && !(subAttrs.getSecond().containsAll(resAttrs.getSecond())
                && resAttrs.getSecond().containsAll(subAttrs.getSecond()))){
            if (cc.getIsNegative()){
                return new Pair(true, false);
            }
            return new Pair(false, false);
        }
        
        if (cc.getIsNegative()){
                return new Pair(false, false);
            }
        return new Pair(true, false);
    }
    
    private static Set<AtomicConstraint> candidateConstraintByType(String Tsub, String Tres,
            Config config, int subExtraDist, int resExtraDist, int maxTotalPathLength){
        // First find the shortest path to reachable types for sub and
        Set<Triple<String, Integer, ArrayList<String>>> subPaths;
        Set<Triple<String, Integer, ArrayList<String>>> resPaths;
        Set<AtomicConstraint> results = new HashSet<AtomicConstraint>();
        
        Set<Triple<String, Integer, ArrayList<String>>> shortestSubPaths = getAllShortestPaths(Tsub,config);
        Set<Triple<String, Integer, ArrayList<String>>> shortestResPaths = getAllShortestPaths(Tres,config);
        if (subExtraDist == 0){
            subPaths = shortestSubPaths;
        }
        else{
            Map<String, Integer> subDistances = new HashMap<String, Integer>();
            for (Triple<String, Integer, ArrayList<String>> e:shortestSubPaths){
                if ((subDistances.get(e.getFirst()) != null && e.getSecond() > subDistances.get(e.getFirst())) || !subDistances.containsKey(e.getFirst())){
                    subDistances.put(e.getFirst(), e.getSecond() - 1); // not included id attribute
                }
            }
            subPaths = getAllPaths(Tsub, config, subDistances, subExtraDist);
        }
        
        if (resExtraDist == 0){
            resPaths = shortestResPaths;
        }
        else{
            Map<String, Integer> resDistances = new HashMap<String, Integer>();
            
            for (Triple<String, Integer, ArrayList<String>> e:shortestResPaths){
                if ((resDistances.get(e.getFirst()) != null && e.getSecond() > resDistances.get(e.getFirst())) || !resDistances.containsKey(e.getFirst())){
                    resDistances.put(e.getFirst(), e.getSecond() - 1); // not included id attribute
                }
            }
            resPaths = getAllPaths(Tres, config, resDistances, resExtraDist);
        }
        for (Triple<String, Integer, ArrayList<String>> path1:subPaths){
            for (Triple<String, Integer, ArrayList<String>> path2:resPaths){
                // get parent class of each path's object if any
                ArrayList<String> parentsSub = new ArrayList<String>();
                ArrayList<String> parentsRes = new ArrayList<String>();
                String currentSubClass = path1.getFirst();
                while (config.getClassModel().get(currentSubClass).getParentClass() != null){
                    String newParent = config.getClassModel().get(currentSubClass).getParentClass().getClassName();
                    parentsSub.add(newParent);
                    currentSubClass = newParent;
                }
                String currentResClass = path2.getFirst();
                while (config.getClassModel().get(currentResClass).getParentClass() != null){
                    String newParent = config.getClassModel().get(currentResClass).getParentClass().getClassName();
                    parentsRes.add(newParent);
                    currentResClass = newParent;
                }
                if (path1.getFirst().equals(path2.getFirst())){
                    ArrayList<ConstraintOperator> ops = operatorFromPath(Tsub, path1.getThird(), Tres, path2.getThird(), config);
                    for (ConstraintOperator co:ops){
                        results.add(new AtomicConstraint(path1.getThird(), path2.getThird(),co));
                    }
                }
                // check parent classes
                else if (!parentsSub.isEmpty() && !parentsRes.isEmpty()){
                    for (int i = 0; i < parentsSub.size(); i++){
                        String parentSub = parentsSub.get(i);
                        for (int j = 0; j < parentsRes.size(); j++){
                            String parentRes = parentsRes.get(j);
                            if (parentSub.equals(parentRes) && checkSatisfyConstraint(path1.getFirst(), path2.getFirst(), config.getClassIdValMap())){
                                ArrayList<ConstraintOperator> ops = operatorFromPath(Tsub, path1.getThird(), Tres, path2.getThird(), config);
                                for (ConstraintOperator co:ops){
                                    results.add(new AtomicConstraint(path1.getThird(), path2.getThird(),co));
                                }
                            }
                        }
                    }
                    
                }
                else if (!parentsSub.isEmpty()){
                    for (String parentSub:parentsSub){
                        if (parentSub.equals(path2.getFirst())){
                            ArrayList<ConstraintOperator> ops = operatorFromPath(Tsub, path1.getThird(), Tres, path2.getThird(), config);
                            for (ConstraintOperator co:ops){
                                results.add(new AtomicConstraint(path1.getThird(), path2.getThird(),co));
                            }
                        }
                    }
                }
                else if (!parentsRes.isEmpty()){
                    for (String parentRes:parentsRes){
                        if (path1.getFirst().equals(parentRes)){
                            ArrayList<ConstraintOperator> ops = operatorFromPath(Tsub, path1.getThird(), Tres, path2.getThird(), config);
                            for (ConstraintOperator co:ops){
                                results.add(new AtomicConstraint(path1.getThird(), path2.getThird(),co));
                            }
                        }
                    }
                }
            }
        }
        Iterator<AtomicConstraint> it = results.iterator();
        while(it.hasNext()) {
            AtomicConstraint c = it.next();
            if ((c.getSubPath().size() + c.getResPath().size()) > maxTotalPathLength) {
                it.remove();
            }
        }
        return results;
    }
    
    private static Set<Triple<String, Integer, ArrayList<String>>> getAllShortestPaths(String type, Config config){
        Map<String, Class1> classModel = config.getClassModel();
        Set<Triple<String, Integer, ArrayList<String>>> results = new HashSet<Triple<String, Integer, ArrayList<String>>>();
        Map<String, Triple<String, Integer, ArrayList<String>>> graphs = new HashMap<String, Triple<String, Integer, ArrayList<String>>>();
        Queue<Triple<String, Integer, ArrayList<String>>> q = new LinkedList<Triple<String, Integer, ArrayList<String>>>();
        Triple<String, Integer, ArrayList<String>> root = new Triple(type, 0, new ArrayList<String>());
        for (Map.Entry<String, Class1> entry:classModel.entrySet()){
            if (entry.getKey().equals(root.getFirst())){
                q.add(root);
                graphs.put(type, root);
            }
            else {
                graphs.put(entry.getKey(), new Triple(entry.getKey(), 1000, new ArrayList<String>()));
            }
        }
        while (!q.isEmpty()){
            Triple<String, Integer, ArrayList<String>> current = q.remove();
            for (Pair<String, String> neighbor:config.getAdjacencyList().get(current.getFirst())){
                String neighborType = neighbor.getSecond();
                Triple<String, Integer, ArrayList<String>> neighborNode = graphs.get(neighborType);
                //self loop
                if (neighborNode.getSecond() == 1000){
                    neighborNode.setSecond(current.getSecond() + 1);
                    if (!current.getThird().isEmpty()){
                        neighborNode.getThird().addAll(current.getThird());
                    }
                    neighborNode.getThird().add(neighbor.getFirst());
                    q.add(neighborNode);
                    results.add(neighborNode);
                }
                else if (neighborNode.getSecond() == current.getSecond() + 1){
                    // the condition used to keep all shortest path, not just the first one found.
                    ArrayList<String> path = new ArrayList<String>(current.getThird());
                    path.add(neighbor.getFirst());
                    q.add(new Triple(neighborNode.getFirst(), current.getSecond() + 1, path));
                    results.add(new Triple(neighborNode.getFirst(), current.getSecond() + 1, path));
                }
                else if (neighborNode.getFirst().equals(current.getFirst())){
                    // the second condition used handle self loop. We limit the level of self loop to 1 by adding new path with this self loop and do not add this node again to the queue.
                    ArrayList<String> path = new ArrayList<String>(current.getThird());
                    path.add(neighbor.getFirst());
                    results.add(new Triple(neighborNode.getFirst(), current.getSecond(), path));
                }
            }
        }
        
        // add id to all of paths in the result
        for (Triple<String, Integer, ArrayList<String>> path:results){
            path.getThird().add("id");
            path.setSecond(path.getSecond() + 1);
        }
        // Add path with id only
        ArrayList<String> id = new ArrayList<String>();
        id.add("id");
        results.add(new Triple(root.getFirst(), 1, id));
        return results;
    }
    
    private static Set<Triple<String, Integer, ArrayList<String>>> getAllPaths(String type, Config config, Map<String, Integer> shortestDistances, int extraDist){
        Set<Triple<String, Integer, ArrayList<String>>> results = new HashSet<Triple<String, Integer, ArrayList<String>>>();
        Queue<Pair<String, ArrayList<String>>> q = new LinkedList<Pair<String, ArrayList<String>>>();
        Pair<String, ArrayList<String>> root = new Pair(type, new ArrayList<String>());
        q.add(root);
        // maxLength is the longest path that a type can have
        int maxLength = Collections.max(shortestDistances.values());
        maxLength = maxLength  + extraDist;
        while (!q.isEmpty()){
            Pair<String, ArrayList<String>> current = q.remove();
            for (Pair<String, String> neighbor:config.getAdjacencyList().get(current.getFirst())){
                ArrayList<String> newPath = new ArrayList<String>();
                if (!current.getSecond().isEmpty()){
                    newPath.addAll(current.getSecond());
                }
                newPath.add(neighbor.getFirst());
                if (current.getSecond().size() + 1 <= shortestDistances.get(neighbor.getSecond()) + extraDist){
                    results.add(new Triple(neighbor.getSecond(), newPath.size(), newPath));
                }
                if (current.getSecond().size() + 1 < maxLength){
                    q.add(new Pair(neighbor.getSecond(), newPath));
                }
            }
        }
        
        // add id to all of paths in the result
        for (Triple<String, Integer, ArrayList<String>> path:results){
            path.getThird().add("id");
            path.setSecond(path.getSecond() + 1);
        }
        // Add path with id only
        ArrayList<String> id = new ArrayList<String>();
        id.add("id");
        results.add(new Triple(root.getFirst(), 1, id));
        return results;
    }
    
    private static boolean checkSatisfyConstraint(String class1, String class2,
            Map<String, HashSet<String>> classIdValMap) {
        HashSet<String> class1Id = new HashSet<>(classIdValMap.get(class1));
        HashSet<String> class2Id = new HashSet<>(classIdValMap.get(class2));
        return !class1Id.retainAll(class2Id);
    }
    
    private static ArrayList<ConstraintOperator> operatorFromPath(String subType, ArrayList<String>subPath, String resType, ArrayList<String> resPath, Config config){
        boolean isSubPathMany = isManyMultiplicity(subType, subPath, config);
        boolean isResPathMany = isManyMultiplicity(resType, resPath, config);
        ArrayList<ConstraintOperator> result = new ArrayList<ConstraintOperator>();
        if (isSubPathMany && isResPathMany){
            result.add(ConstraintOperator.SUPSETEQ);
            result.add(ConstraintOperator.SUBSETEQ);
            result.add(ConstraintOperator.EQUALS_SET);
        }
        else if (!isSubPathMany && isResPathMany){
            result.add(ConstraintOperator.IN);
        }
        else if (isSubPathMany && !isResPathMany){
            result.add(ConstraintOperator.CONTAINS);
        }
        else result.add(ConstraintOperator.EQUALS_VALUE);
        return result;
    }
    
    private static Boolean isManyMultiplicity(String type, ArrayList<String> path, Config config){
        Map<String, Class1> classes = config.getClassModel();
        String currentClassName  = type;
        for (int i = 0; i < path.size(); i++){
            String attr = path.get(i);
            if (attr.equals("id")){
                continue;
            }
            if (config.getClassAttributes().get(currentClassName).get(attr).getIsBoolean()){
                continue;
            }
            if (config.getClassAttributes().get(currentClassName).get(attr).getMultiplicity() == FieldType.Multiplicity.MANY){
                return true;
            }
            currentClassName = config.getClassAttributes().get(currentClassName).get(attr).getType().getClassName();
        }
        return false;
    }
    
    private static Set<Pair<String, String>> generateSubResPair(Config config) {
        ArrayList<Triple<String, String, String>> tripleList = config.getUPList();
        Set<Pair<String, String>> subResPair = new HashSet<>();
        
        Map<String, Object1> objectModel = config.getObjectModel();
        Map<String, Class1> classModel = config.getClassModel();
        for (Triple<String, String, String> triple : tripleList) {
            // get subject and resource ids
            String subObjId = triple.getFirst();
            String resObjId = triple.getSecond();
            
            // get subject and resource class names
            Class1 subClass = classModel.get(objectModel.get(subObjId).getClass1());
            Class1 resClass = classModel.get(objectModel.get(resObjId).getClass1());
            subResPair.add(new Pair(subClass.getClassName(), resClass.getClassName()));
            
            /*
            Class1 subParentClass = subClass.getParentClass();
            Class1 resParentClass = resClass.getParentClass();
            if (subParentClass != null && resParentClass != null){
                subResPair.add(new Pair(subParentClass.getClassName(), resParentClass.getClassName()));
                subResPair.add(new Pair(subClass.getClassName(), resParentClass.getClassName()));
                subResPair.add(new Pair(subParentClass.getClassName(), resClass.getClassName()));
            }
            else if (subParentClass != null){
                subResPair.add(new Pair(subParentClass.getClassName(), resClass.getClassName()));
            }
            else if (resParentClass != null){
                subResPair.add(new Pair(subClass.getClassName(), resParentClass.getClassName()));
            }*/
        }
        
        return subResPair;
    }
    
    /**
     * This method return list of attribute paths of a type with specific path length limit. path length limit must be > 0
     * @param type
     * @param config
     * @param pathLimit
     * @return
     */
    private static ArrayList<ArrayList<String>> getAllAttributePaths(String type, Config config, int pathLimit){
        Map<String, Class1> classModel = config.getClassModel();
        ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
        // add all attributes of this current object.
        // id attribute
        ArrayList<String> idAttr = new ArrayList<String>();
        idAttr.add("id");
        results.add(idAttr);
        // boolean attributes
        for (String fieldName:config.getClassAttributes().get(type).keySet()){
            if (config.getClassAttributes().get(type).get(fieldName).getIsBoolean()){
                ArrayList<String> newBoolAttr = new ArrayList<String>();
                newBoolAttr.add(fieldName);
                results.add(newBoolAttr);
            }
        }
        ArrayList<Pair<String, ArrayList<String>>> currentClasses = new ArrayList<Pair<String, ArrayList<String>>>();
        currentClasses.add(new Pair(type, new ArrayList<String>()));
        for (int i = 0; i < pathLimit - 1; i++){
            ArrayList<Pair<String, ArrayList<String>>> newCurrentClasses = new ArrayList<Pair<String, ArrayList<String>>>();
            for (Pair<String, ArrayList<String>> currentClass:currentClasses){
                Set<Pair<String, String>> neighbors = config.getAdjacencyList().get(currentClass.getFirst());
                for (Pair<String, String> neighbor:neighbors){
                    ArrayList<String> newPath = new ArrayList<String>(currentClass.getSecond());
                    newPath.add(neighbor.getFirst());
                    newCurrentClasses.add(new Pair(neighbor.getSecond(), newPath));
                    ArrayList<String> newAttrPath = new ArrayList<String>(newPath);
                    newAttrPath.add("id");
                    results.add(newAttrPath);
                    // get all boolean attributes of this neighbor
                    for (String fieldName:config.getClassAttributes().get(neighbor.getSecond()).keySet()){
                        if (config.getClassAttributes().get(neighbor.getSecond()).get(fieldName).getIsBoolean()){
                            ArrayList<String> newBoolAttrPath = new ArrayList<String>(newPath);
                            newBoolAttrPath.add(fieldName);
                            results.add(newBoolAttrPath);
                        }
                    }
                }
            }
            currentClasses = newCurrentClasses;
        }
        return results;
    }
    
    
    private static void readConfiguration(String configFile, Config config){
        // configFile has the format of key-value pairs
        try{
            Properties inputProperties = new Properties();
            inputProperties.load(new FileInputStream(configFile));
            
            // read subConstraintExtraDistance
            int subConstraintExtraDistance = Integer.parseInt(inputProperties.getProperty("subConstraintExtraDistance"));
            config.setSubConstraintExtraDistance(subConstraintExtraDistance);
            
            // read resConstraintExtraDistance
            int resConstraintExtraDistance = Integer.parseInt(inputProperties.getProperty("resConstraintExtraDistance"));
            config.setResConstraintExtraDistance(resConstraintExtraDistance);
            
            // read totalConstraintLengthPathLimit
            int totalConstraintLengthPathLimit = Integer.parseInt(inputProperties.getProperty("totalConstraintLengthPathLimit"));
            config.setTotalConstraintLengthPathLimit(totalConstraintLengthPathLimit);
            
            // read subConditionPathLimit
            int subConditionPathLimit = Integer.parseInt(inputProperties.getProperty("subConditionPathLimit"));
            config.setSubConditionPathLimit(subConditionPathLimit);
            
            // read resConditionPathLimit
            int resConditionPathLimit = Integer.parseInt(inputProperties.getProperty("resConditionPathLimit"));
            config.setResConditionPathLimit(resConditionPathLimit);
            
            // read isOneAtomicConstraint
            String isOneAtomicConstraint = inputProperties.getProperty("isOneAtomicConstraint");
            config.setIsOneAtomicConstraint(isOneAtomicConstraint.equals("true"));
            
            // read policySize
            String policySize = inputProperties.getProperty("sizes");
            policySize = policySize.substring(1, policySize.length() -1);
            String[] policySizes = policySize.split(",");
            int[] sizes = new int[policySizes.length];
            for (int i = 0; i < policySizes.length; i++){
                sizes[i] = Integer.parseInt(policySizes[i]);
            }
            config.setPolicySize(sizes);
            
            // read numPoliciesPerSize
            int numPoliciesPerSize = Integer.parseInt(inputProperties.getProperty("numPoliciesPerSize"));
            config.setNumPoliciesPerSize(numPoliciesPerSize);
			
			// read runPolicy
            int runPolicy = Integer.parseInt(inputProperties.getProperty("runPolicy"));
            config.setRunPolicy(runPolicy);
            
            // read policyName
            String policyName = inputProperties.getProperty("policyName");
            config.setPolicyName(policyName);
            
            // read input file
            String inputPath = inputProperties.getProperty("inputPath");
            config.setInputPath(inputPath);
            
            // read output file
            String outputPath = inputProperties.getProperty("outputPath");
            config.setOutputPath(outputPath);
			
			// read time output file
            String outputTimePath = inputProperties.getProperty("outputTimePath");
            config.setOutputTimePath(outputTimePath);
        }
        catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    private static void HashMap() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
