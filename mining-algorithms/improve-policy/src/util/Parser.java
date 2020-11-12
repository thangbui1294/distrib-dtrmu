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

import algo.ReBACMiner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import util.FieldType.Multiplicity;

/**
 * Parser class is used for parsing .abac dataset.
 * @author Thang Bui
 */
public class Parser {
    final public class Pattern{
        // pattern of object line
        public static final String OBJECT_PATTERN = "object\\(.*\\)";
        public static final String CLASS_PATTERN = "class\\(.*\\)";
        public static final String COMMENT_PATTERN = "#.*";
        public static final String RULE_PATTERN = "rule\\(.*\\)";
        public static final String SEED_STRING_PATTERN = "seedString.*";
        public static final String END_OF_CLASS_LINE = "#endofclassmodel";
        // rule pattern for user friendly form
        public static final String SUB_PATTERN = "SUB:.*";
        public static final String RES_PATTERN = "RES:.*";
        public static final String CON_PATTERN = "CON:.*";
        public static final String ACT_PATTERN = "ACT:.*";
    }
    
    /**
     * This function parse noise data file and update the UP list in config
     * and set of UP tuple.
     * @param inputNoiseFile
     * @param config the configuration stores all models
     */
    public static void parseNoiseDataFile(String inputNoiseFile, Config config){
        try{
            FileReader fileReader = new FileReader(inputNoiseFile);
            BufferedReader inputReader = new BufferedReader(fileReader);
           
            String line;
            boolean isReadingUnderAssignments = true;
            
            HashSet<Triple<String, String, String>> underAssignmentList = new HashSet<>();
            HashSet<Triple<String, String, String>> overAssignmentList = new HashSet<>();
            
            // sequentially parse each line
            while ((line = inputReader.readLine()) != null) {
                if (line.toLowerCase().contains("over assignment tuples")){
                    isReadingUnderAssignments = false;
                    continue;
                }
                if (line.toLowerCase().contains("#")){
                    continue;
                }
                // parse tuple line
                String tupleString = line.substring(1, line.length() - 1);
                String[] tupleArray = tupleString.split(",");
                Triple<String, String, String> up = new Triple(tupleArray[0], tupleArray[1], tupleArray[2]);
                if (isReadingUnderAssignments && config.getUnderassignmentDetection()){
                    underAssignmentList.add(up);
                    config.getUPList().remove(up);
                }
                else if (config.getOverassignmentDetection()){
                    overAssignmentList.add(up);
                    config.getUPList().add(up);
                }
            }
            // update
            config.setUnderassignmentUP(underAssignmentList);
            config.setOverassignmentUP(overAssignmentList);
            computeUPMaps(config);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }  
    
    /**
     * This function parse log data file and update the UP list in config
     * and set of UP tuple.
     * @param inputLogFile
     * @param config the configuration stores all models
     */
    public static void parseLogDataFile(String inputLogFile, Config config){
        try{
            FileReader fileReader = new FileReader(inputLogFile);
            BufferedReader inputReader = new BufferedReader(fileReader);
           
            String line;
            boolean isReadingDeniedTuples = false;
            boolean isReadingPermittedTuples = false;
            // sequentially parse each line
            while ((line = inputReader.readLine()) != null) {
                if (line.toLowerCase().contains("permitted tuples")){
                    isReadingPermittedTuples = true;
                    continue;
                }
                if (line.toLowerCase().contains("denied tuples")){
                    isReadingDeniedTuples = true;
                    continue;
                }
                if (line.toLowerCase().contains("#")){
                    continue;
                }
                // parse tuple line
                String tupleString = line.substring(1, line.length() - 1);
                String[] tupleArray = tupleString.split(",");
                Triple<String, String, String> up = new Triple(tupleArray[0], tupleArray[1], tupleArray[2]);
                if (isReadingDeniedTuples){
                    // reading tuples in "Denied Tuples" section
                    config.getRejectedUPFromLog().add(up);
                }
                else if (!isReadingPermittedTuples){
                    // reading tuples in "Removed Tuples" section
                    config.getRemovedTuplesFromLog().add(up);
                    config.getUPList().remove(up);
                }
                else {
                    // reading tuples in "Permitted Tuples" section
                    config.getUPList().add(up);
                }
            }
            computeUPMaps(config);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }  
    
    /**
     * This function returns a map contains all objects, classes, rules, and un-removable attributes parsed from a .abac_txt file.
     * and set of UP tuple.
     * @param inputFile
     * @param config the configuration stores all models
     */
    public static void parseInputFile(String inputFile, Config config){
        try{
            FileReader fileReader = new FileReader(inputFile);
            BufferedReader inputReader = new BufferedReader(fileReader);
            // classLines stores all class definition lines. Used for creating field later
            ArrayList<String[]> classLines = new ArrayList<String[]>();

            String line;
            // sequentially parse each line
            while ((line = inputReader.readLine()) != null) {
                if (line.toLowerCase().replaceAll("\\s+", "").equals(Pattern.END_OF_CLASS_LINE)){
                    Parser.ProcessClassAttributes(config.getClassModel(), classLines);
                } else if (line.matches(Pattern.CLASS_PATTERN)) {
                    // match class definition
                    Parser.processClass(line, config.getClassModel(), classLines);
                } else if (line.matches(Pattern.OBJECT_PATTERN)) {
                    // match object definition
                    Parser.processObject(line, config.getObjectModel(), config.getClassModel());
                } else if (line.matches(Pattern.RULE_PATTERN)) {
                    // match rule definition
                    Parser.processRule(line, config.getRuleModel(), config.getClassModel(), config);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // compute class attributes map
        for (String className:config.getClassModel().keySet()){
            config.getClassAttributes().put(className, config.getClassModel().get(className).getAllAttributes());
        }
        // Check consistency between object model and rule model with class model
        if (!Parser.checkConsistency(config)){
            System.out.println("CONSISTENCY CHECK FAILD!!!");
        }
        else{
            for (String idKey:config.getObjectModel().keySet()){
                Object1 obj = config.getObjectModel().get(idKey);
                String classType = obj.getClass1();
                if (!config.getObjectList().containsKey(classType)){
                    ArrayList<Object1> newObjectList = new ArrayList<Object1>();
                    newObjectList.add(obj);
                    config.getObjectList().put(classType, newObjectList);
                }
                else{
                    ArrayList<Object1> newObjectList = new ArrayList<Object1>(config.getObjectList().get(classType));
                    newObjectList.add(obj);
                    config.getObjectList().put(classType, newObjectList);
                }
            }
            config.setUPRelation(Parser.computeUPRelation(config));
            Set<Triple<String, String, String>> upList = new HashSet<Triple<String, String, String>>();
            for (Rule key:config.getUPRelation().keySet()){
                for (Triple<String, String, String> up:config.getUPRelation().get(key)){
                    upList.add(up);
                }
            }
            config.setUPList(new ArrayList<Triple<String, String, String>>(upList));
            computeUPMaps(config);
            computeAdjacencyList(config);
            computeFieldNum(config);
            computeObjectCount(config);
        }
    }

    /**
     * This function parse a rules file from an input file to a specific list of rules
     * and set of UP tuple.
     * @param inputFile
     * @param config the configuration stores all models
     * @param ruleSet list of rules to parsed the rules to
     */
    public static void parseInputRulesFile(String inputFile, Config config, LinkedList<Rule> ruleSet){
        try{
            FileReader fileReader = new FileReader(inputFile);
            BufferedReader inputReader = new BufferedReader(fileReader);
            String line;
            // sequentially parse each line
            while ((line = inputReader.readLine()) != null) {
                Parser.processRule(line, ruleSet, config.getClassModel(), config);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * this method concatenates the rules in ruleFile to inputFile
     * @param inputFile
     * @param ruleFile
     */
    public static void concatFiles(String inputFile, String ruleFile) {
    	try {
    		FileReader reader = new FileReader(ruleFile);
            BufferedReader in = new BufferedReader(reader);
            FileWriter writer = new FileWriter(inputFile, true);
            BufferedWriter out = new BufferedWriter(writer);
            ArrayList<String> lines = new ArrayList<String>();
            String line = "";
            while ((line = in.readLine()) != null) {
            	lines.add(line);
            }
            for (String s : lines) {
            	out.write(s + "\n");
            }
            in.close();
            out.close();
    	} catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * this method compute upListMapOnSub and upListMapOnRes in the config
     * @param config 
     */
    public static void computeUPMaps(Config config){
        Map<String, Map<String, Set<Pair<String, String>>>> upListSub = new HashMap<String, Map<String, Set<Pair<String, String>>>>();
        Map<String, Map<String, Set<Pair<String, String>>>> upListRes = new HashMap<String, Map<String, Set<Pair<String, String>>>>();
        for (Triple<String, String, String> up:config.getUPList()){
            // subject
            String subClassName = config.getObjectModel().get(up.getFirst()).getClass1();
            if (!upListSub.containsKey(subClassName)){
                Set<Pair<String, String>> permList = new HashSet<Pair<String,String>>();
                permList.add(new Pair(up.getSecond(), up.getThird()));
                Map <String, Set<Pair<String, String>>> subMap = new HashMap<String, Set<Pair<String, String>>>();
                subMap.put(up.getFirst(), permList);
                upListSub.put(subClassName, subMap);
            }
            else {
                if (!upListSub.get(subClassName).containsKey(up.getFirst())){
                    Set<Pair<String, String>> permList = new HashSet<Pair<String,String>>();
                    permList.add(new Pair(up.getSecond(), up.getThird()));
                    upListSub.get(subClassName).put(up.getFirst(), permList);
                }
                else {
                    Set<Pair<String, String>> newResList = new HashSet<Pair<String, String>>(upListSub.get(subClassName).get(up.getFirst()));
                    newResList.add(new Pair(up.getSecond(), up.getThird()));
                    upListSub.get(subClassName).put(up.getFirst(), newResList);
                }
            }
            
            // resource
            String resClassName = config.getObjectModel().get(up.getSecond()).getClass1();
            if (!upListRes.containsKey(resClassName)){
                Set<Pair<String, String>> userList = new HashSet<Pair<String,String>>();
                userList.add(new Pair(up.getFirst(), up.getThird()));
                Map <String, Set<Pair<String, String>>> resMap = new HashMap<String, Set<Pair<String, String>>>();
                resMap.put(up.getSecond(), userList);
                upListRes.put(resClassName, resMap);
            }
            else {
                if (!upListRes.get(resClassName).containsKey(up.getSecond())){
                    Set<Pair<String, String>> userList = new HashSet<Pair<String,String>>();
                    userList.add(new Pair(up.getFirst(), up.getThird()));
                    upListRes.get(resClassName).put(up.getSecond(), userList);
                }
                else {
                    Set<Pair<String, String>> newSubList = new HashSet<Pair<String, String>>(upListRes.get(resClassName).get(up.getSecond()));
                    newSubList.add(new Pair(up.getFirst(), up.getThird()));
                    upListRes.get(resClassName).put(up.getSecond(), newSubList);
                }
            }
        }
        config.setUPListMapOnSub(upListSub);
        config.setUPListMapOnRes(upListRes);
    }
    /**
     * This method used to process a line represent an object.
     * It will create new Object1 and add to objects map.
     * @param line line that represents an object
     * @param objects map of parsed objects
     * @param classes map of all parsed classes
     */
    public static void processObject(String line, Map<String, Object1> objects, Map<String, Class1> classes){
        String content = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
        content = content.trim();
        String[] attributes = content.split(";");
        // Get type and id of the object from first and second element of attributes
        String class1 = attributes[0].trim();
        String[] idField = attributes[1].split("=");
        if (!idField[0].trim().equals("id")){
            System.out.println("ID for the object not found");
            System.exit(0);
        }
        String id = idField[1].trim();
        Map<String, FieldValue> data = new HashMap<String, FieldValue>();
        if (attributes.length > 2){
            for (int i = 2; i < attributes.length; i++){
                String[] pair = attributes[i].split("=");
                String fieldName = pair[0].trim();
                String fieldContent = pair[1].trim();
                FieldValue value = new FieldValue();
                if (!fieldContent.equals("null") && !fieldContent.equals("unknown")) {
                    if (fieldContent.indexOf('{') == -1){
                        // Case that this field content is a single-valued attribute
                        if (classes.get(class1).getAllAttributes().get(fieldName).getIsBoolean()){
                            value.setBool(fieldContent.equals("true"));
                        }
                        else{
                            value.setObjId(fieldContent);
                        }
                    }
                    else {
                        // Case that this field content is a mul-valued  attribute
                        value.setSetObjId(new HashSet<String>());
                        fieldContent = fieldContent.substring(1, fieldContent.length() -1);
                        String[] setValues = fieldContent.split(",");
                        for (String val:setValues){
                            value.addToSetObjId(val.trim());
                        }
                    }
                }
                else if (fieldContent.equals("unknown")){
                    value.setIsUnknown(true);
                }
                data.put(fieldName, value);
            }
        }
        // Create new object
        objects.put(id, new Object1(class1, id, data));
    }
    
    
    /**
     * This method used to process a line represent a class.
     * It will create new Class1 and add to classes map.
     * @param line line that represents an object
     * @param classes list of parsed objects
     * @param classLines stores all content of class definition line
     */
    public static void processClass(String line, Map<String, Class1> classes, ArrayList<String[]> classLines){
        // !! Check if this is consistent with class model
        String content = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
        String[] fields = content.split(";");
        classLines.add(fields);
        // Get class name from the first element of fields
        String className = fields[0].trim();
        Class1 class1 = new Class1(className);
        if (!fields[1].equals(" ")){
            class1.setParentClass(classes.get(fields[1].trim()));
        }
        classes.put(className, class1);
    }
    
    /**
     * This method compute the attributes of each class
     * @param classes map of all classes
     * @param classLines content of class definition from input file
     */
    public static void ProcessClassAttributes(Map<String, Class1> classes, ArrayList<String[]> classLines){
        for (String[] fields:classLines){
            Map<String, FieldType> attributes = new HashMap<String, FieldType>();
            if (fields.length > 2){
                String className = fields[0].trim();
                for (int i = 2; i < fields.length; i++){
                    String[] pair = fields[i].split(":");
                    String fieldName = pair[0].trim();
                    String fieldContent = pair[1].trim();
                    switch (fieldContent.charAt(fieldContent.length()-1)) {
                        case '*':
                        {
                            // MANY multiplicity
                            String type = fieldContent.substring(0, fieldContent.length() - 1);
                            attributes.put(fieldName, new FieldType(false, classes.get(type), Multiplicity.MANY));
                            break;
                        }
                        case '?':
                        {
                            // OPTIONAL multiplicity
                            String type = fieldContent.substring(0, fieldContent.length() - 1);
                            attributes.put(fieldName, new FieldType(false, classes.get(type), Multiplicity.OPTIONAL));
                            break;
                        }
                        default:
                        {
                            // ONE multiplicity
                            if (fieldContent.equals("boolean") || fieldContent.equals("Boolean")){
                                attributes.put(fieldName, new FieldType(true, null, Multiplicity.ONE));
                            }
                            else{
                                attributes.put(fieldName, new FieldType(false, classes.get(fieldContent), Multiplicity.ONE));
                            }
                            break;
                        }
                    }
                }               
                // add attributes to the corresponding class
                classes.get(className).setAttributes(attributes);
            }
        }
    }

    /**
     * This method process a line represent a rule definition
     * @param line the string line
     * @param rules list of rules
     * @param classes map of all classes
     * @param config
     */
    public static void processRule(String line, List<Rule> rules, Map<String, Class1> classes, Config config){
        String content = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
        content = content.trim();
        String[] fields = content.split(";");
        // process subject type and resource type
        String subjectType = fields[0].trim();
        String resourceType = fields[2].trim();
        // subject Condition
        String[] subCondition = null;
        if (!fields[1].equals(" ")){
            String[] subConditions = fields[1].split("and");
            subCondition = new String[subConditions.length];
            for (int i = 0; i < subCondition.length; i++){
                subCondition[i] = subConditions[i].trim();
            }
        }
        // resource Condition
        String[] resCondition = null;
        if (!fields[3].equals(" ")){
            String[] resConditions = fields[3].split("and");
            resCondition = new String[resConditions.length];
            for (int i = 0; i < resCondition.length; i++){
                resCondition[i] = resConditions[i].trim();
            }
        }

        // constraint
        String[] constraint = null;
        if (!fields[4].equals(" ")){
            String[] constraints = fields[4].split("and");
            constraint = new String[constraints.length];
            for (int i = 0; i < constraint.length; i++){
                constraint[i] = constraints[i].trim();
            }
        }

        // actions
        String[] action = null;
        fields[5] = fields[5].trim();
        String actionsContent = fields[5].substring(1, fields[5].length() - 1);
        if (!actionsContent.equals("")){
            String[] actions = actionsContent.split(",");
            action = new String[actions.length];
            for (int i = 0; i < action.length; i++){
                action[i] = actions[i].trim();
            }
        }
        rules.add(createRule(subjectType, subCondition, resourceType, resCondition, constraint, action, classes));
    }
    
    public static Rule createRule(String subType, String[] subCondition, String resType, String[] resCondition, String[] constraint, String[] ops, Map<String, Class1> classes){
        ArrayList<AtomicCondition> subjectCondition = new ArrayList<AtomicCondition>();
        ArrayList<AtomicCondition> resourceCondition = new ArrayList<AtomicCondition>();
        ArrayList<AtomicConstraint> constraints = new ArrayList<AtomicConstraint>();
        Set<String> opsSet = new HashSet<String>();
        if (subCondition != null){
            for (String con:subCondition){
                Boolean isNegativeCond = false;
                if (con.indexOf('!') > -1){
                    isNegativeCond = true;
                }
                String constantString = con.substring(con.indexOf('{') + 1, con.indexOf('}'));
                String[] elements = con.split(" ");
                ArrayList<String> path = new ArrayList<String>();
                String[] pathElements = elements[0].split("\\.");
                for (String pe:pathElements){
                    path.add(pe);
                }
                Set<String> constant = new HashSet<String>();
                String[] constants = constantString.split(",");
                for (String c:constants){
                    constant.add(c.trim());
                }
                if (elements[1].toLowerCase().equals("in")){
                    AtomicCondition sc = new AtomicCondition(path, constant, ConditionOperator.IN);
                    sc.setIsNegative(isNegativeCond);
                    subjectCondition.add(sc);
                }
                else if (elements[1].toLowerCase().equals("contains")){
                    AtomicCondition sc = new AtomicCondition(path, constant, ConditionOperator.CONTAINS);
                    sc.setIsNegative(isNegativeCond);
                    subjectCondition.add(sc);
                }
            }
        }
        if (resCondition != null){
            for (String con:resCondition){
                Boolean isNegativeCond = false;
                if (con.indexOf('!') > -1){
                    isNegativeCond = true;
                }
                String constantString = con.substring(con.indexOf('{') + 1, con.indexOf('}'));
                String[] elements = con.split(" ");
                ArrayList<String> path = new ArrayList<String>();
                String[] pathElements = elements[0].split("\\.");
                for (String pe:pathElements){
                    path.add(pe);
                }
                Set<String> constant = new HashSet<String>();
                String[] constants = constantString.split(",");
                for (String c:constants){
                    constant.add(c.trim());
                }
                if (elements[1].toLowerCase().equals("in")){
                    AtomicCondition rc = new AtomicCondition(path, constant, ConditionOperator.IN);
                    rc.setIsNegative(isNegativeCond);
                    resourceCondition.add(rc);
                }
                else if (elements[1].toLowerCase().equals("contains")){
                    AtomicCondition rc = new AtomicCondition(path, constant, ConditionOperator.CONTAINS);
                    rc.setIsNegative(isNegativeCond);
                    resourceCondition.add(rc);
                }
            }
        }
        if (constraint != null){
            for (String con:constraint){
                Boolean isNegativeCons = false;
                if (con.indexOf('!') > -1){
                    isNegativeCons = true;
                }
                String[] elements = con.split(" ");
                ArrayList<String> subPath = new ArrayList<String>();
                String[] path = elements[0].split("\\.");
                for (String pathElement:path){
                    subPath.add(pathElement);
                }
                ArrayList<String> resPath = new ArrayList<String>();
                if (isNegativeCons){
                    elements[2] = elements[2].substring(0, elements[2].length() - 4);
                }
                path = elements[2].split("\\.");
                for (String pathElement:path){
                    resPath.add(pathElement);
                }
                if (elements[1].toLowerCase().equals("in")){
                    AtomicConstraint ac = new AtomicConstraint(subPath, resPath, ConstraintOperator.IN);
                    ac.setIsNegative(isNegativeCons);
                    constraints.add(ac);
                }
                else if (elements[1].toLowerCase().equals("=")){
                    if (checkValidPath(subType, subPath, classes).getSecond()){
                        AtomicConstraint ac = new AtomicConstraint(subPath, resPath, ConstraintOperator.EQUALS_SET);
                        ac.setIsNegative(isNegativeCons);
                        constraints.add(ac);
                    }
                    else{
                        AtomicConstraint ac = new AtomicConstraint(subPath, resPath, ConstraintOperator.EQUALS_VALUE);
                        ac.setIsNegative(isNegativeCons);
                        constraints.add(ac);
                    }
                }
                else if (elements[1].toLowerCase().equals("contains")){
                    AtomicConstraint ac = new AtomicConstraint(subPath, resPath, ConstraintOperator.CONTAINS);
                    ac.setIsNegative(isNegativeCons);
                    constraints.add(ac);
                }
                else if (elements[1].toLowerCase().equals("supseteq")){
                    AtomicConstraint ac = new AtomicConstraint(subPath, resPath, ConstraintOperator.SUPSETEQ);
                    ac.setIsNegative(isNegativeCons);
                    constraints.add(ac);
                }
                else if (elements[1].toLowerCase().equals("subseteq")){
                    AtomicConstraint ac = new AtomicConstraint(subPath, resPath, ConstraintOperator.SUBSETEQ);
                    ac.setIsNegative(isNegativeCons);
                    constraints.add(ac);
                }
            }
        }
        for (String op:ops){
            opsSet.add(op);
        }
        return new Rule(classes.get(subType),subjectCondition, classes.get(resType), resourceCondition, constraints, opsSet);
    }
    
    /** 
     * This method process a line represent a rule definition in readable format
     * @param line the string line
     * @param rules list of rules
     * @param classes map of all classes
     */
    /*public static void processReadableRule(String line, LinkedList<Rule> rules, Map<String, Class1> classes){
        line = line.trim();
        String[] fields = line.split("[|]");
        String temp = "";
        int index = 0;
        int index1 = 0;
        // process subject type
        String subjectType = "";
        temp = fields[0].trim();
        temp = temp.substring(4);
        temp = temp.trim();
        index = temp.indexOf("and", -1);
        index1 = temp.toLowerCase().indexOf("isinstance", -1);
        if (index1 != -1) {
        	if (index != -1) {
        		subjectType = temp.substring(index1 + 11, index).trim();
        	} else {
        		subjectType = temp.substring(index1 + 11).trim();
        	}
        } else {
        	System.out.println("FORMAT ERROR");
        	return;
        }
        // subject Condition
        String[] subCondition = null;
        if (index != -1){
        	temp = temp.substring(index + 4);
        	temp = temp.trim();
            String[] subConditions = temp.split("and");
            subCondition = new String[subConditions.length];
            for (int i = 0; i < subCondition.length; i++){
                subCondition[i] = subConditions[i].trim().replace("sub.", "");
            }
        }
        
        // process resource type
        String resourceType = "";
        temp = fields[1].trim();
        temp = temp.substring(4);
        temp = temp.trim();
        index = temp.indexOf("and", -1);
        index1 = temp.toLowerCase().indexOf("isinstance", -1);
        if (index1 != -1) {
        	if (index != -1) {
        		resourceType = temp.substring(index1 + 11, index).trim();
        	} else {
        		resourceType = temp.substring(index1 + 11).trim();
        	}
        } else {
        	System.out.println("FORMAT ERROR");
        	return;
        }
        // resource Condition
        String[] resCondition = null;
        if (index != -1){
        	temp = temp.substring(index + 4);
            String[] resConditions = temp.split("and");
            resCondition = new String[resConditions.length];
            for (int i = 0; i < resCondition.length; i++){
                resCondition[i] = resConditions[i].trim().replace("res.", "");
            }
        }
        
        // constraint 
        String[] constraint = null;
        temp = fields[2].trim();
        temp = temp.substring(4);
        temp = temp.trim();
        if (!temp.equals("")){
            String[] constraints = temp.split("and");
            constraint = new String[constraints.length];
            for (int i = 0; i < constraint.length; i++){
                constraint[i] = constraints[i].trim().replace("res.", "").replace("sub.", "");
            }
        }
        
        // actions
        String[] action = null;
        temp = fields[3].trim();
        temp = temp.substring(4);
        temp = temp.trim();
        if (!temp.equals("")){
            String[] actions = temp.split(",");
            action = new String[actions.length];
            for (int i = 0; i < action.length; i++){
                action[i] = actions[i].trim();
            }
        }
        rules.add(CaseStudyGenerator.createReadableRule(subjectType, subCondition, resourceType, resCondition, constraint, action, classes));
    }	*/
    
    /**
     * This function check if object model and rule model consistent with class model
     * @param config the abac policy contains class, object, and rule models
     * @return
     */
    /**
     * This function check if object model and rule model consistent with class model
     * @param config the abac policy contains class, object, and rule models
     * @return
     */
    public static boolean checkConsistency(Config config){
        Map<String, Object1> objects = config.getObjectModel();
        Map<String, Class1> classes = config.getClassModel();

        // CHECK OBJECTS
        for (Map.Entry<String, Object1> entry:objects.entrySet()){
            String objectType = entry.getValue().getClass1();
            // Check if class name is valid
            if (!classes.containsKey(objectType)){
                System.out.println("Invalid class name: " + objectType);
                return false;
            }
            // Check attributes
            Map<String, FieldType> classAttributes = classes.get(objectType).getAllAttributes();
            // check size of data field, note that id field is not explicitly defined in class model
            if (entry.getValue().getData().size()  != classAttributes.size()){
                System.out.println("Invalid data in object: " + entry.getKey());
                System.out.println("Object type: " + objectType);
                System.out.println("entry data size: " + entry.getValue().getData().size());
                System.out.println("class attributes size: " + classAttributes.size());
                return false;
            }
            for (String key:entry.getValue().getData().keySet()){
                if (!classAttributes.containsKey(key)){
                    System.out.println("Invalid field " + key + " in object " + entry.getKey());
                    return false;
                }
                FieldValue value = entry.getValue().getData().get(key);
                // check if the value's type matches
                if (classAttributes.get(key).getIsBoolean()){
                    // check if correct type is Boolean
                    if (value.getObjId() != null || value.getSetObjId() != null){
                        System.out.println("Invalid value: Not a boolean. Field " + key + " in object " + entry.getKey());
                        return false;
                    }
                }
                else{
                    String correctType = classAttributes.get(key).getType().getClassName();
                    Multiplicity correctMul = classAttributes.get(key).getMultiplicity();
                    if (correctMul == Multiplicity.ONE && value.getObjId() == null && !value.getIsUnknown()){
                        System.out.println("Invalid value: Multiplicity is ONE. Field " + key + " in object " + entry.getKey());
                        return false;
                    }
                    if (correctMul == Multiplicity.MANY && value.getObjId() != null){
                        System.out.println("Invalid value: Multiplicity is MANY. Field " + key + " in object " + entry.getKey());
                        return false;
                    }
                    if ((correctMul == Multiplicity.ONE || correctMul == Multiplicity.OPTIONAL) && value.getSetObjId() != null){
                        System.out.println("Invalid value: Multiplicity is :" + correctMul+ ". Field " + key + " in object " + entry.getKey());
                        return false;
                    }
                    if (value.getObjId() != null){
                        if (!objects.containsKey(value.getObjId())){
                            System.out.println("Invalid object id: " + value.getObjId() + ". Field " + key + " in object " + entry.getKey());
                            return false;
                        }
                        String valueType = objects.get(value.getObjId()).getClass1();
                        String parentClass = "";
                        if (classes.get(valueType) != null && classes.get(valueType).getParentClass() != null){
                            parentClass = classes.get(valueType).getParentClass().getClassName();
                        }
                        if (!correctType.equals(valueType) && !correctType.equals(parentClass)){
                            System.out.println("Invalid value type: " + value.getObjId() + ". Field " + key + " in object " + entry.getKey());
                            return false;
                        }
                    }
                    else if (value.getSetObjId() != null){
                        // setObjId != null means that setObjId has at least 1 element
                        for (String element:value.getSetObjId()){
                            if (!objects.containsKey(element)){
                                System.out.println("Invalid object id: " + element + ". Field " + key + " in object " + entry.getKey());
                                return false;
                            }
                            String valueType = objects.get(element).getClass1();
                            String parentClass = "";
                            if (classes.get(valueType) != null && classes.get(valueType).getParentClass() != null){
                                parentClass = classes.get(valueType).getParentClass().getClassName();
                            }
                            if (!correctType.equals(valueType) && !correctType.equals(parentClass)){
                                System.out.println("Invalid value type: " + element + ". Field " + key + " in object " + entry.getKey());
                                return false;
                            }
                        }
                    }
                }
            }
        }
        // CHECK RULES
        for (Rule r:config.getRuleModel()){
            // Check subjectType and resourceType
            if (!classes.containsKey(r.getSubjectType().getClassName()) || !classes.containsKey(r.getResourceType().getClassName())){
                System.out.println("Rule Error: Invalid Type");
                return false;
            }
            // Check subject and resrouce conditions
            for (AtomicCondition sc:r.getSubjectCondition()){
                Triple<Boolean, Boolean, String> validity = Parser.checkValidPath(r.getSubjectType().getClassName(), sc.getPath(), classes);
                if (!validity.getFirst()){
                    System.out.println("Rule Error: Invalid subject path " + sc.getPath());
                    return false;
                }
                if (sc.getConditionOperator() != ConditionOperator.IN && sc.getConditionOperator() != ConditionOperator.CONTAINS){
                    System.out.println("Invalid subject condition operator" + sc.getPath());
                    return false;
                }
                if (sc.getConditionOperator() == ConditionOperator.IN && validity.getSecond()){
                    System.out.println("Inconsistency in subject condition operator and multiplicity" + sc.getPath());
                    return false;
                }
                if (sc.getConditionOperator() == ConditionOperator.CONTAINS && !validity.getSecond()){
                    System.out.println("Inconsistency in subject condition operator and multiplicity" + sc.getPath());
                    return false;
                }
            }
            for (AtomicCondition rc:r.getResourceCondition()){
                Triple<Boolean, Boolean, String> validity = Parser.checkValidPath(r.getResourceType().getClassName(), rc.getPath(), classes);
                if (!validity.getFirst()){
                    System.out.println("Rule Error: Invalid resource path");
                    return false;
                }
                if (rc.getConditionOperator() != ConditionOperator.IN && rc.getConditionOperator() != ConditionOperator.CONTAINS){
                    System.out.println("Invalid resource condition operator");
                    return false;
                }
                if (rc.getConditionOperator() == ConditionOperator.IN && validity.getSecond()){
                    System.out.println("Inconsistency in subject condition operator and multiplicity" + rc.getPath());
                    return false;
                }
                if (rc.getConditionOperator() == ConditionOperator.CONTAINS && !validity.getSecond()){
                    System.out.println("Inconsistency in subject condition operator and multiplicity" + rc.getPath());
                    return false;
                }
            }
            // Check constraints
            for (AtomicConstraint c:r.getConstraint()){
                Triple<Boolean, Boolean, String> validity = Parser.checkValidPath(r.getSubjectType().getClassName(), c.getSubPath(), classes);
                if (!validity.getFirst()){
                    System.out.println("Rule Error: Invalid subject path - constraint");
                    return false;
                }
                Triple<Boolean, Boolean, String> validity1 = Parser.checkValidPath(r.getResourceType().getClassName(), c.getResPath(), classes);
                if (!validity1.getFirst()){
                    System.out.println("Rule Error: Invalid resource path - constraint");
                    return false;
                }
                if (!validity.getSecond() && !validity1.getSecond() && c.getConstraintOperator() != ConstraintOperator.EQUALS_VALUE){
                    System.out.println("Invalid constraint operator, it should be EQUALS_VALUE");
                    System.out.println("Sub Type: " + r.getSubjectType().getClassName() + "\nRes Type: " + r.getResourceType().getClassName());
                    System.out.println(c);
                    return false;
                }
                if (!validity.getSecond() && validity1.getSecond() && c.getConstraintOperator() != ConstraintOperator.IN){
                    System.out.println("Invalid constraint operator, it should be IN");
                    return false;
                }
                if (validity.getSecond() && !validity1.getSecond() && c.getConstraintOperator() != ConstraintOperator.CONTAINS){
                    System.out.println("Invalid constraint operator, it should be CONTAINS");
                    return false;
                }
                if (validity.getSecond() && validity1.getSecond() && c.getConstraintOperator() != ConstraintOperator.SUPSETEQ
                        && c.getConstraintOperator() != ConstraintOperator.SUBSETEQ && c.getConstraintOperator() != ConstraintOperator.EQUALS_SET){
                    System.out.println(c);
                    System.out.println("Invalid constraint operator");
                    return false;
                }
                if (!validity.getThird().equals(validity1.getThird()) && 
                        (classes.get(validity.getThird()).getParentClass() != null && !classes.get(validity.getThird()).getParentClass().equals(validity1.getThird())) &&
                        (classes.get(validity1.getThird()).getParentClass() != null && !classes.get(validity1.getThird()).getParentClass().equals(validity.getThird()))
                        ){
                    System.out.println("Inconsistency constraint types");
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * this method check if a path in a atomic condition or atomic constraint is valid
     * @param type
     * @param path
     * @param classes
     * @return an triple value, consist of 2 boolean values and a string
     * return value 1: boolean indicate if the path is valid
     * return value 2: boolean indicate if the path has MULTIPLE multiplicity
     * return value 3: string indicate the type of the path (type of last attribute other than id)
     */
    public static Triple<Boolean, Boolean, String> checkValidPath(String type, ArrayList<String> path, Map<String, Class1> classes){
        boolean validPath = true; 
        boolean isManyMultiplicity = false;
        String currentClassName  = type;
        for (int i = 0; i < path.size(); i++){
            String attr = path.get(i);
            if (i != path.size() - 1 && attr.equals("id")){
                validPath = false;
            }
            if (attr.equals("id")){
                continue;
            }
            if (!classes.get(currentClassName).getAllAttributes().containsKey(attr)){
                validPath = false;
            }
            if (classes.get(currentClassName).getAllAttributes().get(attr) == null){
                validPath = false;
                return new Triple(validPath, null, null);
            }
            if (classes.get(currentClassName).getAllAttributes().get(attr).getMultiplicity() == Multiplicity.MANY){
                isManyMultiplicity = true;
            }
            if (classes.get(currentClassName).getAllAttributes().get(attr).getIsBoolean()){
                continue;
            }
            currentClassName = classes.get(currentClassName).getAllAttributes().get(attr).getType().getClassName();
        }
        return new Triple(validPath, isManyMultiplicity, currentClassName);
    }
    
    /**
     * This method generates set of UP tuples from object model and rule model
     * @param config configuration contains object model and rule model
     * @return set of UP triples.
     */
    public static Map<Rule, Set<Triple<String, String, String>>> computeUPRelation(Config config){
        Map<String, Object1> objects  = config.getObjectModel();
        LinkedList<Rule> rules = config.getRuleModel();
        Map<String, Class1> classes = config.getClassModel();
        Map<Rule, Set<Triple<String, String, String>>> results = new HashMap<Rule, Set<Triple<String, String, String>>>();       
        ArrayList<Pair<Integer, Rule>> ruleUPList = new ArrayList<Pair<Integer, Rule>>();
        for (Rule r:rules){       
            Set<Triple<String, String, String>> upRelations = new HashSet<Triple<String, String, String>>();
            int countUP = 0;
            ArrayList<Object1> satisfiedSubObjects = new ArrayList<Object1>();
            ArrayList<Object1> satisfiedResObjects = new ArrayList<Object1>();
            // get all objects satisfy subject Condition
            for (Object1 obj:objects.values()){
                if (Parser.checkSatisfyAtomicConditions(obj,r, true, objects, classes, config)){
                    satisfiedSubObjects.add(obj);
                }
                if (Parser.checkSatisfyAtomicConditions(obj,r, false, objects, classes, config)){
                    satisfiedResObjects.add(obj);
                }              
            }            
            for (Object1 sub:satisfiedSubObjects){
                for (Object1 res:satisfiedResObjects){
                    if (ReBACMiner.checkSatisfyConstraints(sub, res, r.getConstraint(), config)){
                        for (String action:r.getActions()){
                            upRelations.add(new Triple(sub.getId(), res.getId(), action));
                            countUP++;
                        }
                    }
                }
            }
            ruleUPList.add(new Pair(countUP, r));
            results.put(r, upRelations);
        }
        config.setRuleListWithUP(ruleUPList);
        Collections.sort(ruleUPList);
        for (Pair<Integer, Rule> entry:config.getRuleListWithUP()){
            System.out.println(entry);
        }
        return results;
    }
    
    /**
     * This method generate the adjacency list for all Class in class model
     * @param config
     */
    public static void computeAdjacencyList(Config config){
        Map<String, Class1> classModel = config.getClassModel();
        Map<String, Set<Pair<String,String>>> adjacentList = new HashMap<String, Set<Pair<String, String>>>();
        for (String classID:classModel.keySet()){
            Set<Pair<String, String>> neighbors = new HashSet<Pair<String, String>>();
            for (Map.Entry<String, FieldType> entry:classModel.get(classID).getAllAttributes().entrySet()){
                if (!entry.getValue().getIsBoolean()){
                    neighbors.add(new Pair(entry.getKey(), entry.getValue().getType().getClassName()));
                }
            }
            adjacentList.put(classID, neighbors);
        }
        config.setAdjacencyList(adjacentList);
    }
    
    /**
     * this method compute the number of each object ID appear in the attribute data
     * @param config 
     */
    public static void computeObjectCount(Config config){
        Map<String, Object1> objectModel = config.getObjectModel();
        Map<String, Integer> objectCount = config.getObjectCount();
        // first put entries for all object ID with initialized count = 0
        for (String object:objectModel.keySet()){
            objectCount.put(object, 0);
        }
        // go through each object, increase object ID count from field values
        for (String object:objectModel.keySet()){
            Object1 objectField = objectModel.get(object);
            Map<String, FieldValue> fields = objectField.getData();
            for (String fieldName:fields.keySet()){
                FieldValue fieldValue = fields.get(fieldName);
                if (fieldValue.getObjId() != null){
                    objectCount.replace(fieldValue.getObjId(), objectCount.get(fieldValue.getObjId()) + 1);
                }
                else if (fieldValue.getSetObjId() != null && !fieldValue.getSetObjId().isEmpty()){
                    for (String value:fieldValue.getSetObjId()){
                        objectCount.replace(value, objectCount.get(value) + 1);
                    }
                }
            }
        }
    }
    
    /**
     * This method compute the number of fields for all objects in object model
     * @param config
     */
    public static void computeFieldNum(Config config){
        Map<String, Class1> classModel = config.getClassModel();
        Map<String, Integer> classFieldNum = new HashMap<>();
        Map<String, Object1> objectModel = config.getObjectModel();
        int totalFieldNum = 0;
        for (String classID:classModel.keySet()){
        	int fieldNum = 0;
        	Class1 currentClass = classModel.get(classID);
        	Map<String, FieldType> attributeMap = currentClass.getAttributes();
        	if (attributeMap != null) {
	        	for (String key:attributeMap.keySet()) {
	        		if (!attributeMap.get(key).getIsBoolean()) {
	        			fieldNum++;
	        		}
	        	}
        	}
        	Class1 parentClass = currentClass.getParentClass();
        	while (parentClass != null) {
        		attributeMap = parentClass.getAttributes();
            	if (attributeMap != null) {
    	        	for (String key:attributeMap.keySet()) {
    	        		if (!attributeMap.get(key).getIsBoolean()) {
    	        			fieldNum++;
    	        		}
    	        	}
            	}
        		parentClass = parentClass.getParentClass();
        	}
        	classFieldNum.put(classID, fieldNum);
        }
        for (String objectID:objectModel.keySet()) {
            Object1 currentObject = objectModel.get(objectID);
            totalFieldNum += classFieldNum.get(currentObject.getClass1());
            /*if (objectMap.containsKey(currentObject.getClass1())) {
            	int n = objectMap.get(currentObject.getClass1());
            	objectMap.put(currentObject.getClass1(), n + 1);
            } else {
            	objectMap.put(currentObject.getClass1(), 1);
            }*/
        }
        /*int sum = 0;
        for (String cName:objectMap.keySet()) {
        	sum += classFieldNum.get(cName) * objectMap.get(cName);
        }
        System.out.println(sum);*/
        config.setTotalFieldNum(totalFieldNum);
    }
    
    /**
     * This method check if an object satisfy an a atomic conditions. It can be
     * both subject conditions or resource conditions
     * @param obj the object need to check
     * @param rule the rule contains the atomic conditions
     * @param isSubject true if checking with subject condition, false if checking 
     * @param objects object model
     * @param classes class model
     * resource condition
     * @return 
     */
    public static boolean checkSatisfyAtomicConditions(Object1 obj, Rule rule, boolean isSubject, Map<String, Object1> objects, Map<String, Class1> classes, Config config){
        // Note that in our current case study, we only have 1-level inheritance.
        if (isSubject){
            if (classes.get(obj.getClass1()).getParentClass() != null){
                if (!obj.getClass1().equals(rule.getSubjectType().getClassName()) && !classes.get(obj.getClass1()).getParentClass().getClassName().equals(rule.getSubjectType().getClassName())){
                    return false;
                }
            }
            else {
                if (!obj.getClass1().equals(rule.getSubjectType().getClassName())){
                    return false;
                }
            }
        }
        else {
            if (classes.get(obj.getClass1()).getParentClass() != null){
                if (!obj.getClass1().equals(rule.getResourceType().getClassName()) && !classes.get(obj.getClass1()).getParentClass().getClassName().equals(rule.getResourceType().getClassName())){
                    return false;
                }
            }
            else { 
                if (!obj.getClass1().equals(rule.getResourceType().getClassName())){
                    return false;
                }
            }
        }
        ArrayList<AtomicCondition> conditions;
        if (isSubject){
            conditions = rule.getSubjectCondition();
        }
        else conditions = rule.getResourceCondition();
        for (AtomicCondition ac:conditions){
            if (!ReBACMiner.checkSatisfyAtomicCondition(obj, ac, objects, classes, config)){
                return false;
            }
        }
        return true;
    }
    
    /**
     * This method returns attribute values of an object from a given path.
     * @param obj1 the object need to be checked
     * @param path the attribute path
     * @param objects object model
     * @param classes class model
     * @return true and an array list of string with singleton "true or false", or
     * false with list of objects' ids.
     */
    public static Triple<Boolean, ArrayList<String>, Boolean> getAttributePathValues(Object1 obj1, ArrayList<String> path, Map<String, Object1> objects, Map<String, Class1> classes, Config config){
        ArrayList<String> results = new ArrayList<String>();
        boolean isBoolean = false;
        boolean isUnknown = false;
        ArrayList<Object1> currentObjects = new ArrayList<Object1>();
        currentObjects.add(obj1);
        String currentClassName = obj1.getClass1();
        for (String s:path){
            if (!isUnknown && !s.equals("id")){
                for (Object1 obj: currentObjects){
                    if (obj.getData().get(s) == null){
                        System.out.println();
                    }
                    if (obj.getData().get(s).getIsUnknown()){
                        isUnknown = true;
                        break;
                    }
                }
                
            }
            if (s.equals("id")){
                for (Object1 obj:currentObjects){
                    results.add(obj.getId());
                }
                break;
            }
            else if (config.getClassAttributes().get(currentClassName).get(s).getIsBoolean()){
                isBoolean = true;
                if (!currentObjects.isEmpty()){
                    for (Object1 obj : currentObjects) {
                        if (obj.getData().get(s).getBool()) {
                            results.add("true");
                        }
                        else{
                            results.add("false");
                        }
                    }
                }
                break;
            }
            else {
                ArrayList<Object1> oldObjects = new ArrayList<Object1>(currentObjects);
                currentObjects.clear();
                if (config.getClassAttributes().get(currentClassName).get(s).getMultiplicity() == Multiplicity.ONE || config.getClassAttributes().get(currentClassName).get(s).getMultiplicity() == Multiplicity.OPTIONAL){
                    for (Object1 obj:oldObjects){
                        if (obj.getData().get(s).getObjId() != null){
                            currentObjects.add(objects.get(obj.getData().get(s).getObjId()));
                        }
                    }
                }
                else if (config.getClassAttributes().get(currentClassName).get(s).getMultiplicity() == Multiplicity.MANY){
                    for (Object1 obj:oldObjects){
                        if (obj.getData().get(s).getSetObjId() != null){
                            for (String id:obj.getData().get(s).getSetObjId()){
                                currentObjects.add(objects.get(id));
                            }
                        }
                    }
                }
                currentClassName = config.getClassAttributes().get(currentClassName).get(s).getType().getClassName();
            } 
        }
        return new Triple(isBoolean, results, isUnknown);
    }
//    public static void main(String[] args){


        /* EMR */
//        int[] emrSize = {5, 10, 15, 20, 25, 30, 35, 40};
//        int numPoliciesPerEMRSize = 10;
//        for (int i = 0; i < emrSize.length; i++){
//            int size = emrSize[i];
//            for (int j = 0; j < numPoliciesPerEMRSize; j++){
//                CaseStudyGenerator c = new EMRCaseStudyGenerator();
//                c.generateCaseStudy("", "EMR_" + size + "_" + j, size, null);
//            }
//        }
        
        /* Grant Proposal */
//        int[] gpSize = {50};
//        int numPoliciesPerGPSize = 30;
//        for (int i = 0; i < gpSize.length; i++){
//            int size = gpSize[i];
//            for (int j = 0; j < numPoliciesPerGPSize; j++){
//                CaseStudyGenerator c = new GrantProposalCaseStudyGenerator();
//                c.generateCaseStudy("", "grantProposal_" + size + "_" + j, size, null);
//                Config config = new Config();
//                Parser.parseInputFile("grantProposal_" + size + "_" + j + ".abac_txt", config);
//            }
//        }
        
        
        
        
        /* E-WORKFORCE MANAGEMENT */
//        int[] eWorkforceSize = {10};
//        int numPoliciesPerEWorkforceSize = 40;
//        for (int i = 0; i < eWorkforceSize.length; i++){
//            int size = eWorkforceSize[i];
//            for (int j = 14; j < numPoliciesPerEWorkforceSize; j++){
//                System.out.println(j);
//                CaseStudyGenerator c = new EWorkforceCaseStudyGenerator();
//                c.generateCaseStudy("", "eWorkforce_" + size + "_" + j, size, null);
//                Config config = new Config();
//                Parser.parseInputFile("eWorkforce_" + size + "_" + j + ".abac_txt", config);
//            }
//        }
//    }
        
        
        /* HEALTHCARE */
//        int[] healthcareSize = {10};
//        int numPoliciesPerHealthcareSize = 10;
//        for (int i = 0; i < healthcareSize.length; i++){
//            int size = healthcareSize[i];
//            for (int j = 0; j < numPoliciesPerHealthcareSize; j++){
//                CaseStudyGenerator c = new HealthcareCaseStudyGenerator();
//                c.generateCaseStudy("", "healthcare_" + size + "_" + j, size, null);
//                Config config = new Config();
//                Parser.parseInputFile("healthcare_" + size + "_" + j + ".abac_txt", config);
//            }
//        }
        
        
        // UNIVERSITY 
//        int[] sizes = {5,6,7,8,9,10,11,12};
//        int numSize = 10;
//        for (int i = 0; i < sizes.length; i++){
//            int size = sizes[i];
//            for (int j = 0; j < numSize; j++){
//                CaseStudyGenerator c = new UniversityCaseStudyGenerator();
//                c.generateCaseStudy("", "university_" + size + "_" + j, size, null);
//                Config config = new Config();
//                Parser.parseInputFile("university_" + size + "_" + j + ".abac_txt", config);
//            }
//        }
        
        /* PROJECT MANAGEMENT */
//        int[] sizes = {10};
//        int numSize = 30;
//        for (int i = 0; i < sizes.length; i++){
//            int size = sizes[i];
//            for (int j = 0; j < numSize; j++){
//                CaseStudyGenerator c = new ProjectManagementCaseStudyGenerator();
//                c.generateCaseStudy("", "project-management_" + size + "_" + j, size, null);
//                Config config = new Config();
//                Parser.parseInputFile("project-management_" + size + "_" + j + ".abac_txt", config);
//            }
//        }
    	
    	/* E-DOCUMENT */
//    	//Parser.concatFiles("test.abac_txt", "e-document-rule.txt");
//    	//Parser.parseInputFile("e-doc_1_0.abac_txt", config);
//    	int[] sizes = {50,60,70,80,90};
//        int numSize = 10;
//        for (int i = 0; i < sizes.length; i++){
//            int size = sizes[i];
//            for (int j = 0; j < numSize; j++){
//            	System.out.println("This is " + j);
//                CaseStudyGenerator c = new EDocumentCaseStudyGenerator();
//                c.generateCaseStudy("", "e-doc_" + size + "_" + j, size, null);
//                Config config = new Config();
//                Parser.parseInputFile("e-doc_" + size + "_" + j + ".abac_txt", config);
//            }
//        }
//    }
}
