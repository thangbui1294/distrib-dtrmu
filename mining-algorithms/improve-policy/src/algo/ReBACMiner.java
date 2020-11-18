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
 
package algo;

import java.io.BufferedWriter;
import java.io.File;
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
import util.AtomicCondition;
import util.AtomicConditionComparator;
import util.AtomicConstraint;
import util.AttributePathType;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import util.Class1;
import util.ConditionOperator;
import util.Config;
import util.ConstraintOperator;
import util.DiscreteNormalDistribution;
import util.FieldType;
import util.Object1;
import util.Pair;
import util.Parser;
import util.Rule;
import util.RulePairComparator;
import util.RuleQualityComparator;
import util.QualityValue;
import util.Time;
import util.Triple;
import util.UPComparator;

/**
 * This class used to mine an ReBAC policy.
 * @author Thang
 */
public class ReBACMiner {
    
    /**
     * this method used to check if a path has MANY multiplicity
     * @param type class type
     * @param path
     * @param config
     * @return true if the path has MANY multiplicity, false otherwise.
     */
    public static Boolean isManyMultiplicity(String type, ArrayList<String> path, Config config){
        Map<String, Class1> classes = config.getClassModel();
        String currentClassName  = type;
        for (int i = 0; i < path.size(); i++){
            String attr = path.get(i);
            if (attr.equals("id")){
                continue;
            }
            if (classes.get(currentClassName).getAllAttributes().get(attr).getIsBoolean()){
                continue;
            }
            if (classes.get(currentClassName).getAllAttributes().get(attr).getMultiplicity() == FieldType.Multiplicity.MANY){
                return true;
            }
            currentClassName = classes.get(currentClassName).getAllAttributes().get(attr).getType().getClassName();
        }
        return false;
    }

    /**
     * This method return an appropriate constraint operator base of subject and resource paths' multiplicity
     * @param subType type of the subject
     * @param subPath subject path
     * @param resType type of the resource
     * @param resPath resource path
     * @param config
     * @return constraint operator based on the multiplicity
     */
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



    /**
     * This method will return all reachable types form a root type with the shortest path
     * @param type
     * @param config
     * @return set of triple of 1) reachable type name, 2) the distance from the root type, and 3) the path to this type.
     */
    public static Set<Triple<String, Integer, ArrayList<String>>> getAllShortestPaths(String type, Config config){
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
    
    /**
     * this method returns all reachable type from the root type with all paths less than or equal the distance of the shortest path to that type + extra distance
     * @param type
     * @param config
     * @param shortestDistances
     * @param extraDist
     * @return set of triple of 1) reachable type name, 2) the distance from the root type, and 3) the path to this type.
     */
    public static Set<Triple<String, Integer, ArrayList<String>>> getAllPaths(String type, Config config, Map<String, Integer> shortestDistances, int extraDist){
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
    
    /**
     * This method computes set of candidate constraint based on the types of of subject and resource with extra distances for sub paths and res paths
     * @param Tsub
     * @param Tres
     * @param config
     * @param subExtraDist extra distance for sub path
     * @param resExtraDist extra distance for res path
     * @return
     */
    private static Set<AtomicConstraint> candidateConstraintByType(String Tsub, String Tres, Config config, int subExtraDist, int resExtraDist){
        // First find the shortest path to reachable types for sub and
        Set<Triple<String, Integer, ArrayList<String>>> subPaths;
        Set<Triple<String, Integer, ArrayList<String>>> resPaths;
        Set<AtomicConstraint> results = new HashSet<AtomicConstraint>();
        Map<String, Set<Pair<String,String>>> adjacentList = config.getAdjacencyList();
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
                            if (parentSub.equals(parentRes)){
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
                            for (ConstraintOperator co:ops)
                                results.add(new AtomicConstraint(path1.getThird(), path2.getThird(),co));
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
        return results;
    }

    public static Pair<Set<String>, Integer> getConditionMeaning(String type, List<AtomicCondition> condition, Map<String, Object1> objects, Map<String, Class1> classes, Config config){
        // Note that in our current case study, we only have 1-level inheritance.
        int countLoop = 0;
        Set<String> results = new HashSet<String>();
        Map<String, Set<String>> classConditionMeaningMap = config.getConditionMeanings().get(condition);
        if (classConditionMeaningMap != null && classConditionMeaningMap.get(type) != null){
            return new Pair(classConditionMeaningMap.get(type), countLoop);
        }
        else{
            for (int i = 0; i < condition.size(); i++){
                AtomicCondition ac = condition.get(i);
                Map<String, Set<String>> classAtomicConditionMeaningMap = config.getAtomicConditionMeanings().get(ac);
                if (classAtomicConditionMeaningMap != null && classAtomicConditionMeaningMap.get(type) != null){
                    if (i == 0){
                        results.addAll(classAtomicConditionMeaningMap.get(type));
                    }
                    else{
                        results.retainAll(classAtomicConditionMeaningMap.get(type));
                    }
                }
                else{
                    if (classAtomicConditionMeaningMap == null){
                        Map<String, Set<String>> newMap = new HashMap<String, Set<String>>();
                        newMap.put(type, null);
                        config.getAtomicConditionMeanings().put(ac, newMap);
                        classAtomicConditionMeaningMap = config.getAtomicConditionMeanings().get(ac);
                    }
                    else{
                        classAtomicConditionMeaningMap.put(type, null);
                    }
                    Set<String> satisfiedObjects = new HashSet<String>();
                    for (Object1 obj:objects.values()){
                        countLoop++;
                        if (checkSameType(obj, type, classes)){
                            boolean satisfyCheck = checkSatisfyAtomicCondition(obj, ac, objects, classes, config);
                            if (satisfyCheck){
                                satisfiedObjects.add(obj.getId());
                            }
                        }
                    }
                    classAtomicConditionMeaningMap.replace(type, satisfiedObjects);
                    if (i == 0){
                        results.addAll(satisfiedObjects);
                    }
                    else{
                        results.retainAll(satisfiedObjects);
                    }
                }
            }
            if (classConditionMeaningMap == null){
                Map<String, Set<String>> newMap = new HashMap<String, Set<String>>();
                newMap.put(type, results);
                config.getConditionMeanings().put(condition, newMap);
            }
            else{
                classConditionMeaningMap.put(type, results);
            }
            return new Pair(results, countLoop);
        }
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
    
    public static boolean checkSameType(Object1 obj, String type, Map<String, Class1> classes){
        if (classes.get(obj.getClass1()).getParentClass() != null){
            if (!obj.getClass1().equals(type) && !classes.get(obj.getClass1()).getParentClass().getClassName().equals(type)){
                return false;
            }
        }
        else {
            if (!obj.getClass1().equals(type)){
                return false;
            }
        }
        return true;
    }
    
    /**
     * This method check if a pair of subject and resource satisfies set of constraints given the right subject Type and resource Type
     * @param sub
     * @param res
     * @param constraints
     * @param config
     * @return
     */
    public static boolean checkSatisfyConstraints(Object1 sub, Object1 res, ArrayList<AtomicConstraint> constraints, Config config){
        Pair<String, String> p = new Pair(sub.getId(), res.getId());
        Pair<Set<Pair<String, String>>, Set<Pair<String, String>>> constraintObjectLists = config.getConstraintMeanings().get(constraints);
        if (constraintObjectLists!= null && (constraintObjectLists.getFirst().contains(p) || constraintObjectLists.getSecond().contains(p))){
            if (constraintObjectLists.getFirst().contains(p)){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            boolean constraintResult = true;
            for (AtomicConstraint c:constraints){
                // check if we already cache this
                Pair<Set<Pair<String, String>>, Set<Pair<String, String>>> atomicConstraintObjectLists = config.getAtomicConstraintMeanings().get(c);
                if (atomicConstraintObjectLists != null && (atomicConstraintObjectLists.getFirst().contains(p) || atomicConstraintObjectLists.getSecond().contains(p))){
                    if (atomicConstraintObjectLists.getSecond().contains(p)){
                        constraintResult = false;
                        break;
                    }
                }
                else{
                    if (atomicConstraintObjectLists == null){
                        config.getAtomicConstraintMeanings().put(c, new Pair(new HashSet<String>(), new HashSet<String>()));
                        atomicConstraintObjectLists = config.getAtomicConstraintMeanings().get(c);
                    }
                    boolean checkSatisfy = checkSatisfyConstraint(sub, res, c, config);
                    if (!checkSatisfy){
                        constraintResult = false;
                        atomicConstraintObjectLists.getSecond().add(p);
                        break;
                    }
                    else{
                        atomicConstraintObjectLists.getFirst().add(p);
                    }
                }
            }
            if (constraintObjectLists == null){
                config.getConstraintMeanings().put(constraints, new Pair(new HashSet<String>(), new HashSet<String>()));
                constraintObjectLists = config.getConstraintMeanings().get(constraints);
            }
            if (constraintResult){
                constraintObjectLists.getFirst().add(p);
            }
            else{
                constraintObjectLists.getSecond().add(p);
            }
            // update constraint meanings map
            return constraintResult;
        }
    }

    /**
     * This method check if 2 objects (subject and object) satisfy a rule constraint
     * @param subObj subject object
     * @param resObj resource object
     * @param cc atomic constraint to check with
     * @param config
     * @return true if subject and resource satisfy the constraint, false otherwise.
     */
    private static boolean checkSatisfyConstraint(Object1 subObj, Object1 resObj, AtomicConstraint cc, Config config){
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


    /**
     * this method generates set of candidate constraint give subject and resource
     * @param sub
     * @param res
     * @param config
     * @param subExtraDist extra distance for sub path
     * @param resExtraDist extra distance for res path
     * @param totalPathLengthLimit total path limit (maximum subject's path length + resource's path length)
     * @return
     */
    public static ArrayList<AtomicConstraint> candidateConstraint(Object1 sub, Object1 res, Config config, int subExtraDist, int resExtraDist, int totalPathLengthLimit){
        ArrayList<AtomicConstraint> results = new ArrayList<AtomicConstraint>();
        Set<AtomicConstraint> ccs = candidateConstraintByType(sub.getClass1(), res.getClass1(), config, subExtraDist, resExtraDist);
        for (AtomicConstraint cc:ccs){
            boolean checkSatisfy = checkSatisfyConstraint(sub, res, cc, config);
            if (cc.getSubPath().size() + cc.getResPath().size() <= totalPathLengthLimit && checkSatisfy){
                results.add(cc);
            }
        }
        return results;
    }
    
    /**
     * This method returns the type of of the constraint path (not Boolean for now)
     * @param type
     * @param path
     * @param config
     * @return
     */
    public static String getConstraintPathType(String type, ArrayList<String> path, Config config){
        Map<String, Class1> classes = config.getClassModel();
        String currentClassName  = type;
        for (int i = 0; i < path.size(); i++){
            String attr = path.get(i);
            if (attr.equals("id")){
                continue;
            }
            if (classes.get(currentClassName).getAllAttributes().get(attr).getIsBoolean()){
                continue;
            }
            currentClassName = classes.get(currentClassName).getAllAttributes().get(attr).getType().getClassName();
        }
        return currentClassName;
    }
    
    /**
     * This method checks how many extra distance we need to cover all constraint paths in a rule.
     * @param rule
     * @param config
     * @return this method return 2 lists of integer. the first one is list of extra distance for sub of each constraint in the rule, the second one is for res.
     */
    public static Pair<ArrayList<Integer>, ArrayList<Integer>> checkExtraDistanceForConstraint(Rule rule, Config config){
        String Tsub = rule.getSubjectType().getClassName();
        String Tres = rule.getResourceType().getClassName();
        Map<String, Integer> subDistances = new HashMap<String, Integer>();
        Map<String, Integer> resDistances = new HashMap<String, Integer>();
        Set<Triple<String, Integer, ArrayList<String>>> subPaths = getAllShortestPaths(Tsub,config);
        Set<Triple<String, Integer, ArrayList<String>>> resPaths = getAllShortestPaths(Tres,config);
        for (Triple<String, Integer, ArrayList<String>> e:subPaths){
            if ((subDistances.get(e.getFirst()) != null && e.getSecond() > subDistances.get(e.getFirst())) || !subDistances.containsKey(e.getFirst())){
                subDistances.put(e.getFirst(), e.getSecond());
            }
        }
        for (Triple<String, Integer, ArrayList<String>> e:resPaths){
            if ((resDistances.get(e.getFirst()) != null && e.getSecond() > resDistances.get(e.getFirst())) || !resDistances.containsKey(e.getFirst())){
                resDistances.put(e.getFirst(), e.getSecond());
            }
        }
        ArrayList<Integer> subResults = new ArrayList<Integer>();
        ArrayList<Integer> resResults = new ArrayList<Integer>();
        for (AtomicConstraint ac:rule.getConstraint()){
            String subPathType = getConstraintPathType(Tsub, ac.getSubPath(), config);
            subResults.add(ac.getSubPath().size() - subDistances.get(subPathType));
            String resPathType = getConstraintPathType(Tres, ac.getResPath(), config);
            resResults.add(ac.getResPath().size() - resDistances.get(resPathType));
        }
        return new Pair(subResults, resResults);
    }
    
    /**
     * This method will print all extra distance information for each rule in the config and the summary.
     * @param config
     */
    public static void getExtraDistanceForConstraintInformation(Config config){
        int maxSubAll = 0;
        int maxResAll = 0;
        int[] subExtraDistances = new int[100];
        int[] resExtraDistances = new int[100];
        for (Rule r:config.getUPRelation().keySet()){
            if (!r.getConstraint().isEmpty()){
                System.out.println("============\nRule: " + r);
                Pair<ArrayList<Integer>, ArrayList<Integer>> results = ReBACMiner.checkExtraDistanceForConstraint(r, config);
                System.out.println("Extra distances for subject: " + results.getFirst());
                System.out.println("Extra distances for resource: " + results.getSecond());
                int maxSub = Collections.max(results.getFirst());
                if (maxSub > maxSubAll){
                    maxSubAll = maxSub;
                }
                int maxRes = Collections.max(results.getSecond());
                if (maxRes > maxResAll){
                    maxResAll = maxRes;
                }
                for (int i:results.getFirst()){
                    if (i > 0){
                        subExtraDistances[i]++;
                    }
                }
                for (int i:results.getSecond()){
                    if (i > 0){
                        resExtraDistances[i]++;
                    }
                }
            }
            
        }
        System.out.println("==============================\nSUMMARY");
        System.out.println("Max distance for sub: " + maxSubAll);
        System.out.println("Max distance for res: " + maxResAll);
        System.out.println("Number of sub requires extra distances:");
        for (int i = 1 ; i <= maxSubAll; i++){
            System.out.println ("--Extra "+ i + " distance: " + subExtraDistances[i]);
        }
        System.out.println("Number of res requires extra distances:");
        for (int i = 1 ; i <= maxResAll; i++){
            System.out.println ("--Extra "+ i + " distance: " + resExtraDistances[i]);
        }
    }
    
    
    /**
     * This method return list of attribute paths of a type with specific path length limit. path length limit must be > 0
     * @param type
     * @param config
     * @param pathLimit
     * @return
     */
    public static ArrayList<ArrayList<String>> getAllAttributePaths(String type, Config config, int pathLimit){
        Map<String, Class1> classModel = config.getClassModel();
        ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
        // add all attributes of this current object.
        // id attribute
        ArrayList<String> idAttr = new ArrayList<String>();
        idAttr.add("id");
        results.add(idAttr);
        // boolean attributes
        for (String fieldName:classModel.get(type).getAllAttributes().keySet()){
            if (classModel.get(type).getAllAttributes().get(fieldName).getIsBoolean()){
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
                    for (String fieldName:classModel.get(neighbor.getSecond()).getAllAttributes().keySet()){
                        if (classModel.get(neighbor.getSecond()).getAllAttributes().get(fieldName).getIsBoolean()){
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
    
    /**
     * This method used to test candidate constraint method
     * @param config
     */
    public static void testCandidateConstraintMethod(Config config){
        Map<String, Set<Pair<String,String>>> adjacentList = config.getAdjacencyList();
        
        System.out.println(adjacentList);
        System.out.println();
        System.out.println("===================================================================================================\nTEST candidateConstraints() method");
        
        //ReBACMiner.getExtraDistanceForConstraintInformation(config);
        double totalAvg = 0;
        int numUnusedConstraintsCovered = 0;
        int totalUnusedConstraints = 0;
        for (Rule r:config.getUPRelation().keySet()){
            Map<AtomicConstraint, Integer> unusedConstraints = new HashMap<AtomicConstraint, Integer>();
            int avgUnusedConstraints = 0;
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\nUsing rule: " + r);
            Set<Triple<String, String, String>> upSet= config.getUPRelation().get(r);
            for (Triple<String, String, String> up:upSet){
                ArrayList<AtomicConstraint> ccs = ReBACMiner.candidateConstraint(config.getObjectModel().get(up.getFirst()), config.getObjectModel().get(up.getSecond()), config, 0 , 2, 7);
                if (!ccs.containsAll(r.getConstraint())){
                    System.out.println("REQUIRED CONSTRAINTS ARE NOT GENERATED");
                    System.exit(0);
                }
                avgUnusedConstraints += ccs.size() - r.getConstraint().size();
                for (AtomicConstraint cc:ccs){
                    if (!r.getConstraint().contains(cc)){
                        // add this constraint to unused contraints maps for this rule
                        if (unusedConstraints.containsKey(cc)){
                            unusedConstraints.put(cc, unusedConstraints.get(cc) + 1);
                        }
                        else {
                            unusedConstraints.put(cc, 1);
                        }
                    }
                }
            }
//            System.out.println("Average unused constraints for this rule: " + (double)avgUnusedConstraints/upSet.size());
//            System.out.println("List of unused constraints: ");
            if (!upSet.isEmpty()){
                totalAvg += (double)avgUnusedConstraints/upSet.size();
            }
            
            for (AtomicConstraint ac:unusedConstraints.keySet()){
                System.out.println(ac + "\n");
                System.out.println("Fraction of number of UP for this rule that satisfy this constraint " + unusedConstraints.get(ac) + "/" + config.getUPRelation().get(r).size());
                if (unusedConstraints.get(ac) == config.getUPRelation().get(r).size()){
                    numUnusedConstraintsCovered++;
                }
            }
            totalUnusedConstraints += unusedConstraints.keySet().size();
            
// Uncomment to print out all candidate constraint testing for specific number of UP relations for each rule
            Iterator<Triple<String, String, String>> upSetIter = upSet.iterator();
//            for (int i = 0 ; i < 1; i++){
//                Triple<String, String, String> up = null;
//                if (upSetIter.hasNext()){
//                    up = upSetIter.next();
//                }
//                if (up != null){
//                    System.out.println("***********************\nUP" + i + ": " + up);
//                    Set<AtomicConstraint> ccs = ReBACMiner.candidateConstraint(config.getObjectModel().get(up.getFirst()), config.getObjectModel().get(up.getSecond()), config, 0 , 3);
//                    for (AtomicConstraint cc:ccs){
//                        //System.out.println(cc);
//                    }
//                }
//            }
        }
        //     System.out.println("Total average of unused constraints: " + totalAvg/config.getRuleModel().size());
        //     System.out.println("Number of unused constraints which are satisfied by all UPs that satisfy the constraints of the rules: " + numUnusedConstraintsCovered);
        //     System.out.println("Total number of unused constraints: " + totalUnusedConstraints);
        
    }
    
    /**
     * This method compute an entropy
     * @param countPos
     * @param countNeg
     * @return
     */
    public static double getEntropy(int countPos, int countNeg){
        if (countPos == 0 || countNeg == 0){
            return 0;
        }
        int sum = countPos + countNeg;
        double posProb = (double)countPos/sum;
        double negProb = (double)countNeg/sum;
        return - posProb * (Math.log(posProb)/Math.log(2)) - negProb * (Math.log(negProb)/Math.log(2));
    } 
    
    /**
     * This method generates set of UP tuples satisfy a specific rule r
     * @param config configuration contains object model and rule model
     * @param r rule to check
     * @return list of UP triples.
     */
    public static ArrayList<Triple<String, String, String>> computeCoveredUP(Rule r, Config config){
        Map<String, Object1> objects  = config.getObjectModel();
        Map<String, Class1> classes = config.getClassModel();
        ArrayList<Triple<String, String, String>> upRelations = new ArrayList<Triple<String, String, String>>();
        Collections.sort(r.getConstraint());
        Collections.sort(r.getResourceCondition());
        Collections.sort(r.getSubjectCondition());
        // free up some space in the meaning maps, in case there is memory overhead error
        // remove 20% number of the entries in condition meaning and constraint meaning maps when the memory size is less than 2GB
        Runtime rt = Runtime.getRuntime();
        long mem1= rt.freeMemory();
        if (mem1 < 2000000000){
            System.out.println("FREE UP SPACE!!!!!!!!!!!!");
            int nEntriesConditionsMeaning = config.getConditionMeanings().size();
            int numConditionRemovedEntries = (int)(nEntriesConditionsMeaning * 0.2);
            Iterator<List<AtomicCondition>> condIterator = config.getConditionMeanings().keySet().iterator();
            while (condIterator.hasNext() && numConditionRemovedEntries > 0){
                condIterator.next();
                condIterator.remove();
                numConditionRemovedEntries--;
            }
            
            int nEntriesconstraintMeaning = config.getConstraintMeanings().size();
            int numConstraintRemovedEntries = (int)(nEntriesconstraintMeaning * 0.2);
            Iterator<List<AtomicConstraint>> consIterator = config.getConstraintMeanings().keySet().iterator();
            while (consIterator.hasNext() && numConstraintRemovedEntries > 0){
                consIterator.next();
                consIterator.remove();
                numConstraintRemovedEntries--;
            }
            // manually called garbaage collector
            rt.gc();
        }
        
        Set<String> satisfiedSubObjects = new HashSet<String>();
        Set<String> satisfiedResObjects = new HashSet<String>();
        
        // for subject
        if (r.getSubjectCondition().isEmpty()){
            for (Object1 obj:objects.values()){
                if (checkSameType(obj, r.getSubjectType().getClassName(), classes)){
                    satisfiedSubObjects.add(obj.getId());
                }
            }
        }
        else{
            Pair<Set<String>, Integer> result = getConditionMeaning(r.getSubjectType().getClassName(), r.getSubjectCondition(), objects, classes, config);
            satisfiedSubObjects = result.getFirst();
        }
        
        // for resource
        if (r.getResourceCondition().isEmpty()){
            for (Object1 obj:objects.values()){
                if (checkSameType(obj, r.getResourceType().getClassName(), classes)){
                    satisfiedResObjects.add(obj.getId());
                }
            }
        }
        else{
            Pair<Set<String>, Integer> result = getConditionMeaning(r.getResourceType().getClassName(), r.getResourceCondition(), objects, classes, config);
            satisfiedResObjects = result.getFirst();
        }
        
        for (String sub:satisfiedSubObjects){
            for (String res:satisfiedResObjects){
                if (checkSatisfyConstraints(config.getObjectModel().get(sub), config.getObjectModel().get(res), r.getConstraint(), config)){
                    for (String action:r.getActions()){
                        upRelations.add(new Triple(sub, res, action));
                    }
                }
            }
        }
        //System.out.println("SIZE OF CONSTRAINT MEANING MAPS: " + config.getAtomicConstraintMeanings().size() + ", " + config.getConstraintMeanings().size());
        
        return upRelations;
    }
    
    /**
     * This method compute the rule quality specified in ABAC Miner paper.
     * @param r
     * @param uncovUP
     * @param config
     * @return
     */
    public static QualityValue computeRuleQuality(Rule r, ArrayList<Triple<String, String, String>> uncovUP, Config config){
        QualityValue result = new QualityValue();
        r.setCoveredUP(computeCoveredUP(r, config));
        int numCoveredTuples = 0;
        for (Triple<String, String, String> tuple:r.getCoveredUP()){
            if (uncovUP.contains(tuple)){
                numCoveredTuples++;
            }
        }
        result.firstComponent = ((double) numCoveredTuples) / r.getWSC();
        result.secondComponent = r.getConstraint().size();
        int constraintsPathLength = 0;
        for (AtomicConstraint ac:r.getConstraint()){
            constraintsPathLength += ac.getSubPath().size();
            constraintsPathLength += ac.getResPath().size();
        }
        result.thirdComponent = (1 / (double)constraintsPathLength);
        return result;
    }
    
   /* /**
     * This method return the highest-quality generalization of a rule r
     * @param r
     * @param candidateConstraints
     * @param uncovUP
     * @param config
     * @param currentLevel
     * @param maxLevel
     * @param isOneConditionPerPathRestricted
     * @param limitConstraintSizeAll
     * @param limitConstraintSizeHalf
     * @param removeConditionThreshold
     * @return
     */
    /*
    public static Rule generalizeRule(Rule r, ArrayList<AtomicConstraint> candidateConstraints, ArrayList<Triple<String, String, String>> uncovUP, Config config, int currentLevel, int maxLevel, boolean isOneConditionPerPathRestricted, int limitConstraintSizeAll, int limitConstraintSizeHalf, int removeConditionThreshold, double alpha){
        if(currentLevel > maxLevel){
            return r;
        }
        if (!isValidRule(r, config, config.getUnderassignmentDetection(),alpha)){
            System.out.println(r);
            System.out.println("ERROR: INVALID RULE IS NOT REMOVED");
            System.exit(0);
        }
        if (r.getQuality() == null){
            r.setQuality(computeRuleQuality(r, uncovUP, config));
        }
        // bestRule is the highest-quality generalization of r
        Rule bestRule = r;
        QualityValue bestQuality = bestRule.getQuality();
        // cc contains formulas from constraints that lead to valid generalizations of r
        ArrayList<Pair<AtomicConstraint, Rule>> cc = new ArrayList<Pair<AtomicConstraint, Rule>>();
        
        // find formulas in cc that lead to valid generalaizations of r
        for (int i = 0; i < candidateConstraints.size(); i++) {
            AtomicConstraint f = candidateConstraints.get(i);
            Rule r1 = new Rule(r);
            ArrayList<String> subPath = f.getSubPath();
            ArrayList<String> resPath = f.getResPath();
            // try to generalize r by adding candidateConstraints[i] and eliminating both cojuncts in sub and res
            ArrayList<AtomicCondition> removeConditions = new ArrayList<AtomicCondition>();
            boolean isRemoved = false;
            for (AtomicCondition ac:r1.getSubjectCondition()) {
                if (subPath.equals(ac.getPath())) {
                    removeConditions.add(ac);
                    if (isOneConditionPerPathRestricted){
                        break;
                    }
                }
            }
            if (!removeConditions.isEmpty()){
                isRemoved = true;
            }
            r1.getSubjectCondition().removeAll(removeConditions);
            removeConditions.clear();
            for (AtomicCondition ac:r1.getResourceCondition()) {
                if (resPath.equals(ac.getPath())) {
                    removeConditions.add(ac);
                    if (isOneConditionPerPathRestricted){
                        break;
                    }
                }
            }
            if (!removeConditions.isEmpty()){
                isRemoved = true;
            }
            r1.getResourceCondition().removeAll(removeConditions);
            removeConditions.clear();
            
            if (!isRemoved){
                continue;
            }
            
            r1.getConstraint().add(f);
            //if (isValidRule(r1, config)) {
            if (isBetterRule(r1, config, config.getUnderassignmentDetection(),alpha)) {
                // the rule is valid
                r1.setQuality(computeRuleQuality(r1, uncovUP, config));
                cc.add(new Pair(f, r1));
            } else {
                // try to generalize r by adding constraints[i] and eliminating
                // one relevant conjunct
                isRemoved = false;
                Rule r2 = new Rule(r);
                ArrayList<String> subPath2 = f.getSubPath();
                for (AtomicCondition ac:r2.getSubjectCondition()) {
                    if (subPath2.equals(ac.getPath())) {
                        removeConditions.add(ac);
                        if (isOneConditionPerPathRestricted){
                            break;
                        }
                    }
                }
                
                if (!removeConditions.isEmpty()){
                    isRemoved = true;
                }
                r2.getSubjectCondition().removeAll(removeConditions);
                removeConditions.clear();
                
                r2.getConstraint().add(f);
                
                //if (isValidRule(r2, config) && isRemoved) {
                if (isBetterRule(r2, config, config.getUnderassignmentDetection(),alpha) && isRemoved) {
                    r2.setQuality(computeRuleQuality(r2, uncovUP, config));
                    cc.add(new Pair(f,r2));
                } else {
                    Rule r3 = new Rule(r);
                    isRemoved = false;
                    ArrayList<String> resPath3 = f.getResPath();
                    for (AtomicCondition ac:r3.getResourceCondition()) {
                        if (resPath3.equals(ac.getPath())) {
                            removeConditions.add(ac);
                            if (isOneConditionPerPathRestricted){
                                break;
                            }
                        }
                    }
                    if (!removeConditions.isEmpty()){
                        isRemoved = true;
                    }
                    r3.getResourceCondition().removeAll(removeConditions);
                    removeConditions.clear();
                    
                    r3.getConstraint().add(f);
                    
                    //if (isValidRule(r3, config) && isRemoved) {
                    if (isBetterRule(r3, config, config.getUnderassignmentDetection(),alpha) && isRemoved) {
                        r3.setQuality(computeRuleQuality(r3, uncovUP, config));
                        cc.add(new Pair(f, r3));
                    }
                }
            }
        }
        
        Collections.sort(cc, new AtomicConstraintComparator(uncovUP, config));
        if (cc.size() <= limitConstraintSizeAll){
            // Check with L1 limit passed
            for (int i = 0; i < cc.size(); i++) {
                ArrayList<AtomicConstraint> newCC = new ArrayList<AtomicConstraint>();
                for (int j = 0; j < cc.size(); j++){
                    if (i != j){
                        newCC.add(cc.get(j).getFirst());
                    }
                }
                Rule tempR = generalizeRule(cc.get(i).getSecond(), newCC, uncovUP, config, currentLevel + 1, maxLevel,isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
                if (tempR.getQuality().compareTo(bestQuality) > 0) {
                    bestRule = tempR;
                    bestQuality = bestRule.getQuality();
                }
            }
            return bestRule;
            
        }
        else{
            // size of candidate constraints exceeds the considering all subset threshod, now we need to trim the set if it also exceeds L2
            if (cc.size() > limitConstraintSizeHalf){
                int k = cc.size();
                cc.subList(limitConstraintSizeHalf, k).clear();
            }
            for (int i = 0; i < cc.size(); i++) {
                // try to further generalize gen[i]
                ArrayList<AtomicConstraint> newCC = new ArrayList<AtomicConstraint>();
                for (Pair<AtomicConstraint, Rule> ccPair:cc.subList(i + 1, cc.size())){
                    newCC.add(ccPair.getFirst());
                }
                
                Rule tempR = generalizeRule(cc.get(i).getSecond(), newCC, uncovUP, config, currentLevel + 1, maxLevel,isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
                if (tempR.getQuality().compareTo(bestQuality) > 0) {
                    bestRule = tempR;
                    bestQuality = bestRule.getQuality();
                }
            }
            return bestRule;
        }
    } */
    public static int computeConstraintOperatorsIndex(Rule r){
        int totalOpIndex = 0;
        for (AtomicConstraint ac:r.getConstraint()){
            if (ac.getConstraintOperator() == ConstraintOperator.IN){
                totalOpIndex += 3;
            }
            else if (ac.getConstraintOperator() == ConstraintOperator.EQUALS_VALUE){
                totalOpIndex +=2;
            }
            else if (ac.getConstraintOperator() == ConstraintOperator.CONTAINS){
                totalOpIndex +=1;
            }
        }
        return totalOpIndex;
    }
    
    /**
     * This method merge similar rules, but different in subject type or resource type because of inheritance
     * @param rules
     * @param config
     */
    public static int mergeSameRules(LinkedList<Rule> rules, Config config, double alpha){
        int numMerges = 0;
        Map<String, Set<String>> parentClasses = new HashMap<String, Set<String>>();
        Map<String, Class1> classes = config.getClassModel();
        // get all parent classes from class model
        for (String className:classes.keySet()){
            if (classes.get(className).getParentClass() != null){
                // add the parent class
                String parentClassName = classes.get(className).getParentClass().getClassName();
                if (parentClasses.get(parentClassName) == null){
                    Set<String> childClasses = new HashSet<String>();
                    childClasses.add(className);
                    parentClasses.put(parentClassName, childClasses);
                }
                else{
                    Set<String> newChildClasses = new HashSet<String>(parentClasses.get(parentClassName));
                    newChildClasses.add(className);
                    parentClasses.put(parentClassName, newChildClasses);
                }
            }
        }
        ArrayList<Rule> newMergeRules = new ArrayList<Rule>();
        ArrayList<Rule> removeRules = new ArrayList<Rule>();
        for (int i = 0; i < rules.size(); i++){
            Rule r1 = rules.get(i);
            if (removeRules.contains(r1)){
                continue;
            }
            ArrayList<Rule> sameRules = new ArrayList<Rule>();
            sameRules.add(r1);
            for (int j = i + 1; j < rules.size(); j++){
                Rule r2 = rules.get(j);
                if (removeRules.contains(r2)){
                    continue;
                }
                if (r1.getSubjectCondition().containsAll(r2.getSubjectCondition()) && r1.getResourceCondition().containsAll(r2.getResourceCondition()) && r1.getConstraint().containsAll(r2.getConstraint())
                        && r2.getSubjectCondition().containsAll(r1.getSubjectCondition()) && r2.getResourceCondition().containsAll(r1.getResourceCondition()) && r2.getConstraint().containsAll(r1.getConstraint())
                        && r1.getActions().containsAll(r2.getActions()) && r2.getActions().containsAll(r1.getActions())){
                    sameRules.add(r2);
                }
            }
            if (sameRules.size() > 1){
                // check if we can merge these same rules
                Set<String> subTypes = new HashSet<String>();
                Set<String> resTypes = new HashSet<String>();
                for (Rule r:sameRules){
                    subTypes.add(r.getSubjectType().getClassName());
                    resTypes.add(r.getResourceType().getClassName());
                }
                String newPossibleSubType = null;
                String newPossibleResType = null;
                if (subTypes.size() > 1){
                    for (String parent:parentClasses.keySet()){
                        int countSubType = 0;
                        for (String subType : subTypes){
                            if (parentClasses.get(parent).contains(subType)){
                                countSubType++;
                                if (countSubType > 1){
                                    break;
                                }
                            }
                        }
                        if (countSubType > 1){
                            newPossibleSubType = parent;
                        }
                    }
                }
                if (resTypes.size() > 1){
                    for (String parent:parentClasses.keySet()){
                        int countResType = 0;
                        for (String resType : resTypes){
                            if (parentClasses.get(parent).contains(resType)){
                                countResType++;
                                if (countResType > 1){
                                    break;
                                }
                            }
                        }
                        if (countResType > 1){
                            newPossibleResType = parent;
                        }
                    }
                }
                
                // check if the paths in subject condition, resource condition and constraints is valid
                boolean validSubPaths = true;
                boolean validResPaths = true;
                // subject condition
                if (newPossibleSubType != null){
                    for (AtomicCondition sc: r1.getSubjectCondition()){
                        if (!Parser.checkValidPath(newPossibleSubType, sc.getPath(), config.getClassModel()).getFirst()){
                            validSubPaths = false;
                            break;
                        }
                    }
                    // constraint
                    for (AtomicConstraint ac: r1.getConstraint()){
                        if (!Parser.checkValidPath(newPossibleSubType, ac.getSubPath(), config.getClassModel()).getFirst()){
                            validSubPaths = false;
                            break;
                        }
                    }
                }
                // resource condition
                if (newPossibleResType != null){
                    for (AtomicCondition rc: r1.getResourceCondition()){
                        if (!Parser.checkValidPath(newPossibleResType, rc.getPath(), config.getClassModel()).getFirst()){
                            validResPaths = false;
                            break;
                        }
                    }
                    // constraint
                    for (AtomicConstraint ac: r1.getConstraint()){
                        if (!Parser.checkValidPath(newPossibleResType, ac.getResPath(), config.getClassModel()).getFirst()){
                            validResPaths = false;
                            break;
                        }
                    }
                }
                
                if (newPossibleSubType != null && validSubPaths && newPossibleResType != null && validResPaths){
                    numMerges++;
                    // merge rules with new subType and resType
                    Rule mergeRule = new Rule(classes.get(newPossibleSubType), r1.getSubjectCondition(), classes.get(newPossibleResType), r1.getResourceCondition(), r1.getConstraint(), r1.getActions());
                    mergeRule.setQuality(computeRuleQuality(mergeRule, config.getUPList(), config));
                    if (isValidRule(mergeRule, config, config.getUnderassignmentDetection(), alpha)){
                        newMergeRules.add(mergeRule);
                        for (Rule rule:sameRules){
                            if (rule.getSubjectType().getParentClass() != null && rule.getSubjectType().getParentClass().getClassName().equals(newPossibleSubType) && rule.getResourceType().getParentClass() != null && rule.getResourceType().getParentClass().getClassName().equals(newPossibleResType)){
                                removeRules.add(rule);
                            }
                        }
                    }
                }
                if (newPossibleSubType != null && validSubPaths){
                    numMerges++;
                    // merge rules with new subType only
                    Rule mergeRule = new Rule(classes.get(newPossibleSubType), r1.getSubjectCondition(), r1.getResourceType(), r1.getResourceCondition(), r1.getConstraint(), r1.getActions());
                    mergeRule.setQuality(computeRuleQuality(mergeRule, config.getUPList(), config));
                    if (isValidRule(mergeRule, config, config.getUnderassignmentDetection(), alpha)){
                        newMergeRules.add(mergeRule);
                        for (Rule rule:sameRules){
                            if (rule.getSubjectType().getParentClass() != null && rule.getSubjectType().getParentClass().getClassName().equals(newPossibleSubType) && rule.getResourceType().getClassName().equals(r1.getResourceType().getClassName())){
                                removeRules.add(rule);
                            }
                        }
                    }
                }
                if (newPossibleResType != null && validResPaths){
                    numMerges++;
                    // merge rules with new subType and resType
                    Rule mergeRule = new Rule(r1.getSubjectType(), r1.getSubjectCondition(), classes.get(newPossibleResType), r1.getResourceCondition(), r1.getConstraint(), r1.getActions());
                    mergeRule.setQuality(computeRuleQuality(mergeRule, config.getUPList(), config));newMergeRules.add(mergeRule);
                    if (isValidRule(mergeRule, config, config.getUnderassignmentDetection(), alpha)){
                        for (Rule rule:sameRules){
                            if (rule.getResourceType().getParentClass() != null && rule.getResourceType().getParentClass().getClassName().equals(newPossibleResType) && rule.getSubjectType().getClassName().equals(r1.getSubjectType().getClassName())){
                                removeRules.add(rule);
                            }
                        }
                    }
                }
            }
        }
        rules.removeAll(removeRules);
        rules.addAll(newMergeRules);
        return numMerges;
    }
    
    
    public static int removeRedundantRules(LinkedList<Rule> rules, Config config){
        // remove redundant rules
        Set<Rule> redundantRules = new HashSet<Rule>();
        for (Rule r1:rules){
            if (redundantRules.contains(r1)){
                continue;
            }
            for (Rule r2:rules){
                if (r1 == r2 || redundantRules.contains(r2)){
                    continue;
                }
                if (r2.getCoveredUP().containsAll(r1.getCoveredUP()) && r1.getCoveredUP().containsAll(r2.getCoveredUP()) ){
                    if (r1.getWSC() > r2.getWSC()){
                        redundantRules.add(r1);
                        break;
                    }
                    else if (computeConstraintOperatorsIndex(r1) < computeConstraintOperatorsIndex(r2)){
                        redundantRules.add(r1);
                    }
                    else{
                        redundantRules.add(r2);
                    }
                }
                else if (r2.getCoveredUP().containsAll(r1.getCoveredUP())){
                    redundantRules.add(r1);
                    break;
                }
                else if (r1.getCoveredUP().containsAll(r2.getCoveredUP())){
                    redundantRules.add(r2);
                }
            }
        }
        rules.removeAll(redundantRules);
        return redundantRules.size();
    }
    
    /**
     * This method merges a set of rules
     * @param rules
     * @param config
     * @return
     */
    public static boolean mergeRules(LinkedList<Rule> rules, Config config, int[] stats, double alpha){
        boolean merged = false;
        stats[0]++;
        // remove redundant rules
        /*Set<Rule> redundantRules = new HashSet<Rule>();
        for (Rule r1:rules){
        if (redundantRules.contains(r1)){
        continue;
        }
        for (Rule r2:rules){
        if (r1 == r2 || redundantRules.contains(r2)){
        continue;
        }
        if (r2.getCoveredUP().containsAll(r1.getCoveredUP()) && r1.getCoveredUP().containsAll(r2.getCoveredUP()) ){
        if (r1.getWSC() > r2.getWSC()){
        redundantRules.add(r1);
        break;
        }
        else{
        redundantRules.add(r2);
        }
        }
        else if (r2.getCoveredUP().containsAll(r1.getCoveredUP())){
        redundantRules.add(r1);
        break;
        }
        else if (r1.getCoveredUP().containsAll(r2.getCoveredUP())){
        redundantRules.add(r2);
        }
        }
        }
        rules.removeAll(redundantRules); */
//        System.out.println("START TESTING IN MERGE RULE FUNCTION 1");
//        for (Rule r:rules){
//            boolean firstC = false;
//            boolean secondC = false;
//            for (AtomicConstraint ac:r.getConstraint()){
//                if (ac.getSubPath().get(0).equals("affiliation")){
//                    if (ac.getResPath().contains("affiliation") && ac.getResPath().contains("physician")){
//                        firstC = true;
//                    }
//                    if (ac.getResPath().contains("patient") && ac.getResPath().contains("registrations")){
//                        secondC = true;
//                    }
//                }
//            }
//            if (firstC && secondC){
//                System.out.println(r);
//            }
//        }
//        System.out.println("DONE TESTING IN MERGE RULE FUNCTION 1");
// Merge rules
// Construct workset
        ArrayList<Pair<Rule, Rule>> workSet = new ArrayList<Pair<Rule, Rule>>();
        for (int i = 0; i < rules.size(); i++) {
            Rule r1 = rules.get(i);
            for (int j = i + 1; j < rules.size(); j++) {
                Rule r2 = rules.get(j);
                if (r1.getSubjectType().getClassName().equals(r2.getSubjectType().getClassName()) && r1.getResourceType().getClassName().equals(r2.getResourceType().getClassName()) && (r1.getConstraint().containsAll(r2.getConstraint())) && (r2.getConstraint().containsAll(r1.getConstraint())) ) {
                    workSet.add(new Pair<Rule, Rule>(r1, r2));
                }
            }
        }
//        for (Pair<Rule, Rule> pair:workSet){
//            // test chosen pair
//            QualityValue maxPair;
//            QualityValue minPair;
//            if (pair.getFirst().getQuality().compareTo(pair.getSecond().getQuality()) >= 0){
//                maxPair = pair.getFirst().getQuality();
//                minPair = pair.getSecond().getQuality();
//            }
//            else{
//                maxPair = pair.getSecond().getQuality();
//                minPair = pair.getFirst().getQuality();
//            }
//
//            System.out.println("<" + maxPair.firstComponent + ", " + minPair.firstComponent + ">");
//        }
        Collections.sort(workSet, new RulePairComparator());
        while (!workSet.isEmpty()) {
            ArrayList<Rule> removedRules = new ArrayList<Rule>();
            Rule r1 = workSet.get(0).getFirst();
            Rule r2 = workSet.get(0).getSecond();
            workSet.remove(0);
            
            Rule temp2 = r2;
            Rule temp = new Rule(r1);
            for (int n = 0; n < temp.getSubjectCondition().size(); n++) {
                AtomicCondition c1 = temp.getSubjectCondition().get(n);
                boolean found = false;
                for (int m = 0; m < temp2.getSubjectCondition().size(); m++) {
                    AtomicCondition c2 = temp2.getSubjectCondition().get(m);
                    if (c1.equals(c2)){
                        found = true;
                        break;
                    }
                    if (c2.getPath().equals(c1.getPath()) && !c1.getIsNegative() && !c2.getIsNegative()) {
                        // if the pair share a same subject condition path, union the constant.
                        // Note that the current implementation is enforcing the restriction
                        // that a rule can only have 1 conjunct of a path. Therefore, we only
                        // union them if the path has ONE multiplicity
                        if (c2.getConditionOperator() == ConditionOperator.IN){
                            c1.getConstant().addAll(c2.getConstant());
                            found = true;
                            break;
                        }
                        else{
                            // path has multiplicity MANY
                            // the operation is on set, but for condition with MANY multiplicity path, constant has at most 1 element
                            if (c1.getConstant().equals(c2.getConstant())){
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (found == false) {
                    temp.getSubjectCondition().remove(n);
                    n--;
                }
            }
            for (int n = 0; n < temp.getResourceCondition().size(); n++) {
                AtomicCondition c1 = temp.getResourceCondition().get(n);
                boolean found = false;
                for (int m = 0; m < temp2.getResourceCondition().size(); m++) {
                    AtomicCondition c2 = temp2.getResourceCondition().get(m);
                    if (c1.equals(c2)){
                        found = true;
                        break;
                    }
                    if (c2.getPath().equals(c1.getPath()) && !c1.getIsNegative() && !c2.getIsNegative()) {
                        // if the pair share a same subject condition path, union the constant.
                        // Note that the current implementation is enforcing the restriction
                        // that a rule can only have 1 conjunct of a path. Therefore, we only
                        // union them if the path has ONE multiplicity
                        if (c2.getConditionOperator() == ConditionOperator.IN){
                            c1.getConstant().addAll(c2.getConstant());
                            found = true;
                            break;
                        }
                        else{
                            // path has multiplicity MANY
                            // the operation is on set, but for condition with MANY multiplicity path, constant has at most 1 element
                            if (c1.getConstant().equals(c2.getConstant())){
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (found == false) {
                    temp.getResourceCondition().remove(n);
                    n--;
                }
            }
            temp.getActions().addAll(temp2.getActions());
            if (isValidRule(temp, config, config.getUnderassignmentDetection(), alpha)) {
                temp.setQuality(computeRuleQuality(temp, config.getUPList(), config));
                merged = true;
                stats[1]++;
                removedRules.add(r1);
                removedRules.add(r2);
                Set<Triple<String, String, String>> mergedRulesCovered = new HashSet<Triple<String, String, String>>();
                mergedRulesCovered.addAll(r1.getCoveredUP());
                mergedRulesCovered.addAll(r2.getCoveredUP());
//                for (Rule r:rules) {
//                    if (r.equals(r1) || r.equals(r2)) {
//                        continue;
//                    }
// Line 10: remove redundant rules
//if (temp.getCoveredUP().containsAll(r.getCoveredUP())){
//removedRules.add(r);
//}
//}
                
//Line 12:
                ArrayList<Pair<Rule, Rule>> removedPairs = new ArrayList<Pair<Rule, Rule>>();
                for (Pair<Rule, Rule> rulePair:workSet){
                    if (removedRules.contains(rulePair.getFirst()) || removedRules.contains(rulePair.getSecond())){
                        removedPairs.add(rulePair);
                    }
                }
                workSet.removeAll(removedPairs);
                rules.removeAll(removedRules);
                for (Rule r:rules){
                    // Line 13:
                    if (r.getSubjectType().getClassName().equals(temp.getSubjectType().getClassName()) && r.getResourceType().getClassName().equals(temp.getResourceType().getClassName()) && !removedRules.contains(r) && (r.getConstraint().containsAll(temp.getConstraint())) && (temp.getConstraint().containsAll(r.getConstraint()))) {
                        workSet.add(new Pair<Rule, Rule>(temp, r));
                    }
                }
                Collections.sort(workSet, new RulePairComparator());
                rules.add(temp);
            }
        }
//System.out.println("START TESTING IN MERGE RULE FUNCTION 2");
//        for (Rule r:rules){
//            boolean firstC = false;
//            boolean secondC = false;
//            for (AtomicConstraint ac:r.getConstraint()){
//                if (ac.getSubPath().get(0).equals("affiliation")){
//                    if (ac.getResPath().contains("affiliation") && ac.getResPath().contains("physician")){
//                        firstC = true;
//                    }
//                    if (ac.getResPath().contains("patient") && ac.getResPath().contains("registrations")){
//                        secondC = true;
//                    }
//                }
//            }
//            if (firstC && secondC){
//                System.out.println(r);
//            }
//        }
//System.out.println("DONE TESTING IN MERGE RULE FUNCTION 2");
        return merged;
    }
    
    /**
     * This method checks if a rule is valid, which means it does not cover any UP relation of than ones from the input UP set.
     * @param r
     * @param config
     * @return
     */
    public static boolean isValidRule(Rule r, Config config, boolean noiseDetection, double alpha){
        r.setCoveredUP(computeCoveredUP(r, config));
        if (!noiseDetection || alpha == 0.0){

            return (config.getUPList().containsAll(r.getCoveredUP()));
        }
        else{
            int numUnderAssign = (int) (r.getCoveredUP().size() * alpha);
            int underAssign = 0;
            for (Triple<String, String, String> element : r.getCoveredUP()) {
                if (!config.getUPList().contains(element)) {
                    underAssign++;
                    if (underAssign > numUnderAssign) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
    
    /**
     * This method checks if a rule is better, which means it does not cover any other UP relation than ones from the input UP set.
     * If config compareCoveredUPNum is true, it checks if the new rule covers more UP than the original one as well.
     * @param r
     * @param config
     * @return
     */
    public static boolean isBetterRule(Rule r, Config config, boolean noiseDetection, double alpha){
        int originalCoveredUPNum = r.getCoveredUP().size();
        r.setCoveredUP(computeCoveredUP(r, config));
        int newCoveredUPNum = r.getCoveredUP().size();
        //boolean result = config.getUPList().containsAll(r.getCoveredUP());
        boolean result = isValidRule(r, config, noiseDetection, alpha);
        if (config.getCompareCoveredUPNum()) {
            result = result && (newCoveredUPNum > originalCoveredUPNum);
        }
        return result;
    }
    
    
    public static void getCandidateConstraintSortInfo(ArrayList<AtomicConstraint> ccList, Rule newRule){
        for (AtomicConstraint cc: ccList){
            System.out.println("cc: " + cc);
            // cover index = 1 if atomic constraint cover 1 sub path or 1 res path of the candidate rule, 2 if both.
            int coverIndex = 0;
            for (AtomicCondition ac:newRule.getSubjectCondition()){
                if (ac.getPath().equals(cc.getSubPath())){
                    coverIndex++;
                }
            }
            for (AtomicCondition ac:newRule.getResourceCondition()){
                if (ac.getPath().equals(cc.getResPath())){
                    coverIndex++;
                }
                if (coverIndex == 2){
                    break;
                }
            }
            System.out.println("coverIndex = " + coverIndex);
            // if they are still tie, the last criteria is their length and operators
            int length = cc.getSubPath().size() + cc.getResPath().size();
            System.out.println("size: " + length);
        }
    }
    
    /*/**
     * This method added a new candidate rule after generalize it to the rules set
     * @param subList
     * @param resList
     * @param ops
     * @param cc
     * @param uncovUP
     * @param rules
     * @param config
     * @param subPathLimit
     * @param resPathLimit
     * @param numConstraintLimit
     * @param isOneConditionPerPathRestricted
     * @param limitConstraintSizeAll
     * @param limitConstraintSizeHalf
     * @param removeConditionThreshold
     * @return
     */
    /*public static boolean addCandidateRule(ArrayList<String> subList, ArrayList<String> resList, Set<String> ops, ArrayList<AtomicConstraint> cc, ArrayList<Triple<String, String, String>> uncovUP, LinkedList<Rule> rules, Config config, int subPathLimit, int resPathLimit, int numConstraintLimit, boolean isOneConditionPerPathRestricted, int limitConstraintSizeAll, int limitConstraintSizeHalf, int removeConditionThreshold, double alpha){
        // subject type
        String subType = config.getObjectModel().get(subList.get(0)).getClass1();
        // subject conditions
        ArrayList<AtomicCondition> subConditions = computeAttributeExpressions(subList, subPathLimit, config, true, isOneConditionPerPathRestricted);
        // resource type
        String resType = config.getObjectModel().get(resList.get(0)).getClass1();
        // resource conditions
        
        ArrayList<AtomicCondition> resConditions = computeAttributeExpressions(resList, resPathLimit, config, false, isOneConditionPerPathRestricted);
        Rule newRule = new Rule(config.getClassModel().get(subType), subConditions, config.getClassModel().get(resType), resConditions, new ArrayList<AtomicConstraint>(), ops);
        
        // sort the candidate constraints
        
        Rule generalizedRule = generalizeRule(newRule, cc, uncovUP, config, 0, numConstraintLimit, isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
        generalizedRule.setQuality(computeRuleQuality(generalizedRule, config.getUPList(), config));
        
        
        rules.add(generalizedRule);
        uncovUP.removeAll(generalizedRule.getCoveredUP());
        
        if (generalizedRule.equals(newRule)){
            return false;
        }
        return true;
    }*/
    
    
    /**
     * This method checks if a path is a boolean path
     * @param type
     * @param path
     * @param config
     * @return true if the path is boolean path, false otherwise
     */
    public static boolean isBooleanPath(String type, ArrayList<String> path, Config config){
        Map<String, Class1> classes = config.getClassModel();
        String currentClassName = type;
        for (String s:path){
            if (s.equals("id")){
                return false;
            }
            if (classes.get(currentClassName).getAllAttributes().get(s).getIsBoolean()){
                return true;
            }
            else {
                currentClassName = classes.get(currentClassName).getAllAttributes().get(s).getType().getClassName();
            }
        }
        return false;
    }

    /**
     * This method used to simplify a set of rules. It tries to simplify rule by rule.
     * @param rules
     * @param config
     * @param debugMode
     * @return
     */
    public static boolean simplifyRules(LinkedList<Rule> rules, Config config, boolean debugMode, int removeConditionThreshold, int[] stats, double alpha){
        //System.out.println("ENTER SIMPLIFY METHOD");
        boolean isSimplified = false;

        for (int i = 0; i < rules.size(); i++){
            Rule r = rules.get(i);
            // remove atomic conjuncts when appropriate
            ArrayList<Pair<AttributePathType, AtomicCondition>> allConditions = new ArrayList<Pair<AttributePathType, AtomicCondition>>();
            for (AtomicCondition subCon:r.getSubjectCondition()){
                allConditions.add(new Pair(AttributePathType.SubAttributePath, subCon));
            }
            for (AtomicCondition resCon:r.getResourceCondition()){
                allConditions.add(new Pair(AttributePathType.ResAttributePath, resCon));
            }
            Rule bestRule = elimConditions(r, allConditions, config, removeConditionThreshold, alpha);
            if (!bestRule.equals(r)){
                stats[3] += (r.getSubjectCondition().size() + r.getResourceCondition().size()) - (bestRule.getSubjectCondition().size() + bestRule.getResourceCondition().size());
                isSimplified = true;
                rules.set(i, bestRule);
                if (debugMode){
                    System.out.println("==============================================");
                    System.out.println("Eliminating Conditions for:\n" + r);
                    System.out.println("Best rule after elimination: " + bestRule);
                }
            }
        } 

        for (int i = 0; i < rules.size(); i++){
            Rule r = rules.get(i);
            // remove atomic constraints when appropriate
            // reverse the constraint list of the rule because we want to consider removing the rule that is considered later when generalizing rule
            ArrayList<AtomicConstraint> reverseList = new ArrayList<AtomicConstraint>(r.getConstraint());
            Collections.reverse(reverseList);
            Rule bestRule = elimConstraints(r, reverseList, config, alpha);
            if (!bestRule.equals(r)){
                stats[4] += r.getConstraint().size() - bestRule.getConstraint().size();
                isSimplified = true;
                rules.set(i, bestRule);
                if (debugMode){
                    System.out.println("==============================================");
                    System.out.println("Eliminating Constraints for:\n" + r);
                    System.out.println("Best rule after elimination: " + bestRule);
                }
            }
        }
        
        // remove overlapping operations
        ArrayList<Rule> removeRules = new ArrayList<Rule>();
        for (Rule r:rules){
            Pair<Boolean, Boolean> result = elimOverlapOps(r, rules, config, stats, removeRules);
            if (result.getFirst()){
                isSimplified = true;
            }
            if (result.getSecond()){
                removeRules.add(r);
            }
        }
        rules.removeAll(removeRules);
        
        ArrayList<Rule> removeRules1 = new ArrayList<Rule>();
        for (Rule r:rules){
            Pair<Boolean, Boolean> result = elimOverlapOpsUPCovered(r, rules, config, stats);
            if (result.getFirst()){
                isSimplified = true;
            }
            if (result.getSecond()){
                removeRules1.add(r);
            }
        }
        rules.removeAll(removeRules1);
        
        // constant propagation
        for (Rule r:rules){
            ArrayList<AtomicConstraint> removeConstraints = new ArrayList<AtomicConstraint>();
            for (AtomicConstraint cons:r.getConstraint()){
                boolean isFoundSubCond = false;
                boolean isFoundResCond = false;
                boolean isNegativeCons = cons.getIsNegative();
                if (cons.getConstraintOperator().equals(ConstraintOperator.EQUALS_VALUE) || cons.getConstraintOperator().equals(ConstraintOperator.CONTAINS)
                        || cons.getConstraintOperator().equals(ConstraintOperator.IN)){
                    // check if the sub path of the cons appear in an atomic condition of subject condition
                    boolean isReplacedConsSub = false;
                    for (AtomicCondition subCon:r.getSubjectCondition()){
                        if (cons.getSubPath().equals(subCon.getPath())){
                            isFoundSubCond = true;
                            if (subCon.getConstant().size() == 1 && subCon.getConditionOperator().equals(ConditionOperator.IN) && !subCon.getIsNegative()){
                                // remove the constraint
                                removeConstraints.add(cons);
                                // add new resource condition with the resource path from the constraint
                                ArrayList<String> newPath = new ArrayList<String>(cons.getResPath());
                                Set<String> newConstant = new HashSet<String>();
                                newConstant.addAll(subCon.getConstant());
                                AtomicCondition newCondition;
                                if (cons.getConstraintOperator().equals(ConstraintOperator.EQUALS_VALUE)){
                                    newCondition = new AtomicCondition(newPath, newConstant, ConditionOperator.IN);
                                }
                                else {
                                    newCondition = new AtomicCondition(newPath, newConstant, ConditionOperator.CONTAINS);
                                }
                                newCondition.setIsNegative(isNegativeCons);
                                r.getResourceCondition().add(newCondition);
                                isReplacedConsSub = true;
                                break;
                            }
                        }
                    }

                    if (!isReplacedConsSub){
                        // check if the res path of the cons appear in an atomic condition of resource condition
                        for (AtomicCondition resCon:r.getResourceCondition()){
                            if (cons.getResPath().equals(resCon.getPath())){
                                isFoundResCond = true;
                                if (resCon.getConstant().size() == 1 && resCon.getConditionOperator().equals(ConditionOperator.IN) && !resCon.getIsNegative()){
                                    // remove the constraint
                                    removeConstraints.add(cons);
                                    // add new resource condition with the subject path from the constraint
                                    ArrayList<String> newPath = new ArrayList<String>(cons.getSubPath());
                                    Set<String> newConstant = new HashSet<String>();
                                    newConstant.addAll(resCon.getConstant());
                                    AtomicCondition newCondition;
                                    if (cons.getConstraintOperator().equals(ConstraintOperator.EQUALS_VALUE)){
                                        newCondition = new AtomicCondition(newPath, newConstant, ConditionOperator.IN);
                                    }
                                    else{
                                        newCondition = new AtomicCondition(newPath, newConstant, ConditionOperator.CONTAINS);
                                    }
                                    newCondition.setIsNegative(isNegativeCons);
                                    r.getSubjectCondition().add(newCondition);
                                    break;
                                }
                            }
                        }
                    }  
                }
                if (cons.getConstraintOperator().equals(ConstraintOperator.EQUALS_VALUE) && !isFoundSubCond && !isFoundResCond && (cons.getSubPath().size() > 1 || cons.getResPath().size() > 1)){
                    // try to replace the atomic constraint with appropriate sub/res condition if sub path and res path evaluates to the same constant
                    // value for all of subjects and resources that appear in the set of covered tuples.
                    ArrayList<String> subCondPath = new ArrayList<String>(cons.getSubPath());
                    ArrayList<String> resCondPath = new ArrayList<String>(cons.getResPath());
                    if (subCondPath.size() <= resCondPath.size()){
                        boolean isReplaced = false;
                        if (cons.getResPath().size() > 1){
                            Pair<Boolean, String> check = checkSameConstValForConstraintPathEval(config, resCondPath, r.getCoveredUP(), false);
                            if (check.getFirst()){
                                // create new condition
                                Set<String> newConst = new HashSet<String>();
                                newConst.add(check.getSecond());
                                AtomicCondition newSubCondition = new AtomicCondition(new ArrayList<String>(cons.getSubPath()),newConst, ConditionOperator.IN);
                                newSubCondition.setIsNegative(isNegativeCons);
                                // check if the new rule is valid
                                Rule tempR = new Rule(r);
                                tempR.getSubjectCondition().add(newSubCondition);
                                tempR.getConstraint().remove(cons);
                                if (isValidRule(tempR, config, false, 0.0)){
                                    isReplaced = true;
                                    r.getSubjectCondition().add(newSubCondition);
                                    removeConstraints.add(cons);
                                }
                            }
                        }
                        if (!isReplaced){
                            Pair<Boolean, String> check = checkSameConstValForConstraintPathEval(config, subCondPath, r.getCoveredUP(), true);
                            if (check.getFirst()){
                                // create new condition
                                Set<String> newConst = new HashSet<String>();
                                newConst.add(check.getSecond());
                                AtomicCondition newResCondition = new AtomicCondition(new ArrayList<String>(cons.getResPath()),newConst, ConditionOperator.IN);
                                newResCondition.setIsNegative(isNegativeCons);
                                // check if the new rule is valid
                                Rule tempR = new Rule(r);
                                tempR.getResourceCondition().add(newResCondition);
                                tempR.getConstraint().remove(cons);
                                if (isValidRule(tempR, config, false, 0.0)){
                                    r.getResourceCondition().add(newResCondition);
                                    removeConstraints.add(cons);
                                }
                            }
                        }
                    }
                    else{
                        boolean isReplaced = false;
                        if (cons.getSubPath().size() > 1){
                            Pair<Boolean, String> check = checkSameConstValForConstraintPathEval(config, subCondPath, r.getCoveredUP(), true);
                            if (check.getFirst()){
                                // create new condition
                                Set<String> newConst = new HashSet<String>();
                                newConst.add(check.getSecond());
                                AtomicCondition newResCondition = new AtomicCondition(new ArrayList<String>(cons.getResPath()),newConst, ConditionOperator.IN);
                                newResCondition.setIsNegative(isNegativeCons);
                                // check if the new rule is valid
                                Rule tempR = new Rule(r);
                                tempR.getResourceCondition().add(newResCondition);
                                tempR.getConstraint().remove(cons);
                                if (isValidRule(tempR, config, false, 0.0)){
                                    isReplaced = true;
                                    r.getResourceCondition().add(newResCondition);
                                    removeConstraints.add(cons);
                                }
                            }
                        }
                        if (!isReplaced){
                            Pair<Boolean, String> check = checkSameConstValForConstraintPathEval(config, resCondPath, r.getCoveredUP(), false);
                            if (check.getFirst()){
                                // create new condition
                                Set<String> newConst = new HashSet<String>();
                                newConst.add(check.getSecond());
                                AtomicCondition newSubCondition = new AtomicCondition(new ArrayList<String>(cons.getSubPath()),newConst, ConditionOperator.IN);
                                newSubCondition.setIsNegative(isNegativeCons);
                                // check if the new rule is valid
                                Rule tempR = new Rule(r);
                                tempR.getSubjectCondition().add(newSubCondition);
                                tempR.getConstraint().remove(cons);
                                if (isValidRule(tempR, config, false, 0.0)){
                                    r.getSubjectCondition().add(newSubCondition);
                                    removeConstraints.add(cons);
                                }
                            }
                        }
                    }
                }
            }
            if (!removeConstraints.isEmpty()){
                isSimplified = true;
                r.getConstraint().removeAll(removeConstraints);
            }
        } 

        /*
        // remove loops in condition paths
        for (int i = 0; i < rules.size(); i++){
            Rule r = rules.get(i);
            ArrayList<Triple<String, String, String>> originalCoveredUP = r.getCoveredUP();

            // check every sub condition
            for (int subIndex = 0; subIndex < r.getSubjectCondition().size(); subIndex++) {
                AtomicCondition subCon = r.getSubjectCondition().get(subIndex);
                if (removeLoopInConditionPath(r.getSubjectType().getClassName(), subCon, r, originalCoveredUP, config)){
                    isSimplified = true;
                }
            }

            // check every res condition
            for (int resIndex = 0; resIndex < r.getResourceCondition().size(); resIndex++) {
                AtomicCondition resCon = r.getResourceCondition().get(resIndex);
                if (removeLoopInConditionPath(r.getResourceType().getClassName(), resCon, r, originalCoveredUP, config)){
                    isSimplified = true;
                }
            }
        }

        // remove loops in constraint paths
        for (int i = 0; i < rules.size(); i++){
            Rule r = rules.get(i);
            ArrayList<Triple<String, String, String>> originalCoveredUP = r.getCoveredUP();

            // check every atomic constraint
            for (int j = 0; j < r.getConstraint().size(); j++){
                AtomicConstraint cons = r.getConstraint().get(j);
                if (removeLoopInConstraintPath(r.getSubjectType().getClassName(), r.getResourceType().getClassName(), cons, r, originalCoveredUP, config)){
                    isSimplified = true;
                }
            }
        }*/
        return isSimplified;
    }
    
    /**
     * this function is used to check if a subject path or a resource path of an atomic constraint evaluates to a same constant value. Used in one of the constant simplificaiton.
     * @param config
     * @param condPath
     * @param auths
     * @param isCheckingSubjectPath
     * @return 
     */
    private static Pair<Boolean, String> checkSameConstValForConstraintPathEval(Config config, ArrayList<String> condPath, List<Triple<String, String, String>> auths, boolean isCheckingSubjectPath){
        // evaluate the resource path first
        String constVal = "init";
        for (Triple<String, String, String> auth:auths){
            String obj = "";
            if (isCheckingSubjectPath){
                obj = auth.getFirst();
            }
            else{
                obj = auth.getSecond();
            }
            Triple<Boolean, ArrayList<String>, Boolean> val = Parser.getAttributePathValues(config.getObjectModel().get(obj), condPath, config.getObjectModel(), config.getClassModel(), config);
            if (val.getSecond().isEmpty()){
                return new Pair(false, null);
            }
            if (constVal.equals("init")){
                constVal = val.getSecond().get(0);
            }
            else if (!val.getSecond().get(0).equals(constVal)){
                return new Pair(false, null);
            }
        }
        return new Pair(true, constVal);
    }


    private static boolean removeLoopInConditionPath(String conditionClass, AtomicCondition con, Rule r, ArrayList<Triple<String, String, String>> originalCoveredUP, Config config) {
        ArrayList<String> oriConditionPath = new ArrayList<String>(con.getPath());
        boolean isOriConditionPathManyMul = false;
        if (con.getConditionOperator() == ConditionOperator.CONTAINS){
            isOriConditionPathManyMul = true;
        }
        ArrayList<String> conditionPath = con.getPath();
        ArrayList<String> conditionPathClass = new ArrayList<>();
        Map<String, Set<Pair<String,String>>> adjacentList = config.getAdjacencyList();
        String currentClass = conditionClass;
        
        // add current class name first to conditionPathClass list
        conditionPathClass.add(currentClass);
        
        // go through condition path and add class on the path, ignoring the last edge
        for (int i = 0; i < conditionPath.size() - 1; i++) {
            // get edge and get the class associated with the current class and edge
            boolean moveToNextClass = false;
            String currentEdge = conditionPath.get(i);
            Set<Pair<String, String>> edgeList = adjacentList.get(currentClass);
            for (Pair<String, String> edge : edgeList) {
                if (currentEdge.equals(edge.getFirst())) {
                    // use edge to go to next class in the path
                    conditionPathClass.add(edge.getSecond());
                    currentClass = edge.getSecond();
                    moveToNextClass = true;
                    break;
                }
            }
            
            // safety check, handle case when edge goes to nowhere
            if (!moveToNextClass) {
                return false;
            }
        }
        
        // go through condition path class and check if there is any loop
        boolean isCycleRemoved = false;
        for (int i = 1; i < conditionPathClass.size(); i++) {
            String className = conditionPathClass.get(i);
            // check all classes on the path before current index to see if it already appears
            for (int j = 0; j < i; j++) {
                // found loop if the class already appears more than 1 index away
                if (className.equals(conditionPathClass.get(j))) {
                    // remove item from index j to index i in reverse
                    for (int k = i - 1; k >= j; k--) {
                        // 1st item in conditionPathClass doesn't appear in conditionPath, therefore the index to remove is different
                        conditionPath.remove(k);
                        conditionPathClass.remove(k + 1);
                        isCycleRemoved = true;
                    }
                    // reset index
                    i = j;
                    break;
                }
            }
        }
        if (!isCycleRemoved){
            return false;
        }
        // set condition operator
        if (isManyMultiplicity(conditionPathClass.get(0), conditionPath, config)){
            if (con.getConstant().size() > 1){
                con.setPath(oriConditionPath);
                return false;
            }
            con.setConditionOperator(ConditionOperator.CONTAINS);
        }
        else {
            con.setConditionOperator(ConditionOperator.IN);
        }
        // check if the new rule is still valid
        if (!isValidRule(r, config, false, 0.0)) {
            if (isOriConditionPathManyMul){
                con.setConditionOperator(ConditionOperator.CONTAINS);
            }
            else {
                con.setConditionOperator(ConditionOperator.IN);
            }
            con.setPath(oriConditionPath);
            r.setCoveredUP(computeCoveredUP(r, config));
            return false;
        }
        
        // isValidRule() computes the new rule's covered UP
        if (r.getCoveredUP().isEmpty()) {
            // new rule doesn't covered any UP, skip
            if (isOriConditionPathManyMul){
                con.setConditionOperator(ConditionOperator.CONTAINS);
            }
            else {
                con.setConditionOperator(ConditionOperator.IN);
            }
            con.setPath(oriConditionPath);
            r.setCoveredUP(computeCoveredUP(r, config));
            return false;
        }
        
        if (!r.getCoveredUP().containsAll(originalCoveredUP)) {
            // new rule doesn't cover all covered UP of original rule,
            // skip
            if (isOriConditionPathManyMul){
                con.setConditionOperator(ConditionOperator.CONTAINS);
            }
            else {
                con.setConditionOperator(ConditionOperator.IN);
            }
            con.setPath(oriConditionPath);
            r.setCoveredUP(computeCoveredUP(r, config));
            return false;
        }
        return true;
    }
    
    /*private static boolean removeLoopInConstraintPath(String constraintSubClass, String constraintResClass, AtomicConstraint con, Rule r, ArrayList<Triple<String, String, String>> originalCoveredUP, Config config) {
        ArrayList<String> oriConstraintSubPath = new ArrayList<String>(con.getSubPath());
        ArrayList<String> oriConstraintResPath = new ArrayList<String>(con.getResPath());
        ConstraintOperator oriConstraintOperator = con.getConstraintOperator();
        ArrayList<String> constraintSubPath = con.getSubPath();
        ArrayList<String> constraintResPath = con.getResPath();
        ArrayList<String> constraintSubPathClass = new ArrayList<>();
        ArrayList<String> constraintResPathClass = new ArrayList<>();
        Map<String, Set<Pair<String,String>>> adjacentList = config.getAdjacencyList();
        
        String currentSubClass = constraintSubClass;
        
        // add current class name first to constraintPathClass list
        constraintSubPathClass.add(currentSubClass);
        
        // go through constraint path and add class on the path, ignoring the last edge
        for (int i = 0; i < constraintSubPath.size() - 1; i++) {
            // get edge and get the class associated with the current class and edge
            boolean moveToNextClass = false;
            String currentEdge = constraintSubPath.get(i);
            Set<Pair<String, String>> edgeList = adjacentList.get(currentSubClass);
            for (Pair<String, String> edge : edgeList) {
                if (currentEdge.equals(edge.getFirst())) {
                    // use edge to go to next class in the path
                    constraintSubPathClass.add(edge.getSecond());
                    currentSubClass = edge.getSecond();
                    moveToNextClass = true;
                    break;
                }
            }
            
            // safety check, handle case when edge goes to nowhere
            if (!moveToNextClass) {
                return false;
            }
        }
        
        String currentResClass = constraintResClass;
        
        // add current class name first to constraintPathClass list
        constraintResPathClass.add(currentResClass);
        
        // go through constraint path and add class on the path, ignoring the last edge
        for (int i = 0; i < constraintResPath.size() - 1; i++) {
            // get edge and get the class associated with the current class and edge
            boolean moveToNextClass = false;
            String currentEdge = constraintResPath.get(i);
            Set<Pair<String, String>> edgeList = adjacentList.get(currentResClass);
            for (Pair<String, String> edge : edgeList) {
                if (currentEdge.equals(edge.getFirst())) {
                    // use edge to go to next class in the path
                    constraintResPathClass.add(edge.getSecond());
                    currentResClass = edge.getSecond();
                    moveToNextClass = true;
                    break;
                }
            }
            
            // safety check, handle case when edge goes to nowhere
            if (!moveToNextClass) {
                return false;
            }
        }
        
        // go through subject path class and check if there is any loop
        boolean isSubCycleRemoved = false;
        for (int i = 1; i < constraintSubPathClass.size(); i++) {
            String className = constraintSubPathClass.get(i);
            // check all classes on the path before current index to see if it already appears
            for (int j = 0; j < i; j++) {
                // found loop if the class already appears more than 1 index away
                if (className.equals(constraintSubPathClass.get(j))) {
                    // remove item from index j to index i in reverse
                    for (int k = i - 1; k >= j; k--) {
                        // 1st item in constraintPathClass doesn't appear in constraintPath, therefore the index to remove is different
                        constraintSubPath.remove(k);
                        constraintSubPathClass.remove(k + 1);
                        isSubCycleRemoved = true;
                    }
                    // reset index
                    i = j;
                    break;
                }
            }
        }
        
        // go through resource path class and check if there is any loop
        boolean isResCycleRemoved = false;
        for (int i = 1; i < constraintResPathClass.size(); i++) {
            String className = constraintResPathClass.get(i);
            // check all classes on the path before current index to see if it already appears
            for (int j = 0; j < i; j++) {
                // found loop if the class already appears more than 1 index away
                if (className.equals(constraintResPathClass.get(j))) {
                    // remove item from index j to index i in reverse
                    for (int k = i - 1; k >= j; k--) {
                        // 1st item in constraintPathClass doesn't appear in constraintPath, therefore the index to remove is different
                        constraintResPath.remove(k);
                        constraintResPathClass.remove(k + 1);
                        isResCycleRemoved = true;
                    }
                    // reset index
                    i = j;
                    break;
                }
            }
        }
        
        if (!isSubCycleRemoved && !isResCycleRemoved){
            return false;
        }
        
        
        // set constraint operator
        con.setConstraintOperator(operatorFromPath(constraintSubClass, constraintSubPath, constraintResClass,constraintResPath, config));
        
        // check if the new rule is still valid
        if (!isValidRule(r, config, false, 0.0)) {
            con.setSubPath(oriConstraintSubPath);
            con.setResPath(oriConstraintResPath);
            con.setConstraintOperator(oriConstraintOperator);
            r.setCoveredUP(computeCoveredUP(r, config));
            return false;
        }
        
        // isValidRule() computes the new rule's covered UP
        if (r.getCoveredUP().isEmpty()) {
            // new rule doesn't covered any UP, skip
            con.setSubPath(oriConstraintSubPath);
            con.setResPath(oriConstraintResPath);
            con.setConstraintOperator(oriConstraintOperator);
            r.setCoveredUP(computeCoveredUP(r, config));
            return false;
        }
        
        if (!r.getCoveredUP().containsAll(originalCoveredUP)) {
            // new rule doesn't cover all covered UP of original rule,
            // skip
            con.setSubPath(oriConstraintSubPath);
            con.setResPath(oriConstraintResPath);
            con.setConstraintOperator(oriConstraintOperator);
            r.setCoveredUP(computeCoveredUP(r, config));
            return false;
        }
        return true;
    }*/
    
    
    /**
     * This method try to simplify rules by eliminate appropriate atomic constraints  of a rule
     * @param r
     * @param constraints
     * @param config
     * @return
     */
    public static Rule elimConstraints(Rule r, ArrayList<AtomicConstraint> constraints, Config config, double alpha) {
        Rule bestRule = r;
        QualityValue bestQuality = r.getQuality();
        ArrayList<AtomicConstraint> validConstraints = new ArrayList<AtomicConstraint>(
                constraints);
        for (int i = 0; i < validConstraints.size(); i++) {
            Rule temp = new Rule(r);
            temp.getConstraint().remove(validConstraints.get(i));
            if (!isValidRule(temp, config, config.getUnderassignmentDetection(), alpha)) {
                temp.setQuality(computeRuleQuality(temp, config.getUPList(), config));
                validConstraints.remove(i);
                i--;
            }
        }
        for (int i = 0; i < validConstraints.size(); i++) {
            Rule r1 = new Rule(r);
            r1.getConstraint().remove(validConstraints.get(i));
            if (!isValidRule(r1, config, config.getUnderassignmentDetection(), alpha)){
                continue;
            }
            r1.setQuality(computeRuleQuality(r1, config.getUPList(), config));
            ArrayList<AtomicConstraint> newCC = new ArrayList<AtomicConstraint>();
            for (AtomicConstraint ac:validConstraints.subList(i + 1, validConstraints.size())){
                newCC.add(ac);
            }
            Rule tempR = elimConstraints(r1, newCC, config, alpha);
            QualityValue quality = computeRuleQuality(tempR, config.getUPList(),config);
            if (quality.compareTo(bestQuality) > 0) {
                bestRule = tempR;
                bestQuality = quality;
            }
            
        }
        return bestRule;
    }
    public static Rule elimConditionsRecursive(Rule r, ArrayList<Pair<AttributePathType, AtomicCondition>> conditions, Config config, double alpha){
        Rule bestRule = r;
        QualityValue bestQuality = r.getQuality();
        ArrayList<Pair<AttributePathType, AtomicCondition>> validRemoveConditions = new ArrayList<Pair<AttributePathType, AtomicCondition>>(conditions);
        
        // compute the list of removing condition that still preserve the validity of the rule
        for (int i = 0; i < validRemoveConditions.size(); i++) {
            Pair<AttributePathType, AtomicCondition> currentCondition = validRemoveConditions.get(i);
            Rule temp = new Rule(r);
            if (currentCondition.getFirst() == AttributePathType.SubAttributePath){
                temp.getSubjectCondition().remove(currentCondition.getSecond());
            }
            else {
                temp.getResourceCondition().remove(currentCondition.getSecond());
            }
            if (!isValidRule(temp, config, config.getUnderassignmentDetection(), alpha)) {
                temp.setQuality(computeRuleQuality(temp, config.getUPList(), config));
                validRemoveConditions.remove(i);
                i--;
            }
        }
        
        for (int i = 0; i < validRemoveConditions.size(); i++) {
            Pair<AttributePathType, AtomicCondition> currentCondition = validRemoveConditions.get(i);
            Rule r1 = new Rule(r);
            if (currentCondition.getFirst() == AttributePathType.SubAttributePath){
                r1.getSubjectCondition().remove(currentCondition.getSecond());
            }
            else {
                r1.getResourceCondition().remove(currentCondition.getSecond());
            }
            if (!isValidRule(r1, config, config.getUnderassignmentDetection(), alpha)){
                continue;
            }
            r1.setQuality(computeRuleQuality(r1, config.getUPList(), config));
            ArrayList<Pair<AttributePathType, AtomicCondition>> newCC = new ArrayList<Pair<AttributePathType, AtomicCondition>>();
            for (Pair<AttributePathType, AtomicCondition> ac:validRemoveConditions.subList(i + 1, validRemoveConditions.size())){
                newCC.add(ac);
            }
            Rule tempR = elimConditionsRecursive(r1, newCC, config, alpha);
            QualityValue quality = computeRuleQuality(tempR, config.getUPList(),config);
            if (quality.compareTo(bestQuality) >= 0) {
                bestRule = tempR;
                bestQuality = quality;
            }
        }
        return bestRule;
    }
    
    /**
     * this method computes the list of UP triples covered by a set of input rules
     * @param rules
     * @return
     */
    public static ArrayList<Triple<String, String, String>> computePolicyCoveredUP(LinkedList<Rule> rules){
        ArrayList<Triple<String, String, String>> results = new ArrayList<Triple<String, String, String>>();
        for (Rule r: rules){
            results.addAll(r.getCoveredUP());
        }
        return results;
    }
    
    /**
     * this method computes the list of UP triples covered by a set of input rules
     * @param rules
     * @return
     */
    public static Set<Triple<String, String, String>> computePolicyCoveredUPNoise(LinkedList<Rule> rules, Config config){
        Set<Triple<String, String, String>> results = new HashSet<Triple<String, String, String>>();
        for (Rule r: rules){
            results.addAll(computeCoveredUP(r, config));
        }
        return results;
    }
    
    /**
     * This method removes an operation op from the set of operations in a rule R when this preserves the meaning of the policy
     * @param r
     * @param rules
     * @param config
     * @return
     */
    public static Pair<Boolean, Boolean> elimOverlapOpsUPCovered(Rule r, LinkedList<Rule> rules, Config config, int[] stats){
        boolean isSimplified = false;
        LinkedList<Rule> tempRules = new LinkedList<Rule>(rules);
        tempRules.remove(r);
        Rule tempRule = new Rule(r);
        tempRules.add(tempRule);
        Iterator<String> ops = r.getActions().iterator();
        while (ops.hasNext()){
            String op = ops.next();
            tempRule.getActions().remove(op);
            tempRule.setCoveredUP(computeCoveredUP(tempRule, config));
            if (computePolicyCoveredUPNoise(tempRules,config).containsAll(config.getUPList())){
                ops.remove();
                stats[5]++;
                r.setCoveredUP(computeCoveredUP(r, config));
                isSimplified = true;
                
            }
            else {
                tempRule.getActions().add(op);
                tempRule.setCoveredUP(computeCoveredUP(tempRule, config));
            }
        }
        if (r.getActions().isEmpty()) {
            return new Pair(true, true);
        }
        return new Pair(isSimplified, false);
    }
    
    /**
     * This method used to eliminate overlapping operations in a rule as discussed in ABAC Mining paper as item (6) of simplify() method
     * @param r
     * @param rules
     * @param config
     * @return
     */
    public static Pair<Boolean, Boolean> elimOverlapOps(Rule r, LinkedList<Rule> rules, Config config, int[] stats, ArrayList<Rule> removedRules){
        boolean isSimplified = false;
        for (Rule r1:rules){
            if (r == r1 || removedRules.contains(r1)){
                continue;
            }
            
            ArrayList<String> parentsSub = new ArrayList<String>();
            String currentSubClass = r.getSubjectType().getClassName();
            parentsSub.add(currentSubClass);
            while (config.getClassModel().get(currentSubClass).getParentClass() != null){
                String newParent = config.getClassModel().get(currentSubClass).getParentClass().getClassName();
                parentsSub.add(newParent);
                currentSubClass = newParent;
            }
            if (!parentsSub.contains(r1.getSubjectType().getClassName())){
                continue;
            }
            
            ArrayList<String> parentsRes = new ArrayList<String>();
            String currentResClass = r.getResourceType().getClassName();
            parentsRes.add(currentResClass);
            while (config.getClassModel().get(currentResClass).getParentClass() != null){
                String newParent = config.getClassModel().get(currentResClass).getParentClass().getClassName();
                parentsRes.add(newParent);
                currentResClass = newParent;
            }
            if (!parentsRes.contains(r1.getResourceType().getClassName())){
                continue;
            }
            
            if (!r.getConstraint().containsAll(r1.getConstraint())){
                continue;
            }
            if (!r.getSubAttributes().containsAll(r1.getSubAttributes())){
                continue;
            }
            
            if (!r.getResAttributes().containsAll(r1.getResAttributes())){
                continue;
            }
            boolean isSubset = true;
            // check subject conditions
            if (r.getSubjectCondition().size() != r1.getSubjectCondition().size()){
                continue;
            }
            boolean containsAllSubConds = true;
            for (AtomicCondition ac:r.getSubjectCondition()){
                boolean found = false;
                for (AtomicCondition ac1:r1.getSubjectCondition()){
                    if (ac.getPath().equals(ac1.getPath()) && (ac.getIsNegative() == ac1.getIsNegative())){
                        found = true;
                    }
                }
                if (!found){
                    containsAllSubConds = false;
                    break;
                }
            }
            if (!containsAllSubConds){
                continue;
            }
            for (AtomicCondition ac1:r1.getSubjectCondition()){
                boolean found = false;
                ArrayList<String> path1 = ac1.getPath();
                for (AtomicCondition ac:r.getSubjectCondition()){
                    if (ac.getPath().equals(path1) && (ac.getIsNegative() == ac1.getIsNegative())){
                        if (!ac1.getConstant().containsAll(ac.getConstant())){
                            isSubset = false;
                            break;
                        }
                        found = true;
                    }
                }
                if (!isSubset || !found){
                    break;
                }
            }
            if (!isSubset){
                continue;
            }
            
            // check resource conditions
            if (r.getResourceCondition().size() != r1.getResourceCondition().size()){
                continue;
            }
            
            boolean containsAllResConds = true;
            for (AtomicCondition ac:r.getResourceCondition()){
                boolean found = false;
                for (AtomicCondition ac1:r1.getResourceCondition()){
                    if (ac.getPath().equals(ac1.getPath()) && (ac.getIsNegative() == ac1.getIsNegative())){
                        found = true;
                    }
                }
                if (!found){
                    containsAllResConds = false;
                    break;
                }
            }
            if (!containsAllResConds){
                continue;
            }
            
            for (AtomicCondition ac1:r1.getResourceCondition()){
                ArrayList<String> path1 = ac1.getPath();
                boolean found = false;
                for (AtomicCondition ac:r.getResourceCondition()){
                    if (ac.getPath().equals(path1) && ac.getIsNegative() == ac1.getIsNegative()){
                        if (!ac1.getConstant().containsAll(ac.getConstant())){
                            isSubset = false;
                            break;
                        }
                        found = true;
                    }
                }
                if (!isSubset || !found){
                    break;
                }
            }
            if (!isSubset){
                continue;
            }
            
            // Then remove ovelapping rules
            Set<String> ovelapOps = new HashSet<String>();
            for (String op : r.getActions()) {
                if (r1.getActions().contains(op)) {
                    ovelapOps.add(op);
                    stats[5]++;
                }
            }
            r.getActions().removeAll(ovelapOps);
            r.setCoveredUP(computeCoveredUP(r, config));
            if (!ovelapOps.isEmpty()) {
                isSimplified = true;
            }
            if (r.getActions().isEmpty()) {
                return new Pair(true, true);
            }
        }
        
        // remove ops that covered UP which could be covered by other rules
//        LinkedList<Rule> tempRules = new LinkedList<Rule>(rules);
//        tempRules.remove(r);
//        Rule tempRule = new Rule(r);
//        tempRules.add(tempRule);
//        Iterator<String> ops = r.getActions().iterator();
//        while (ops.hasNext()){
//            String op = ops.next();
//            ArrayList<Triple<String, String, String>> coveredUPBefore = computePolicyCoveredUP(tempRules);
//            tempRule.getActions().remove(op);
//            tempRule.setCoveredUP(computeCoveredUP(tempRule, config));
//            ArrayList<Triple<String, String, String>> coveredUPAfter = computePolicyCoveredUP(tempRules);
//            if (computePolicyCoveredUP(tempRules).containsAll(config.getUPList())){
//                ops.remove();
//                r.setCoveredUP(computeCoveredUP(r, config));
//                if (r.getActions().isEmpty() && !r.getCoveredUP().isEmpty()){
//                    System.out.println();
//                }
//                isSimplified = true;
//                if (!computePolicyCoveredUP(rules).containsAll(config.getUPList())){
//                    ArrayList<Triple<String, String, String>> coveredUP1 = computePolicyCoveredUP(rules);
//                    ArrayList<Triple<String, String, String>> coveredUP2 = computePolicyCoveredUP(tempRules);
//                    System.out.println();
//                    System.out.println("INVALID RULES GENERATED IN ELIM OVERLAP OPS");
//                    System.exit(0);
//                }
//            }
//            else {
//                tempRule.getActions().add(op);
//                tempRule.setCoveredUP(computeCoveredUP(tempRule, config));
//            }
//        }
//        if (r.getActions().isEmpty()) {
//            if (!computePolicyCoveredUP(rules).containsAll(config.getUPList())){
//                    System.out.println();
//                    System.out.println("INVALID RULES GENERATED IN ELIM OVERLAP OPS");
//                    System.exit(0);
//            }
//            return new Pair(true, true);
//        }
//        if (!computePolicyCoveredUP(rules).containsAll(config.getUPList())){
//                    System.out.println();
//                    System.out.println("INVALID RULES GENERATED IN ELIM OVERLAP OPS");
//                    System.exit(0);
//            }
        return new Pair(isSimplified, false);
    }
    
    
    
    /**
     * This method try to simplify rules by eliminate appropriate atomic conditions in both subject conditions and resource conditions of a rule
     * @param r
     * @param conditions
     * @param config
     * @param removeConditionThreshold
     * @return
     */
    public static Rule elimConditions(Rule r, ArrayList<Pair<AttributePathType, AtomicCondition>> conditions, Config config, int removeConditionThreshold, double alpha){
        // if the size of conditions is less than the remove condition threshold, we will consider all of possible combinations for removing
        if (conditions.size() <= removeConditionThreshold){
            return elimConditionsRecursive(r, conditions, config, alpha);
        }
        Collections.sort(conditions, new AtomicConditionComparator());
        Rule result = r;
        // compute the list of removing condition that still preserve the validity of the rule
        for (int i = 0; i < conditions.size(); i++) {
            Rule temp = new Rule(result);
            Pair<AttributePathType, AtomicCondition> currentCondition = conditions.get(i);
            if (currentCondition.getFirst() == AttributePathType.SubAttributePath){
                temp.getSubjectCondition().remove(currentCondition.getSecond());
            }
            else {
                temp.getResourceCondition().remove(currentCondition.getSecond());
            }
            if (isValidRule(temp, config, config.getUnderassignmentDetection(), alpha)) {
                result = temp;
                result.setQuality(computeRuleQuality(temp, config.getUPList(), config));
            }
        }
        return result;
    }
    
    /**
     * this methods output all the rules of a given policy
     * @param rules
     */
    public static void printPolicy(LinkedList<Rule> rules, Config config, BufferedWriter outputWrite){
        try{
            int policyWSC = 0;
            for (Rule r:rules){
                outputWrite.write(r + "\n");
                policyWSC += r.getWSC();
                outputWrite.write("Rule WSC: " + r.getWSC() + "\n");
                QualityValue rQual = computeRuleQuality(r, config.getUPList(), config);
                outputWrite.write("Rule Quality: <" + rQual.firstComponent + ", " + rQual.secondComponent + ", " + rQual.thirdComponent + ">\n");
                outputWrite.write("------------------------\n");
            }
            outputWrite.write("Policy WSC: " + policyWSC + "\n");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public static int computePolicyWSC(LinkedList<Rule> rules){
        int result = 0;
        for (Rule r:rules){
            result += r.getWSC();
        }
        return result;
    }
    
    /*/**
     * This is the main method to mine a RBAC policy
     * @param config the config contains all input data
     * @param debugMode the flag will print some results for debugging purpose
     * @param subConstraintExtraDistance extra distance from the shortest paths using when computing UAE
     * @param resConstraintExtraDistance extra distance from the shortest paths using when computing RAE
     * @param totalConstraintLengthPathLimit the limit of total length of a constraint (|subPath| + |resPath|)
     * @param subConditionPathLimit the limit of the subject path in each subject condition in an atomic condition
     * @param resConditionPathLimit the limit of the resource path in each resource condition in an atomic condition
     * @param numConstraintLimit the limit of number of constraints generated in a rule, using in generalizeRule() method
     * @param isOneConditionPerPathRestricted the flag for restriction of only 1 condition for each path in UAE or RAE
     * @param removeConditionThreshold the threshold used when remove conditions in simplifyRules()
     * @param limitConstraintSizeAll L1 described in algorithm.txt
     * @param limitConstraintSizeHalf L2 described in algorithm.txt
     * @return
     */
    /*public static Pair<LinkedList<Rule>, int[]> mineReBACPolicy(Config config, boolean debugMode, int subConstraintExtraDistance, int resConstraintExtraDistance, int totalConstraintLengthPathLimit, int subConditionPathLimit, int resConditionPathLimit, int numConstraintLimit, boolean isOneConditionPerPathRestricted, int removeConditionThreshold, int limitConstraintSizeAll, int limitConstraintSizeHalf, double tau, double alpha, boolean batchOptimization, int batchSize){
        int[] stats = new int[8];
        // Line 1 Rules is the set of candidate rules
        LinkedList<Rule> rules = new LinkedList<Rule>();
        
        if (batchOptimization){
            // Divided the UP list to batches of batchSize tuples
            ArrayList<ArrayList<Triple<String, String, String>>> batches = new ArrayList<ArrayList<Triple<String, String, String>>>();
            ArrayList<Triple<String, String, String>> currList = null;
            for (Triple<String, String, String> tuple : config.getUPList()) {
                if (currList == null || currList.size() == batchSize){
                    batches.add(currList = new ArrayList<Triple<String, String, String>>());
                }
                currList.add(tuple);
            }
            ArrayList<LinkedList<Rule>> batchRules = new ArrayList<LinkedList<Rule>>();
            
            // process each batch, including merging
            for (ArrayList<Triple<String, String, String>> batch: batches){
                LinkedList<Rule> batchRule = new LinkedList<Rule>();
                batchRules.add(batchRule);
                // Line 2 and 3
                // uncovUP containts user-permission tuples in UP0 that are not covered
                // by Rules
                ArrayList<Triple<String, String, String>> uncovUP = new ArrayList<Triple<String, String, String>>(batch);
                // Line 4 - 11
                
                Collections.sort(uncovUP, new UPComparator(config));
                //Collections.shuffle(uncovUP);
                while (!uncovUP.isEmpty()){
                    Triple<String, String, String> chosenTuple = uncovUP.get(0);
                    if (debugMode) {
                        int permCount = 0;
                        for (Pair<String, String> perm:config.getUPListMapOnRes().get(config.getObjectModel().get(chosenTuple.getSecond()).getClass1()).get(chosenTuple.getSecond())){
                            if (perm.getSecond().equals(chosenTuple.getThird())){
                                permCount++;
                            }
                        }
                        
                        int subCount = config.getUPListMapOnSub().get(config.getObjectModel().get(chosenTuple.getFirst()).getClass1()).get(chosenTuple.getFirst()).size();
                        System.out.println("Chosen Tuple Quality: <" + permCount + ", " + subCount +">");
                    }
                    ArrayList<AtomicConstraint> cc = candidateConstraint(config.getObjectModel().get(chosenTuple.getFirst()),
                            config.getObjectModel().get(chosenTuple.getSecond()),
                            config, subConstraintExtraDistance, resConstraintExtraDistance, totalConstraintLengthPathLimit);
                    
                    // compute subList: line 6-7
                    Pair<String, String> perm = new Pair(chosenTuple.getSecond(), chosenTuple.getThird());
                    ArrayList<String> subList = new ArrayList<String>();
                    Map<String, Set<Pair<String, String>>> subsWithSameTypeList = config.getUPListMapOnSub().get(config.getObjectModel().get(chosenTuple.getFirst()).getClass1());
                    for (String sub:subsWithSameTypeList.keySet()){
                        if (subsWithSameTypeList.get(sub).contains(perm)){
                            ArrayList<AtomicConstraint> cc1 = candidateConstraint(config.getObjectModel().get(sub),
                                    config.getObjectModel().get(chosenTuple.getSecond()),
                                    config, subConstraintExtraDistance, resConstraintExtraDistance, totalConstraintLengthPathLimit);
                            if (cc.containsAll(cc1) && cc1.containsAll(cc)){
                                subList.add(sub);
                            }
                        }
                    }
                    // line 8
                    ArrayList<String> resList = new ArrayList<String>();
                    resList.add(chosenTuple.getSecond());
                    Set<String> actions = new HashSet<String>();
                    actions.add(chosenTuple.getThird());
                    boolean isGeneralized = addCandidateRule(subList, resList, actions, cc, uncovUP, batchRule, config, subConditionPathLimit, resConditionPathLimit, numConstraintLimit, isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
                    if (isGeneralized){
                        stats[2]++;
                    }
                    if (debugMode){
                        System.out.println("========== Line 8 ==========");
                        System.out.println("Candidate rules");
                        for (Rule r:rules){
                            System.out.println(r);
                        }
                    }
                    // compute actions list: line 9
                    Set<String> actionList = new HashSet<String>();
                    for (Pair<String, String> p:config.getUPListMapOnSub().get(config.getObjectModel().get(chosenTuple.getFirst()).getClass1()).get(chosenTuple.getFirst())){
                        if (p.getFirst().equals(chosenTuple.getSecond())){
                            actionList.add(p.getSecond());
                        }
                    }
                    
                    // line 10
                    ArrayList<String> s = new ArrayList<String>();
                    s.add(chosenTuple.getFirst());
                    if (!s.equals(subList) || !actions.equals(actionList)){
                        // only add new candidate rule if subject list and action list is different
                        boolean isGeneralized1 = addCandidateRule(s, resList, actionList, cc, uncovUP, batchRule, config, subConditionPathLimit, resConditionPathLimit, numConstraintLimit, isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
                        if (isGeneralized1){
                            stats[2]++;
                        }
                    }
                    if (debugMode){
                        System.out.println("========== Line 10 ==========");
                        System.out.println("Candidate rules");
                        for (Rule r:rules){
                            System.out.println(r);
                        }
                    }
                }
                //System.out.println("Number of rules before merge: " + rules.size());
                stats[0]++;
                mergeRules(batchRule, config, stats, alpha);
//                System.out.println("Done with merge rule");
//                System.out.println("Number of rules after merge: " + rules.size());
            }
            // add all batch rules to the final set rules
            for (LinkedList<Rule> batch: batchRules){
                for (Rule r:batch){
                    rules.add(r);
                }
            }
            // merge everything
            mergeRules(rules, config, stats, alpha);
        }
        else{
            // Line 2 and 3
            // uncovUP containts user-permission tuples in UP0 that are not covered
            // by Rules
            ArrayList<Triple<String, String, String>> uncovUP = new ArrayList<Triple<String, String, String>>(config.getUPList());
            // Line 4 - 11
            Collections.sort(uncovUP, new UPComparator(config));
            while (!uncovUP.isEmpty()){
                Triple<String, String, String> chosenTuple = uncovUP.get(0);
                if (debugMode) {
                    int permCount = 0;
                    for (Pair<String, String> perm:config.getUPListMapOnRes().get(config.getObjectModel().get(chosenTuple.getSecond()).getClass1()).get(chosenTuple.getSecond())){
                        if (perm.getSecond().equals(chosenTuple.getThird())){
                            permCount++;
                        }
                    }
                    
                    int subCount = config.getUPListMapOnSub().get(config.getObjectModel().get(chosenTuple.getFirst()).getClass1()).get(chosenTuple.getFirst()).size();
                    System.out.println("Chosen Tuple Quality: <" + permCount + ", " + subCount +">");
                }
                ArrayList<AtomicConstraint> cc = candidateConstraint(config.getObjectModel().get(chosenTuple.getFirst()),
                        config.getObjectModel().get(chosenTuple.getSecond()),
                        config, subConstraintExtraDistance, resConstraintExtraDistance, totalConstraintLengthPathLimit);
                
                // compute subList: line 6-7
                Pair<String, String> perm = new Pair(chosenTuple.getSecond(), chosenTuple.getThird());
                ArrayList<String> subList = new ArrayList<String>();
                Map<String, Set<Pair<String, String>>> subsWithSameTypeList = config.getUPListMapOnSub().get(config.getObjectModel().get(chosenTuple.getFirst()).getClass1());
                for (String sub:subsWithSameTypeList.keySet()){
                    if (subsWithSameTypeList.get(sub).contains(perm)){
                        ArrayList<AtomicConstraint> cc1 = candidateConstraint(config.getObjectModel().get(sub),
                                config.getObjectModel().get(chosenTuple.getSecond()),
                                config, subConstraintExtraDistance, resConstraintExtraDistance, totalConstraintLengthPathLimit);
                        if (cc.containsAll(cc1) && cc1.containsAll(cc)){
                            subList.add(sub);
                        }
                    }
                }
                // line 8
                ArrayList<String> resList = new ArrayList<String>();
                resList.add(chosenTuple.getSecond());
                Set<String> actions = new HashSet<String>();
                actions.add(chosenTuple.getThird());
                boolean isGeneralized = addCandidateRule(subList, resList, actions, cc, uncovUP, rules, config, subConditionPathLimit, resConditionPathLimit, numConstraintLimit, isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
                if (isGeneralized){
                    stats[2]++;
                }
                if (debugMode){
                    System.out.println("========== Line 8 ==========");
                    System.out.println("Candidate rules");
                    for (Rule r:rules){
                        System.out.println(r);
                    }
                }
                // compute actions list: line 9
                Set<String> actionList = new HashSet<String>();
                for (Pair<String, String> p:config.getUPListMapOnSub().get(config.getObjectModel().get(chosenTuple.getFirst()).getClass1()).get(chosenTuple.getFirst())){
                    if (p.getFirst().equals(chosenTuple.getSecond())){
                        actionList.add(p.getSecond());
                    }
                }
                
                // line 10
                ArrayList<String> s = new ArrayList<String>();
                s.add(chosenTuple.getFirst());
                if (!s.equals(subList) || !actions.equals(actionList)){
                    // only add new candidate rule if subject list and action list is different
                    boolean isGeneralized1 = addCandidateRule(s, resList, actionList, cc, uncovUP, rules, config, subConditionPathLimit, resConditionPathLimit, numConstraintLimit, isOneConditionPerPathRestricted, limitConstraintSizeAll, limitConstraintSizeHalf, removeConditionThreshold, alpha);
                    if (isGeneralized1){
                        stats[2]++;
                    }
                }
                if (debugMode){
                    System.out.println("========== Line 10 ==========");
                    System.out.println("Candidate rules");
                    for (Rule r:rules){
                        System.out.println(r);
                    }
                }
            }
            
            stats[0]++;
            mergeRules(rules, config, stats, alpha);
//            System.out.println("Done with merge rule");
//            System.out.println("Number of rules after merge: " + rules.size());
        }
        while (simplifyRules(rules, config, false, removeConditionThreshold, stats, alpha) && mergeRules(rules, config, stats, alpha)){
            
        }
        // System.out.println("Done with simplify and merge rules loop");
        // Selected highest quality rules
        LinkedList<Rule> resultRules = new LinkedList<Rule>();
        ArrayList<Triple<String, String, String>> uncoveredUP = new ArrayList<Triple<String, String, String>>(config.getUPList());
        
        // System.out.println("Number of rules before merging same rules and remove redundants: " + rules.size());
        // System.out.println("============== RULES BEFORE MERGING SAME RULES AND REMOVE REDU============");
//        for (Rule r: rules){
//            System.out.println(r);
//        }
// merge same rules based on inheritance and remove redundant rules before final selection
        int numMerges = mergeSameRules(rules, config, alpha);
        stats[6] += numMerges;
        while (simplifyRules(rules, config, false, removeConditionThreshold, stats, alpha) && mergeRules(rules, config, stats, alpha)){
            
        }
        stats[7] = removeRedundantRules(rules, config);
//        System.out.println("Number of rules before final selection: " + rules.size());
//        System.out.println("============== RULES BEFORE FINAL SELECTION============");
//        for (Rule r: rules){
//            System.out.println(r);
//        }
        
        LinkedList<Rule> overRules = new LinkedList<Rule>();
        while (!uncoveredUP.isEmpty()){
            for (Rule r: rules){
                r.setQuality(computeRuleQuality(r, uncoveredUP, config));
            }
            Collections.sort(rules, new RuleQualityComparator());
            Rule nextChosenRule = rules.get(0);
            resultRules.add(nextChosenRule);
            
            // add the tuple covered by the rule if the rule quality is less than tau
            if (config.getOverassignmentDetection()){
                if (computeRuleQuality(nextChosenRule, config.getUPList(), config).firstComponent < tau){
                    overRules.add(nextChosenRule);
                }
            }
            rules.remove(nextChosenRule);
            uncoveredUP.removeAll(nextChosenRule.getCoveredUP());
        }
        resultRules.removeAll(overRules);
        HashSet<Triple<String, String, String>> detectedOver = new HashSet<Triple<String, String, String>>(config.getUPList());
        detectedOver.removeAll(computePolicyCoveredUPNoise(resultRules, config));
        config.setDetectedOverassignmentUP(detectedOver);
        
        
        if (config.getUnderassignmentDetection()){
            HashSet<Triple<String, String, String>> detectedUnder = new HashSet<Triple<String, String, String>>(computePolicyCoveredUPNoise(resultRules, config));
            detectedUnder.removeAll(config.getUPList());
            config.setDetectedUnderassignmentUP(detectedUnder);
        }
        return new Pair(resultRules, stats);
    } */
    
    /**
     * this method check if a set of rules covered all the UP tuples from the input and not anything else
     * @param rules
     * @param config
     * @return
     */
    public static boolean sanityCheck(LinkedList<Rule> rules, Config config){
        Set<Triple<String, String, String>> coveredUP = new HashSet<Triple<String, String, String>>();
        for (Rule r:rules){
            r.setCoveredUP(computeCoveredUP(r, config));
            coveredUP.addAll(r.getCoveredUP());
            if (!config.getUPList().containsAll(r.getCoveredUP())){
                System.out.println("Rule that has overassignment: " + r);
                ArrayList<Triple<String, String, String>> ruleOverUP = new ArrayList<Triple<String, String, String>>(r.getCoveredUP());
                ruleOverUP.removeAll(config.getUPList());
                System.out.println("Overassignmnets: ");
                for (Triple<String, String, String>overUP:ruleOverUP){
                    System.out.println(overUP);
                }
            }
        }
        List<Triple<String, String, String>> uncoveredUP = new ArrayList<Triple<String, String, String>>(config.getUPList());
        uncoveredUP.removeAll(coveredUP);
        if (!uncoveredUP.isEmpty()){
            System.out.println("Uncovered UPs: ");
            for (Triple<String, String, String> up:uncoveredUP){
                System.out.println(up);
//                System.out.println("This uncovered tuple is covered by the following input rule: ");
//                for (Rule inputRule:config.getUPRelation().keySet()){
//                    if (config.getUPRelation().get(inputRule).contains(up)){
//                        Set<Triple<String, String, String>> coveredTuples = config.getUPRelation().get(inputRule);
//                        System.out.println(inputRule);
//                    }
//                }
            }
            return false;
        }
        List<Triple<String, String, String>> overUP = new ArrayList<Triple<String, String, String>>(coveredUP);
        overUP.removeAll(config.getUPList());
        if (!overUP.isEmpty()){
            return false;
        }
        return true;
                //coveredUP.containsAll(config.getUPList()) && config.getUPList().containsAll(coveredUP);
    }
    
    public static <T> double setSimilarity(Set<T> s1, Set<T> s2) {
        Set<T> cloneSet1 = new HashSet<T>(s1);
        cloneSet1.retainAll(s2);
        Set<T> cloneSet2 = new HashSet<T>(s1);
        cloneSet2.addAll(s2);
        if (cloneSet2.isEmpty()){
            return 1.0;
        }
        return (double) cloneSet1.size() / cloneSet2.size();
    }
    
    public static double semanticRuleSimilarity(Rule r1, Rule r2){
        return setSimilarity(new HashSet<Triple<String, String, String>>(r1.getCoveredUP()), new HashSet<Triple<String, String, String>>(r2.getCoveredUP()));
    }
    
    public static double syntacticRuleSimilarity(Rule r1, Rule r2, Config config){
        
        double subTypeSimilarity = 0.0;
        double resTypeSimilarity = 0.0;
        
        if (r1.getSubjectType().getClassName().equals(r2.getSubjectType().getClassName())){
            subTypeSimilarity = 1.0;
        }
        
        if (r1.getResourceType().getClassName().equals(r2.getResourceType().getClassName())){
            resTypeSimilarity = 1.0;
        }
        
        double uaeSimilarity = 0.0;
        double raeSimilarity = 0.0;
        
        // get all subject and resource paths
        Set<ArrayList<String>> subjectPaths = new HashSet<ArrayList<String>>();
        Set<ArrayList<String>> resourcePaths = new HashSet<ArrayList<String>>();
        
        for (AtomicCondition ac:r1.getSubjectCondition()){
            subjectPaths.add(ac.getPath());
        }
        for (AtomicCondition ac:r2.getSubjectCondition()){
            subjectPaths.add(ac.getPath());
        }
        for (AtomicCondition ac:r1.getResourceCondition()){
            resourcePaths.add(ac.getPath());
        }
        for (AtomicCondition ac:r2.getResourceCondition()){
            resourcePaths.add(ac.getPath());
        }
        int denominator = subjectPaths.size();
        for (ArrayList<String> subPath : subjectPaths) {
            // look for subPath in c1
            for (AtomicCondition c1 : r1.getSubjectCondition()) {
                if (c1.getPath().equals(subPath)) {
                    for (AtomicCondition c2 : r2.getSubjectCondition()) {
                        if (c2.getPath().equals(subPath)) {
                            // subPath appears in both c1 and c2
                            double negativeSimilarity = c1.getIsNegative()==c2.getIsNegative()?1:0;
                            uaeSimilarity += (1.0 + negativeSimilarity + setSimilarity(c1.getConstant(), c2.getConstant()))/3;
                            break;
                        }
                    }
                }
            }
        }
        if (denominator > 0) {
            uaeSimilarity = uaeSimilarity / denominator;
        } else {
            uaeSimilarity = 1.0;
        }
        
        denominator = resourcePaths.size();
        
        for (ArrayList<String> resPath : resourcePaths) {
            for (AtomicCondition c1 : r1.getResourceCondition()) {
                if (c1.getPath().equals(resPath)) {
                    for (AtomicCondition c2 : r2.getResourceCondition()) {
                        if (c2.getPath().equals(resPath)) {
                            double negativeSimilarity = c1.getIsNegative()==c2.getIsNegative()?1:0;
                            raeSimilarity += (1.0 + negativeSimilarity + setSimilarity(c1.getConstant(), c2.getConstant()))/3;
                            break;
                        }
                    }
                }
            }
        }
        if (denominator > 0) {
            raeSimilarity = raeSimilarity / denominator;
        } else {
            raeSimilarity = 1.0;
        }
        double conSimilarity = setSimilarity(new HashSet<AtomicConstraint>(r1.getConstraint()), new HashSet<AtomicConstraint>(r2.getConstraint()));
        double opsSimilarity = setSimilarity(r1.getActions(), r2.getActions());
        
        return (subTypeSimilarity + resTypeSimilarity + uaeSimilarity + raeSimilarity + conSimilarity + opsSimilarity) / 6;
    }
    
    /**
     *
     * @param rules1
     *            : the first policy
     * @param rules2
     *            : the second policy
     * @param config
     * @return the syntactic similarity of two policies
     */
    public static double policySyntacticSimilarity(
            List<Rule> rules1, List<Rule> rules2, Config config) {
        double similarity = 0.0;
        
        for (Rule r1 : rules1) {
            double maxSimilarity = 0.0;
            for (Rule r2 : rules2) {
                double currentSimilarity = syntacticRuleSimilarity(r1, r2, config);
                if (Double.compare(currentSimilarity, maxSimilarity) > 0) {
                    maxSimilarity = currentSimilarity;
                }
            }
            similarity += maxSimilarity;
        }
        
        return similarity/rules1.size();
    }
    
    public static double policySemanticSimilarity(
            List<Rule> rules1, List<Rule> rules2) {
        double similarity = 0.0;
        
        for (Rule r1 : rules1) {
            double maxSimilarity = 0.0;
            for (Rule r2 : rules2) {
                double currentSimilarity = semanticRuleSimilarity(r1, r2);
                if (Double.compare(currentSimilarity, maxSimilarity) > 0) {
                    maxSimilarity = currentSimilarity;
                }
            }
            similarity += maxSimilarity;
        }
        
        return similarity/rules1.size();
    }
    
    public static Map<Rule, Set<Pair<Rule, Double>>> checkPolicySyntacticSimilarity(List<Rule> outputRules, List<Rule> inputRules, Config config){
        Map<Rule, Set<Pair<Rule, Double>>> results = new HashMap<Rule, Set<Pair<Rule,Double>>>();
        for (Rule outputRule:outputRules){
            Rule mostSimilarInput = null;
            double mostSimilarInputSimilarity = -1.0;
            for (Rule inputRule:inputRules){
                double currentSimilarity = syntacticRuleSimilarity(outputRule, inputRule, config);
                if (Double.compare(currentSimilarity, mostSimilarInputSimilarity) > 0){
                    mostSimilarInput = inputRule;
                    mostSimilarInputSimilarity = currentSimilarity;
                }
            }
            if (!results.containsKey(mostSimilarInput)){
                Set<Pair<Rule, Double>> similarOutputs = new HashSet<Pair<Rule, Double>>();
                similarOutputs.add(new Pair(outputRule, mostSimilarInputSimilarity));
                results.put(mostSimilarInput, similarOutputs);
            }
            else {
                Set<Pair<Rule, Double>> updateSet = new HashSet<Pair<Rule, Double>>(results.get(mostSimilarInput));
                updateSet.add(new Pair(outputRule, mostSimilarInputSimilarity));
                results.put(mostSimilarInput, updateSet);
            }
        }
        for (Rule r: inputRules){
            if (!results.keySet().contains(r)){
                results.put(r, null);
            }
        }
        return results;
    }
    
    public static Map<Rule, Set<Pair<Rule, Double>>> checkPolicySemanticSimilarity(List<Rule> outputRules, List<Rule> inputRules, Config config){
        Map<Rule, Set<Pair<Rule, Double>>> results = new HashMap<Rule, Set<Pair<Rule,Double>>>();
        for (Rule outputRule:outputRules){
            Rule mostSimilarInput = null;
            double mostSimilarInputSimilarity = -1.0;
            for (Rule inputRule:inputRules){
                double currentSimilarity = semanticRuleSimilarity(outputRule, inputRule);
                if (Double.compare(currentSimilarity, mostSimilarInputSimilarity) > 0){
                    mostSimilarInput = inputRule;
                    mostSimilarInputSimilarity = currentSimilarity;
                }
            }
            if (!results.containsKey(mostSimilarInput)){
                Set<Pair<Rule, Double>> similarOutputs = new HashSet<Pair<Rule, Double>>();
                similarOutputs.add(new Pair(outputRule, mostSimilarInputSimilarity));
                results.put(mostSimilarInput, similarOutputs);
            }
            else {
                Set<Pair<Rule, Double>> updateSet = new HashSet<Pair<Rule, Double>>(results.get(mostSimilarInput));
                updateSet.add(new Pair(outputRule, mostSimilarInputSimilarity));
                results.put(mostSimilarInput, updateSet);
            }
        }
        for (Rule r: inputRules){
            if (!results.keySet().contains(r)){
                results.put(r, null);
            }
        }
        return results;
    }
    
    public static void readConfiguration(String configFile, Config config){
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
            
            // read numConstraintLimit
            int numConstraintLimit = Integer.parseInt(inputProperties.getProperty("numConstraintLimit"));
            config.setNumConstraintLimit(numConstraintLimit);
            
            // read removeConditionThreshold
            int removeConditionThreshold = Integer.parseInt(inputProperties.getProperty("removeConditionThreshold"));
            config.setRemoveConditionThreshold(removeConditionThreshold);
            
            // read limitConstraintSizeAll
            int limitConstraintSizeAll = Integer.parseInt(inputProperties.getProperty("limitConstraintSizeAll"));
            config.setLimitConstraintSizeAll(limitConstraintSizeAll);
            
            // read limitConstraintSizeHalf
            int limitConstraintSizeHalf = Integer.parseInt(inputProperties.getProperty("limitConstraintSizeHalf"));
            config.setLimitConstraintSizeHalf(limitConstraintSizeHalf);
            
            // read numPoliciesPerSize
            int numPoliciesPerSize = Integer.parseInt(inputProperties.getProperty("numPoliciesPerSize"));
            config.setNumPoliciesPerSize(numPoliciesPerSize);
			
			// read runPolicy
            int runPolicy = Integer.parseInt(inputProperties.getProperty("runPolicy"));
            config.setRunPolicy(runPolicy);
            
            // read isOneConditionPerPathRestricted
            String isOneConditionPerPathRestricted = inputProperties.getProperty("isOneConditionPerPathRestricted");
            config.setIsOneConditionPerPathRestricted(isOneConditionPerPathRestricted.equals("true"));
            
            // read compareCoveredUPNum
            String compareCoveredUPNum = inputProperties.getProperty("compareCoveredUPNum");
            config.setCompareCoveredUPNum(compareCoveredUPNum.equals("true"));
            
            // read compareOriginalInputRule
            String compareOriginalInputRule = inputProperties.getProperty("compareOriginalInputRule");
            config.setCompareOriginalInputRule(compareOriginalInputRule.equals("true"));
            
            // read policySize
            String policySize = inputProperties.getProperty("sizes");
            policySize = policySize.substring(1, policySize.length() -1);
            String[] policySizes = policySize.split(",");
            int[] sizes = new int[policySizes.length];
            for (int i = 0; i < policySizes.length; i++){
                sizes[i] = Integer.parseInt(policySizes[i]);
            }
            config.setPolicySize(sizes);
            
            // read policyName
            String policyName = inputProperties.getProperty("policyName");
            config.setPolicyName(policyName);
            
            // read attributeDataPath
            String attributeDataPath = inputProperties.getProperty("attributeDataPath");
            config.setAttributeDataPath(attributeDataPath);
            
            // read minedRulesFromDTPath
            String minedRulesFromDTPath = inputProperties.getProperty("minedRulesFromDTPath");
            config.setMinedRulesFromDTPath(minedRulesFromDTPath);
            
            // read outputPath
            String outputPath = inputProperties.getProperty("outputPath");
            config.setOutputPath(outputPath);
			
			// read outputFile
            String outputFile = inputProperties.getProperty("outputFile");
            config.setOutputFile(outputFile);
            
            // read overassignmentDetection
            String overassignmentDetection = inputProperties.getProperty("overassignmentDetection");
            config.setOverassignmentDetection(overassignmentDetection.equals("true"));
            
            // read overassignmentDetection
            String underassignmentDetection = inputProperties.getProperty("underassignmentDetection");
            config.setUnderassignmentDetection(underassignmentDetection.equals("true"));
            
            // read taus
            if (config.getOverassignmentDetection()){
                String tauString = inputProperties.getProperty("tau");
                tauString = tauString.substring(1, tauString.length() -1);
                String[] tauStrings = tauString.split(",");
                double[] taus = new double[tauStrings.length];
                for (int i = 0; i < tauStrings.length; i++){
                    taus[i] = Double.parseDouble(tauStrings[i]);
                }
                config.setTaus(taus);
            }
            
            // read alphas
            if (config.getUnderassignmentDetection()){
                String alphaString = inputProperties.getProperty("alpha");
                alphaString = alphaString.substring(1, alphaString.length() -1);
                String[] alphaStrings = alphaString.split(",");
                double[] alphas = new double[alphaStrings.length];
                for (int i = 0; i < alphaStrings.length; i++){
                    alphas[i] = Double.parseDouble(alphaStrings[i]);
                }
                config.setAlphas(alphas);
            }
            
            // read noise levels
            //double noiseLevel = Double.parseDouble(inputProperties.getProperty("noiseLevel"));
            //config.setNoiseLevel(noiseLevel);
            
            if (config.getOverassignmentDetection() || config.getUnderassignmentDetection()){
                // read noiseDataFileInputPath
                String noiseDataFileInputPath = inputProperties.getProperty("noiseDataFileInputPath");
                config.setNoiseDataFileInputPath(noiseDataFileInputPath);
                
                // read noiseLevels
                String noiseLevelStr = inputProperties.getProperty("noiseLevel");
                noiseLevelStr = noiseLevelStr.substring(1, noiseLevelStr.length() -1);
                String[] noiseLevelStrArray = noiseLevelStr.split(",");
                double[] noiseLevelArray = new double[noiseLevelStrArray.length];
                for (int i = 0; i < noiseLevelArray.length; i++) {
                    noiseLevelArray[i] = Double.parseDouble(noiseLevelStrArray[i]);
                }
                config.setNoiseLevels(noiseLevelArray);
            }
            // read batchOptimization
            String batchOptimization = inputProperties.getProperty("batchOptimization");
            config.setBatchOptimization(batchOptimization.equals("true"));
            
            // read batch size
            int batchSize = Integer.parseInt(inputProperties.getProperty("batchSize"));
            config.setBatchSize(batchSize);
        }
        catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * add noisePercentage * |up0| numbers of over-assignment noise
     *
     * @param config
     * @param noiseLevel
     */
    public static void addOverassignmentNoise(Config config,
            double noiseLevel) {
        int numNoise = (int) ((5 * config.getUPList().size() * noiseLevel)/6);
        
        while (config.getOverassignmentUP().size() < numNoise) {
            ArrayList<String> ops = new ArrayList<String>();
            ArrayList<String> subs = new ArrayList<String>();
            ArrayList<String> ress = new ArrayList<String>();
            for (Triple<String, String, String> up: config.getUPList()){
                if (!subs.contains(up.getFirst())){
                    subs.add(up.getFirst());
                }
                
                if (!ress.contains(up.getSecond())){
                    ress.add(up.getSecond());
                }
                
                if (!ops.contains(up.getThird())){
                    ops.add(up.getThird());
                }
                
            }
            
            DiscreteNormalDistribution subDist = new DiscreteNormalDistribution(0, subs.size() - 1, subs.size()/2.0, subs.size()/2.0);
            DiscreteNormalDistribution resDist = new DiscreteNormalDistribution(0, ress.size() - 1, ress.size()/2.0, ress.size()/2.0);
            DiscreteNormalDistribution opDist = new DiscreteNormalDistribution(0, ops.size() - 1, ops.size()/2.0, ops.size()/2.0);
            
            String sub = subs.get(subDist.getNextDistVal());
            String res = ress.get(resDist.getNextDistVal());
            String op = ops.get(opDist.getNextDistVal());
            Triple<String, String, String> up = new Triple<String, String, String>(
                    sub, res, op);
            if (!config.getUPList().contains(up)) {
                config.getOverassignmentUP().add(up);
                config.getUPList().add(up);
                Parser.computeUPMaps(config);
            }
        }
    }
    
    /**
     * add noisePercentage * |up0| numbers of under-assignment noise
     *
     * @param config
     * @param noiseLevel
     */
    public static void addUnderassignmentNoise(Config config,
            double noiseLevel) {
        int numNoise = (int) ((config.getUPList().size() * noiseLevel)/6);
        
        while (config.getUnderassignmentUP().size() < numNoise
                && !config.getUPList().isEmpty()) {
            Set<Triple<String, String, String>> upSet = new HashSet<Triple<String, String, String>>(config.getUPList());
            Triple<String, String, String> up = randomElement(upSet);
            config.getUnderassignmentUP().add(up);
            config.getUPList().remove(up);
            Parser.computeUPMaps(config);
        }
    }
    
    /**
     * Pick a randomeElement from a set S
     *
     * @param S
     * @return
     */
    public static <T> T randomElement(Set<T> S) {
        if (S == null) {
            return null;
        }
        int size = S.size();
        int item = new Random(System.currentTimeMillis()).nextInt(size);
        int i = 0;
        T result = null;
        for (T obj : S) {
            if (i == item) {
                result = obj;
                break;
            }
            i = i + 1;
        }
        return result;
    }
    
    public static void printConfig(Config config, BufferedWriter outputWrite){
        try{
            outputWrite.write("subConstraintExtraDistance=" + config.getSubConstraintExtraDistance() + "\n");
            outputWrite.write("resConstraintExtraDistance=" + config.getResConstraintExtraDistance() + "\n");
            outputWrite.write("totalConstraintLengthPathLimit=" + config.getTotalConstraintLengthPathLimit()+ "\n");
            outputWrite.write("subConditionPathLimit=" + config.getSubConditionPathLimit()+ "\n");
            outputWrite.write("resConditionPathLimit=" + config.getResConditionPathLimit() + "\n");
            outputWrite.write("numConstraintLimit=" + config.getNumConstraintLimit() + "\n");
            outputWrite.write("removeConditionThreshold=" + config.getRemoveConditionThreshold() + "\n");
            outputWrite.write("limitConstraintSizeAll=" + config.getLimitConstraintSizeAll() + "\n");
            outputWrite.write("limitConstraintSizeHalf=" + config.getLimitConstraintSizeHalf() + "\n");
            outputWrite.write("isOneConditionPerPathRestricted=" + config.getIsOneConditionPerPathRestricted() + "\n");
            outputWrite.write("compareCoveredUPNum=" + config.getCompareCoveredUPNum() + "\n");
            outputWrite.write("compareOriginalInputRule=" + config.getCompareOriginalInputRule() + "\n");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public static void printStats(int[] stats, BufferedWriter outputWrite){
        try{
            outputWrite.write("Number of calls to mergeRules: " + stats[0] + "\n");
            outputWrite.write("Number of merges: " + stats[1]+ "\n");
            outputWrite.write("Number of generalizations: " + stats[2] + "\n");
            outputWrite.write("Number of eliminated conjuncts: " + stats[3] + "\n");
            outputWrite.write("Number of eliminated constraints: " + stats[4] + "\n");
            outputWrite.write("Number of eliminated operations: " + stats[5] + "\n");
            outputWrite.write("Number of merges to parent object rule: " + stats[6] + "\n");
            outputWrite.write("Number of removed redundant rules: : " + stats[7] + "\n");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public static void printRuleListWithUP(Config config, BufferedWriter outputWrite){
        try {
            for (Pair<Integer, Rule> entry:config.getRuleListWithUP()){
                outputWrite.write(entry + "\n");
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public static void computeClassAttrDomainSizes(Config config){
        Map<String, Object1> objectModel = config.getObjectModel();
        Map<String, Set<String>> classAttrDomainSizes = config.getClassAttrDomainSizes();
        for (String key:objectModel.keySet()){
            Object1 object = objectModel.get(key);
            if (classAttrDomainSizes.get(object.getClass1()) == null){
                Set<String> newValueSet = new HashSet<String>();
                newValueSet.add(object.getId());
                classAttrDomainSizes.put(object.getClass1(), newValueSet);
            }
            else{
                Set<String> newValueSet = new HashSet<String>(classAttrDomainSizes.get(object.getClass1()));
                newValueSet.add(object.getId());
                classAttrDomainSizes.put(object.getClass1(), newValueSet);
            }
        }
        
        // Now compute the attribute domain for parent class
        Map<String, Set<String>> parentClassDomain = new HashMap<String, Set<String>>();
        for (String key:config.getClassAttrDomainSizes().keySet()){
            if (config.getClassModel().get(key).getParentClass() != null){
                if (parentClassDomain.get(config.getClassModel().get(key).getParentClass().getClassName()) == null){
                    Set<String> newValueSet = new HashSet<String>(classAttrDomainSizes.get(key));
                    parentClassDomain.put(config.getClassModel().get(key).getParentClass().getClassName(), newValueSet);
                }
                else{
                    Set<String> newValueSet = new HashSet<String>(parentClassDomain.get(config.getClassModel().get(key).getParentClass().getClassName()));
                    newValueSet.addAll(classAttrDomainSizes.get(key));
                    parentClassDomain.put(config.getClassModel().get(key).getParentClass().getClassName(), newValueSet);
                }
            }
        }
        config.getClassAttrDomainSizes().putAll(parentClassDomain);
    }
    /*public static Pair<Long, double[]> runOneExperiment(Config config, String configFile, int size, int j, String outputPath, String noiseDataPath, boolean noiseDetection, double tau, double alpha, boolean batchOptimization, int batchSize){
        double[] statsInfo = new double[15];
        long totalCPUTime = 0;
        try{
            config = new Config();
            readConfiguration(configFile, config);
            FileWriter fileWriter = new FileWriter(outputPath);
            BufferedWriter outputWrite = new BufferedWriter(fileWriter);
            long startCPUTime = Time.getCpuTime();
            String fileName = config.getAttributeDataPath() + config.getPolicyName() + "_" + size + "_" + j + ".abac_txt";
            outputWrite.write("RUNNING EXPERIMENTS ON: "  + config.getPolicyName() + "_" + size + "_" + j + "\n\n");
            outputWrite.write("============== NUMBER OF UP TUPLES COVERED BY EACH RULE: ==============\n");
            Parser.parseInputFile(fileName, config);
            printRuleListWithUP(config, outputWrite);
            
            // add overassignment and underassigment if running noise detection experiments
            if (noiseDetection){
                //addUnderassignmentNoise(config, config.getNoiseLevel());
                //addOverassignmentNoise(config, config.getNoiseLevel());
                Parser.parseNoiseDataFile(noiseDataPath, config);
            }
            // echo all configurations
            outputWrite.write("============== CONFIGURATIONS ==============\n");
            printConfig(config, outputWrite);
            
            computeClassAttrDomainSizes(config);
            Pair<LinkedList<Rule>, int[]> output = ReBACMiner.mineReBACPolicy(config, false, config.getSubConstraintExtraDistance(), config.getResConstraintExtraDistance(), config.getTotalConstraintLengthPathLimit(), config.getSubConditionPathLimit(), config.getResConditionPathLimit(), config.getNumConstraintLimit(), config.getIsOneConditionPerPathRestricted(), config.getRemoveConditionThreshold(), config.getLimitConstraintSizeAll(), config.getLimitConstraintSizeHalf(), tau, alpha, config.getBatchOptimization(), config.getBatchSize());
            LinkedList<Rule> outputRules = output.getFirst();
            int[] stats = output.getSecond();
            totalCPUTime = Time.getCpuTime() - startCPUTime;
            long totalCPUTimeMillis = (long) (totalCPUTime * 0.000001);
            String runningTime = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(totalCPUTimeMillis),
                    TimeUnit.MILLISECONDS.toSeconds(totalCPUTimeMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalCPUTimeMillis))
            );
            outputWrite.write("============== FINAL MINED RULES ==============\n");
            printPolicy(outputRules, config, outputWrite);
            
            outputWrite.write("Number of final rules: " + outputRules.size() + "\n");
            if (!ReBACMiner.sanityCheck(outputRules, config)){
                System.out.println("FAIL SANITY CHECK");
                System.exit(0);
            }
            outputWrite.write("============== ORIGINAL INPUT RULES ==============\n");
            LinkedList<Rule> originalInputRules = new LinkedList<Rule>();
            originalInputRules.addAll(config.getRuleModel());
            
            printPolicy(originalInputRules, config, outputWrite);
            
            LinkedList<Rule> inputRules = new LinkedList<Rule>(config.getRuleModel());
            if (noiseDetection){
                outputWrite.write("============== COMPARISON STATS ==============\n");
                double overSimilarity = setSimilarity(config.getDetectedOverassignmentUP(), config.getOverassignmentUP());
                statsInfo[12] = overSimilarity;
                outputWrite.write("Over-assignment jaccard: " + overSimilarity + "\n");
                System.out.println();
                System.out.println("Over-assignment jaccard: " + overSimilarity);
                
                double underSimilarity = setSimilarity(config.getDetectedUnderassignmentUP(), config.getUnderassignmentUP());
                statsInfo[13] = underSimilarity;
                outputWrite.write("Under-assignment jaccard: " + underSimilarity + "\n");
                System.out.println();
                System.out.println("Under-assignment jaccard: " + underSimilarity);
                
                int count = 1;
                Map<Rule, Set<Pair<Rule, Double>>> compareSyntacticResult = checkPolicySyntacticSimilarity(outputRules, inputRules, config);
                Map<Rule, Set<Pair<Rule, Double>>> compareSemanticResult = checkPolicySemanticSimilarity(outputRules, inputRules, config);
                double countSameRule = 0;
                outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES SYNTACTICALLY ==============\n");
                for (Rule key :compareSyntacticResult.keySet()){
                    outputWrite.write(count + ")\n");
                    outputWrite.write("Input rule: \n" + key + "\n");
                    if (compareSyntacticResult.get(key) != null){
                        outputWrite.write("Similar output rules: \n");
                        for (Pair<Rule, Double> op:compareSyntacticResult.get(key)){
                            outputWrite.write(op.getFirst() + "\n");
                            outputWrite.write("Syntactic Similarity: " + op.getSecond() + "\n\n");
                            if (op.getSecond() == 1.0){
                                countSameRule++;
                            }
                        }
                    }
                    else {
                        outputWrite.write("\n\n");
                    }
                    count++;
                }
                count = 1;
                outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES SEMANTICALLY ==============\n");
                for (Rule key :compareSemanticResult.keySet()){
                    outputWrite.write(count + ")\n");
                    outputWrite.write("Input rule: \n" + key + "\n");
                    if (compareSemanticResult.get(key) != null){
                        outputWrite.write("Similar output rules: \n");
                        for (Pair<Rule, Double> op:compareSemanticResult.get(key)){
                            outputWrite.write(op.getFirst() + "\n");
                            outputWrite.write("Semantic Similarity: " + op.getSecond() + "\n\n");
                        }
                    }
                    else{
                        outputWrite.write("\n\n");
                    }
                    count++;
                }
                
                Map<Rule, Set<Pair<Rule, Double>>> compareOriginalSyntacticResult = checkPolicySyntacticSimilarity(outputRules, originalInputRules, config);
                double countOriginalSameRule = 0;
                for (Rule key :compareOriginalSyntacticResult.keySet()){
                    if (compareOriginalSyntacticResult.get(key) != null){
                        for (Pair<Rule, Double> op:compareOriginalSyntacticResult.get(key)){
                            if (op.getSecond() == 1.0){
                                countOriginalSameRule++;
                            }
                        }
                    }
                }
                
                
                double semSimilar = setSimilarity(computePolicyCoveredUPNoise(outputRules, config), computePolicyCoveredUPNoise(originalInputRules, config));
                outputWrite.write("\n2) Rule-wise Semantic Similarity(Compare with original input rules): " + semSimilar + "\n");
                statsInfo[14] = semSimilar;
                System.out.println();
                System.out.println("rule-wise semantic similarity: " + semSimilar);
                
                outputWrite.write("============== RUNNING TIME ==============\n");
                outputWrite.write("Mining running time: " + runningTime + "\n");
                outputWrite.write("\n==========================================================================================\n==========================================================================================");
                
                
//                System.out.println("Output rules: ");
//                for (Rule r: outputRules){
//                    System.out.println(r);
//                }
                
//                System.out.println("Original Input rules: ");
//                for (Rule r: originalInputRules){
//                    System.out.println(r);
//                }
//                System.out.println("Syntactic policy similarity: " + policySyntacticSimilarity(outputRules, originalInputRules, config));
            }
            
            
            
            
            
            
            
            
            
            else {
                outputWrite.write("============== SIMPLIFIED AND MERGED INPUT RULES ==============\n");
                for (Rule r: inputRules){
                    r.setQuality(ReBACMiner.computeRuleQuality(r, config.getUPList(), config));
                }
                mergeRules(inputRules, config, new int[2], 0.0);
                
                while (simplifyRules(inputRules, config, false, config.getRemoveConditionThreshold(), stats, 0.0) && mergeRules(inputRules, config, new int[2], 0.0)){
                }
                LinkedList<Rule> simplifiedInputRules = new LinkedList<Rule>();
                ArrayList<Triple<String, String, String>> uncoveredUP = new ArrayList<Triple<String, String, String>>(config.getUPList());
                while (!uncoveredUP.isEmpty()){
                    for (Rule r: inputRules){
                        r.setQuality(computeRuleQuality(r, uncoveredUP, config));
                    }
                    Collections.sort(inputRules, new RuleQualityComparator());
                    Rule nextChosenRule = inputRules.get(0);
                    simplifiedInputRules.add(nextChosenRule);
                    inputRules.remove(nextChosenRule);
                    uncoveredUP.removeAll(nextChosenRule.getCoveredUP());
                }
                printPolicy(simplifiedInputRules, config, outputWrite);
                
                if (!ReBACMiner.sanityCheck(simplifiedInputRules, config)){
                    System.out.println("!!!!!!!!!!!!! INPUT ERROR !!!!!!!!!!!!!!!!");
                    System.exit(0);
                }
                outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES ==============\n");
                outputWrite.write("Number of output rules: " + outputRules.size() + "\n");
                if (config.getCompareOriginalInputRule()) {
                    outputWrite.write("Number of original input rules: " + originalInputRules.size() + "\n");
                } else {
                    outputWrite.write("Number of simplified and merged input rules: " + simplifiedInputRules.size() + "\n");
                }
                outputWrite.write("---------------------\n");
                LinkedList<Rule> compareRules = null;
                if (config.getCompareOriginalInputRule()) {
                    compareRules = originalInputRules;
                } else {
                    compareRules = simplifiedInputRules;
                }
                
                Map<Rule, Set<Pair<Rule, Double>>> compareSyntacticResult = checkPolicySyntacticSimilarity(outputRules, compareRules, config);
                Map<Rule, Set<Pair<Rule, Double>>> compareSemanticResult = checkPolicySemanticSimilarity(outputRules, compareRules, config);
                int count = 1;
                double countSameRule = 0;
                outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES SYNTACTICALLY ==============\n");
                for (Rule key :compareSyntacticResult.keySet()){
                    outputWrite.write(count + ")\n");
                    outputWrite.write("Input rule: \n" + key + "\n");
                    if (compareSyntacticResult.get(key) != null){
                        outputWrite.write("Similar output rules: \n");
                        for (Pair<Rule, Double> op:compareSyntacticResult.get(key)){
                            outputWrite.write(op.getFirst() + "\n");
                            outputWrite.write("Syntactic Similarity: " + op.getSecond() + "\n\n");
                            if (op.getSecond() == 1.0){
                                countSameRule++;
                            }
                        }
                    }
                    else {
                        outputWrite.write("\n\n");
                    }
                    count++;
                }
                
                
                count = 1;
                outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES SEMANTICALLY ==============\n");
                for (Rule key :compareSemanticResult.keySet()){
                    outputWrite.write(count + ")\n");
                    outputWrite.write("Input rule: \n" + key + "\n");
                    if (compareSemanticResult.get(key) != null){
                        outputWrite.write("Similar output rules: \n");
                        for (Pair<Rule, Double> op:compareSemanticResult.get(key)){
                            outputWrite.write(op.getFirst() + "\n");
                            outputWrite.write("Semantic Similarity: " + op.getSecond() + "\n\n");
                        }
                    }
                    else{
                        outputWrite.write("\n\n");
                    }
                    count++;
                }
                
                Map<Rule, Set<Pair<Rule, Double>>> compareOriginalSyntacticResult = checkPolicySyntacticSimilarity(outputRules, originalInputRules, config);
                double countOriginalSameRule = 0;
                for (Rule key :compareOriginalSyntacticResult.keySet()){
                    if (compareOriginalSyntacticResult.get(key) != null){
                        for (Pair<Rule, Double> op:compareOriginalSyntacticResult.get(key)){
                            if (op.getSecond() == 1.0){
                                countOriginalSameRule++;
                            }
                        }
                    }
                }
                
                outputWrite.write("============== COMPARISON STATS ==============\n");
                outputWrite.write("1) WSC: \n");
                int minedPolicyWSC = computePolicyWSC(outputRules);
                outputWrite.write("Mined Policy WSC: " + minedPolicyWSC + "\n");
                statsInfo[0] = minedPolicyWSC;
                
                int simplifiedInputPolicyWSC = computePolicyWSC(compareRules);
                outputWrite.write("Simplified Input Policy WSC: " + simplifiedInputPolicyWSC + "\n");
                statsInfo[1] = simplifiedInputPolicyWSC;
                
                int originalInputPolicyWSC = computePolicyWSC(originalInputRules);
                outputWrite.write("Original Input Policy WSC: " + originalInputPolicyWSC + "\n");
                statsInfo[5] = originalInputPolicyWSC;
                
                double synSimilar = policySyntacticSimilarity(outputRules, compareRules, config);
                outputWrite.write("\n Policy Syntactic Similarity(Compare with simplified input rules): " + synSimilar + "\n");
                statsInfo[2] = synSimilar;
                
                double semSimilar = policySemanticSimilarity(outputRules, compareRules);
                outputWrite.write("\n Rule-wise Semantic Similarity(Compare with simplified input rules): " + semSimilar + "\n");
                statsInfo[3] = semSimilar;
                
                synSimilar = policySyntacticSimilarity(outputRules, originalInputRules, config);
                outputWrite.write("\n Policy Syntactic Similarity(Compare with original input rules): " + synSimilar + "\n");
                statsInfo[6] = synSimilar;
                
                semSimilar = policySemanticSimilarity(outputRules, originalInputRules);
                outputWrite.write("\n Rule-wise Semantic Similarity(Compare with original input rules): " + semSimilar + "\n");
                statsInfo[7] = semSimilar;
                
                double sameFrac = countSameRule/compareRules.size();
                outputWrite.write("\n Fraction of Rules in Both Simplified Input and Mined Policy: " + sameFrac + "\n");
                statsInfo[4] = sameFrac;
                
                double sameOriginalFrac = countOriginalSameRule/originalInputRules.size();
                outputWrite.write("\n Fraction of Rules in Both Original Input and Mined Policy: " + sameOriginalFrac + "\n");
                statsInfo[8] = sameOriginalFrac;
                
                outputWrite.write("============== MINING ALGORITHM STATS ==============\n");
                printStats(stats, outputWrite);
                
                outputWrite.write("============== OBJECT MODEL STATS ==============\n");
                int tupleSize = config.getUPList().size();
                outputWrite.write("Total tuple size: " + tupleSize + "\n");
                statsInfo[9] = tupleSize;
                
                int objectSize = config.getObjectModel().size();
                outputWrite.write("Total object size: " + objectSize + "\n");
                statsInfo[10] = objectSize;
                
                int fieldNum = config.getTotalFieldNum();
                outputWrite.write("Total field number: " + fieldNum + "\n");
                statsInfo[11] = fieldNum;
                
                outputWrite.write("============== RUNNING TIME ==============\n");
                outputWrite.write("Mining running time: " + runningTime + "\n");
                outputWrite.write("\n==========================================================================================\n==========================================================================================");
            }
            outputWrite.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return new Pair(totalCPUTime, statsInfo);
    }*/
    
    public static Pair<Long, double[]> runOneDecisionTreeExperiment(Config config, String configFile, int size, int j, String outputPath){
        double[] statsInfo = new double[15];
        long totalCPUTime = 0;
        try{
            config = new Config();
            readConfiguration(configFile, config);
            FileWriter fileWriter = new FileWriter(outputPath);
            BufferedWriter outputWrite = new BufferedWriter(fileWriter);
            long startCPUTime = Time.getCpuTime();
            String attrFileName = config.getAttributeDataPath() + config.getPolicyName() + "_" + size + "_" + j + ".abac_txt";
            outputWrite.write("RUNNING EXPERIMENTS ON: "  + config.getPolicyName() + "_" + size + "_" + j + "\n\n");
            outputWrite.write("============== NUMBER OF UP TUPLES COVERED BY EACH RULE: ==============\n");
            Parser.parseInputFile(attrFileName, config);
            String minedRulesDTFileName = config.getMinedRulesFromDTPath() + config.getPolicyName() + "_" + size + "_" + j + ".rules";
            Parser.parseInputRulesFile(minedRulesDTFileName, config, config.getMinedRulesFromDT());
            printRuleListWithUP(config, outputWrite);
            System.out.println(config.getUPList().size());
            
            // echo all configurations
            outputWrite.write("============== CONFIGURATIONS ==============\n");
            printConfig(config, outputWrite);
            
            computeClassAttrDomainSizes(config);
            
            LinkedList<Rule> outputRules = config.getMinedRulesFromDT();

            for (Rule r:outputRules){
                r.setCoveredUP(computeCoveredUP(r, config));
            }
            for (Rule r:outputRules) {
                r.setQuality(computeRuleQuality(r, config.getUPList(), config));
            }
            if (!ReBACMiner.sanityCheck(outputRules, config)){
                System.out.println("OUTPUT POLICY FAIL SANITY CHECK BEFORE SIMPLIFICATION");
                System.exit(0);
            }
            
            mergeRules(outputRules, config, new int[2], 0.0);
            simplifyRules(outputRules, config, false, config.getRemoveConditionThreshold(), new int[10], 0.0);
//            while (simplifyRules(outputRules, config, false, config.getRemoveConditionThreshold(), new int[10], 0.0) && mergeRules(outputRules, config, new int[2], 0.0)){
//            }

            
            totalCPUTime = Time.getCpuTime() - startCPUTime;
            long totalCPUTimeMillis = (long) (totalCPUTime * 0.000001);
            String runningTime = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(totalCPUTimeMillis),
                    TimeUnit.MILLISECONDS.toSeconds(totalCPUTimeMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalCPUTimeMillis))
            );
            outputWrite.write("============== FINAL MINED RULES ==============\n");
            printPolicy(outputRules, config, outputWrite);
            
            outputWrite.write("Number of final rules: " + outputRules.size() + "\n");
            if (!ReBACMiner.sanityCheck(outputRules, config)){
                System.out.println("OUTPUT POLICY FAIL SANITY CHECK AFTER SIMPLIFICATION");
                System.exit(0);
            }
            outputWrite.write("============== ORIGINAL INPUT RULES ==============\n");
            LinkedList<Rule> originalInputRules = new LinkedList<Rule>();
            originalInputRules.addAll(config.getRuleModel());
            
            printPolicy(originalInputRules, config, outputWrite);
            
            LinkedList<Rule> inputRules = new LinkedList<Rule>(config.getRuleModel());
            
            outputWrite.write("============== SIMPLIFIED AND MERGED INPUT RULES ==============\n");
            for (Rule r: inputRules){
                r.setQuality(ReBACMiner.computeRuleQuality(r, config.getUPList(), config));
            }
            mergeRules(inputRules, config, new int[2], 0.0);

            while (simplifyRules(inputRules, config, false, config.getRemoveConditionThreshold(), new int[10], 0.0) && mergeRules(inputRules, config, new int[2], 0.0)){
            }
            simplifyRules(inputRules, config, false, config.getRemoveConditionThreshold(), new int[10], 0.0);
            LinkedList<Rule> simplifiedInputRules = inputRules;
            printPolicy(simplifiedInputRules, config, outputWrite);
            
            if (!ReBACMiner.sanityCheck(simplifiedInputRules, config)){
                System.out.println("!!!!!!!!!!!!! INPUT ERROR !!!!!!!!!!!!!!!!");
                System.exit(0);
            }
            outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES ==============\n");
            outputWrite.write("Number of output rules: " + outputRules.size() + "\n");
            if (config.getCompareOriginalInputRule()) {
                outputWrite.write("Number of original input rules: " + originalInputRules.size() + "\n");
            } else {
                outputWrite.write("Number of simplified and merged input rules: " + simplifiedInputRules.size() + "\n");
            }
            outputWrite.write("---------------------\n");
            LinkedList<Rule> compareRules = null;
            if (config.getCompareOriginalInputRule()) {
                compareRules = originalInputRules;
            } else {
                compareRules = simplifiedInputRules;
            }
            
            Map<Rule, Set<Pair<Rule, Double>>> compareSyntacticResult = checkPolicySyntacticSimilarity(outputRules, compareRules, config);
            Map<Rule, Set<Pair<Rule, Double>>> compareSemanticResult = checkPolicySemanticSimilarity(outputRules, compareRules, config);
            int count = 1;
            double countSameRule = 0;
            outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES SYNTACTICALLY ==============\n");
            for (Rule key :compareSyntacticResult.keySet()){
                outputWrite.write(count + ")\n");
                outputWrite.write("Input rule: \n" + key + "\n");
                if (compareSyntacticResult.get(key) != null){
                    outputWrite.write("Similar output rules: \n");
                    for (Pair<Rule, Double> op:compareSyntacticResult.get(key)){
                        outputWrite.write(op.getFirst() + "\n");
                        outputWrite.write("Syntactic Similarity: " + op.getSecond() + "\n\n");
                        if (op.getSecond() == 1.0){
                            countSameRule++;
                        }
                    }
                }
                else {
                    outputWrite.write("\n\n");
                }
                count++;
            }
            
            
            count = 1;
            outputWrite.write("============== COMPARE OUTPUT AND INPUT RULES SEMANTICALLY ==============\n");
            for (Rule key :compareSemanticResult.keySet()){
                outputWrite.write(count + ")\n");
                outputWrite.write("Input rule: \n" + key + "\n");
                if (compareSemanticResult.get(key) != null){
                    outputWrite.write("Similar output rules: \n");
                    for (Pair<Rule, Double> op:compareSemanticResult.get(key)){
                        outputWrite.write(op.getFirst() + "\n");
                        outputWrite.write("Semantic Similarity: " + op.getSecond() + "\n\n");
                    }
                }
                else{
                    outputWrite.write("\n\n");
                }
                count++;
            }
            
            Map<Rule, Set<Pair<Rule, Double>>> compareOriginalSyntacticResult = checkPolicySyntacticSimilarity(outputRules, originalInputRules, config);
            double countOriginalSameRule = 0;
            for (Rule key :compareOriginalSyntacticResult.keySet()){
                if (compareOriginalSyntacticResult.get(key) != null){
                    for (Pair<Rule, Double> op:compareOriginalSyntacticResult.get(key)){
                        if (op.getSecond() == 1.0){
                            countOriginalSameRule++;
                        }
                    }
                }
            }
            
            outputWrite.write("============== COMPARISON STATS ==============\n");
            outputWrite.write("1) WSC: \n");
            int minedPolicyWSC = computePolicyWSC(outputRules);
            outputWrite.write("Mined Policy WSC: " + minedPolicyWSC + "\n");
            statsInfo[0] = minedPolicyWSC;
            
            int simplifiedInputPolicyWSC = computePolicyWSC(compareRules);
            outputWrite.write("Simplified Input Policy WSC: " + simplifiedInputPolicyWSC + "\n");
            statsInfo[1] = simplifiedInputPolicyWSC;
            
            int originalInputPolicyWSC = computePolicyWSC(originalInputRules);
            outputWrite.write("Original Input Policy WSC: " + originalInputPolicyWSC + "\n");
            statsInfo[5] = originalInputPolicyWSC;
            
            double synSimilar = policySyntacticSimilarity(outputRules, compareRules, config);
            outputWrite.write("\n Policy Syntactic Similarity(Compare with simplified input rules): " + synSimilar + "\n");
            statsInfo[2] = synSimilar;
            
            double semSimilar = policySemanticSimilarity(outputRules, compareRules);
            outputWrite.write("\n Rule-wise Semantic Similarity(Compare with simplified input rules): " + semSimilar + "\n");
            statsInfo[3] = semSimilar;
            
            synSimilar = policySyntacticSimilarity(outputRules, originalInputRules, config);
            outputWrite.write("\n Policy Syntactic Similarity(Compare with original input rules): " + synSimilar + "\n");
            statsInfo[6] = synSimilar;
            
            semSimilar = policySemanticSimilarity(outputRules, originalInputRules);
            outputWrite.write("\n Rule-wise Semantic Similarity(Compare with original input rules): " + semSimilar + "\n");
            statsInfo[7] = semSimilar;
            
            double sameFrac = countSameRule/compareRules.size();
            outputWrite.write("\n Fraction of Rules in Both Simplified Input and Mined Policy: " + sameFrac + "\n");
            statsInfo[4] = sameFrac;
            
            double sameOriginalFrac = countOriginalSameRule/originalInputRules.size();
            outputWrite.write("\n Fraction of Rules in Both Original Input and Mined Policy: " + sameOriginalFrac + "\n");
            statsInfo[8] = sameOriginalFrac;
            
            outputWrite.write("============== OBJECT MODEL STATS ==============\n");
            int tupleSize = config.getUPList().size();
            outputWrite.write("Total tuple size: " + tupleSize + "\n");
            statsInfo[9] = tupleSize;
            
            int objectSize = config.getObjectModel().size();
            outputWrite.write("Total object size: " + objectSize + "\n");
            statsInfo[10] = objectSize;
            
            int fieldNum = config.getTotalFieldNum();
            outputWrite.write("Total field number: " + fieldNum + "\n");
            statsInfo[11] = fieldNum;
            
            outputWrite.write("============== RUNNING TIME ==============\n");
            outputWrite.write("Mining running time: " + runningTime + "\n");
            outputWrite.write("\n==========================================================================================\n==========================================================================================");
            outputWrite.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return new Pair(totalCPUTime, statsInfo);
    }

    public static void main(String[] args){
		if (args.length < 1){
			System.out.println("Missing config file argument");
			System.exit(0);
		}
        String configFile = args[0];
        System.out.println("Running " + configFile);
        Config config = new Config();
        readConfiguration(configFile, config);
        int[] sizes = config.getPolicySize();
        for (int i = 0; i < sizes.length; i++){
            int size = sizes[i];
            int policySize = config.getNumPoliciesPerSize();
            System.out.println("RUNNING POLICIES: " + config.getPolicyName() + "_" + size);
            double[][] statsInfo = new double[13][policySize];
            long totalTime = 0;
            new File(config.getOutputPath() + config.getPolicyName() + "_" + size + "/").mkdir();
			int[] runPolicies = new int[policySize];
			if (policySize == 1){
				runPolicies[0] = config.getRunPolicy();
			}
			else{
				for (int j = 0; j < policySize; j++){
					runPolicies[j] = j;
				}
			}
            for (int j : runPolicies){
                Pair<Long, double[]> resultStats = runOneDecisionTreeExperiment(config, configFile, size, j, config.getOutputPath() + config.getPolicyName() + "_" + size + "/" + config.getPolicyName() + "_" + size + "_" + j + ".output");
                for (int k = 0; k < statsInfo.length - 1; k++){
					if (policySize == 1){
						statsInfo[k][0] += resultStats.getSecond()[k];
					}
					else{
						statsInfo[k][j] += resultStats.getSecond()[k];
					}
                }
				if (policySize == 1){
					statsInfo[12][0] = resultStats.getFirst();
				}
				else{
					statsInfo[12][j] = resultStats.getFirst();
				}
                totalTime += resultStats.getFirst();
                System.out.println("FINISH RUNNING EXPERIMENT WITH: " + config.getPolicyName() + "_" + size + "_" + j);
            }
			
			if (policySize == 1){
				try {
					FileWriter outFileWriter = new FileWriter(config.getOutputFile(), true);
					BufferedWriter outFileWrite = new BufferedWriter(outFileWriter);
					outFileWrite.write("Phase 2: Improving Policy Step: \n===============================\n");
					outFileWrite.write("Average Mined Policy WSC: " + statsInfo[0][0] + " Standard Deviation: 0.0\n");
					outFileWrite.write("Average Policy Syntactic Similarity(Compare with simplified input rules): " + statsInfo[2][0] +" Standard Deviation: 0.0\n");
					
					double timeSec = (long) (statsInfo[2][0] * 0.000001) * 0.001;
					String timeSecStr = String.format("%.1f sec", timeSec);
					
					outFileWrite.write("Average time in seconds: " + timeSecStr + "\n");
					outFileWrite.close();
				}
				catch (IOException e){
					e.printStackTrace();
				}
			}
			else{
			
			
				// calculate the average and sd of the stats
				double minedWSCAvg = 0;
				double minedWSCSd = 0;
				double simplifiedInputWSCAvg = 0;
				double simplifiedInputWSCSd = 0;
				double simplifiedSynSimilarAvg = 0;
				double simplifiedSynSimilarSd = 0;
				double simplifiedSemSimilarAvg = 0;
				double simplifiedSemSimilarSd = 0;
				double simplifiedSameFracAvg = 0;
				double simplifiedSameFracSd = 0;
				
				double originalInputWSCAvg = 0;
				double originalInputWSCSd = 0;
				double originalSynSimilarAvg = 0;
				double originalSynSimilarSd = 0;
				double originalSemSimilarAvg = 0;
				double originalSemSimilarSd = 0;
				double originalSameFracAvg = 0;
				double originalSameFracSd = 0;
				
				double tupleSizeAvg = 0;
				double tupleSizeSd = 0;
				double objectSizeAvg = 0;
				double objectSizeSd = 0;
				double fieldNumAvg = 0;
				double fieldNumSd = 0;
				
				double timeAvg = 0;
				double timeSd = 0;
				
				for (int j = 0; j < policySize; j++){
					minedWSCAvg += statsInfo[0][j];
					simplifiedInputWSCAvg += statsInfo[1][j];
					simplifiedSynSimilarAvg += statsInfo[2][j];
					simplifiedSemSimilarAvg += statsInfo[3][j];
					simplifiedSameFracAvg += statsInfo[4][j];
					originalInputWSCAvg += statsInfo[5][j];
					originalSynSimilarAvg += statsInfo[6][j];
					originalSemSimilarAvg += statsInfo[7][j];
					originalSameFracAvg += statsInfo[8][j];
					tupleSizeAvg += statsInfo[9][j];
					objectSizeAvg += statsInfo[10][j];
					fieldNumAvg += statsInfo[11][j];
					timeAvg += statsInfo[12][j];
				}
				minedWSCAvg = minedWSCAvg/policySize;
				simplifiedInputWSCAvg = simplifiedInputWSCAvg/policySize;
				simplifiedSynSimilarAvg = simplifiedSynSimilarAvg/policySize;
				simplifiedSemSimilarAvg = simplifiedSemSimilarAvg/policySize;
				simplifiedSameFracAvg = simplifiedSameFracAvg/policySize;
				originalInputWSCAvg = originalInputWSCAvg/policySize;
				originalSynSimilarAvg = originalSynSimilarAvg/policySize;
				originalSemSimilarAvg = originalSemSimilarAvg/policySize;
				originalSameFracAvg = originalSameFracAvg/policySize;
				tupleSizeAvg = tupleSizeAvg/policySize;
				objectSizeAvg = objectSizeAvg/policySize;
				fieldNumAvg = fieldNumAvg/policySize;
				timeAvg = timeAvg/policySize;
				
				for (int j = 0; j < policySize; j++){
					minedWSCSd = minedWSCSd + ((statsInfo[0][j] - minedWSCAvg) * (statsInfo[0][j] - minedWSCAvg));
					simplifiedInputWSCSd = simplifiedInputWSCSd + ((statsInfo[1][j] - simplifiedInputWSCAvg) * (statsInfo[1][j] - simplifiedInputWSCAvg));
					simplifiedSynSimilarSd = simplifiedSynSimilarSd + ((statsInfo[2][j] - simplifiedSynSimilarAvg) * (statsInfo[2][j] - simplifiedSynSimilarAvg));
					simplifiedSemSimilarSd = simplifiedSemSimilarSd + ((statsInfo[3][j] - simplifiedSemSimilarAvg) * (statsInfo[3][j] - simplifiedSemSimilarAvg));
					simplifiedSameFracSd = simplifiedSameFracSd + ((statsInfo[4][j] - simplifiedSameFracAvg) * (statsInfo[4][j] - simplifiedSameFracAvg));
					
					originalInputWSCSd = originalInputWSCSd + ((statsInfo[5][j] - originalInputWSCAvg) * (statsInfo[5][j] - originalInputWSCAvg));
					originalSynSimilarSd = originalSynSimilarSd + ((statsInfo[6][j] - originalSynSimilarAvg) * (statsInfo[6][j] - originalSynSimilarAvg));
					originalSemSimilarSd = originalSemSimilarSd + ((statsInfo[7][j] - originalSemSimilarAvg) * (statsInfo[7][j] - originalSemSimilarAvg));
					originalSameFracSd = originalSameFracSd + ((statsInfo[8][j] - originalSameFracAvg) * (statsInfo[8][j] - originalSameFracAvg));
					
					tupleSizeSd = tupleSizeSd + ((statsInfo[9][j] - tupleSizeAvg) * (statsInfo[9][j] - tupleSizeAvg));
					objectSizeSd = objectSizeSd + ((statsInfo[10][j] - objectSizeAvg) * (statsInfo[10][j] - objectSizeAvg));
					fieldNumSd = fieldNumSd + ((statsInfo[11][j] - fieldNumAvg) * (statsInfo[11][j] - fieldNumAvg));
					
					timeSd = timeSd + ((statsInfo[12][j] - timeAvg) * (statsInfo[12][j] - timeAvg));
				}
				minedWSCSd = minedWSCSd/policySize;
				simplifiedInputWSCSd = simplifiedInputWSCSd/policySize;
				simplifiedSynSimilarSd = simplifiedSynSimilarSd/policySize;
				simplifiedSemSimilarSd = simplifiedSemSimilarSd/policySize;
				simplifiedSameFracSd = simplifiedSameFracSd/policySize;
				
				originalInputWSCSd = originalInputWSCSd/policySize;
				originalSynSimilarSd = originalSynSimilarSd/policySize;
				originalSemSimilarSd = originalSemSimilarSd/policySize;
				originalSameFracSd = originalSameFracSd/policySize;
				
				tupleSizeSd = tupleSizeSd/policySize;
				objectSizeSd = objectSizeSd/policySize;
				fieldNumSd = fieldNumSd/policySize;
				timeSd = timeSd/policySize;
				
				minedWSCSd = Math.sqrt(minedWSCSd);
				simplifiedInputWSCSd = Math.sqrt(simplifiedInputWSCSd);
				simplifiedSynSimilarSd = Math.sqrt(simplifiedSynSimilarSd);
				simplifiedSemSimilarSd = Math.sqrt(simplifiedSemSimilarSd);
				simplifiedSameFracSd = Math.sqrt(simplifiedSameFracSd);
				
				originalInputWSCSd = Math.sqrt(originalInputWSCSd);
				originalSynSimilarSd = Math.sqrt(originalSynSimilarSd);
				originalSemSimilarSd = Math.sqrt(originalSemSimilarSd);
				originalSameFracSd = Math.sqrt(originalSameFracSd);
				
				tupleSizeSd = Math.sqrt(tupleSizeSd);
				objectSizeSd = Math.sqrt(objectSizeSd);
				fieldNumSd = Math.sqrt(fieldNumSd);
				timeSd = Math.sqrt(timeSd);
				
				long avgCPUTime = totalTime/policySize;
				
				long totalCPUTimeMillis = (long) (totalTime * 0.000001);
				String time = String.format("%d min, %d sec",
						TimeUnit.MILLISECONDS.toMinutes(totalCPUTimeMillis),
						TimeUnit.MILLISECONDS.toSeconds(totalCPUTimeMillis) -
								TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalCPUTimeMillis))
				);
				
				long avgCPUTimeMillis = (long) (avgCPUTime * 0.000001);
				String timeAvgStr = String.format("%d min, %d sec",
						TimeUnit.MILLISECONDS.toMinutes(avgCPUTimeMillis),
						TimeUnit.MILLISECONDS.toSeconds(avgCPUTimeMillis) -
								TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(avgCPUTimeMillis))
				);
				
				double timeAvgSec = (long) (timeAvg * 0.000001) * 0.001;
				String timeAvgSecStr = String.format("%.1f sec", timeAvgSec);
				
				double timeSdSec = (long) (timeSd * 0.000001) * 0.001;
				String timeSdStr = String.format("%.3f sec", timeSdSec);
				
				// output to file
				try{
					FileWriter fileWriter = new FileWriter(config.getOutputPath() + config.getPolicyName() + "_" + size + "/" + config.getPolicyName() + "_" + size + ".output");
					BufferedWriter outputWrite = new BufferedWriter(fileWriter);
					outputWrite.write("Policy: " + config.getPolicyName() + "_" + size + "\n");
					outputWrite.write("Average Mined Policy WSC: " + minedWSCAvg + " Standard Deviation: "+ minedWSCSd + "\n");
					outputWrite.write("Average Simplified Input Policy WSC: " + simplifiedInputWSCAvg + " Standard Deviation: "+ simplifiedInputWSCSd + "\n");
					outputWrite.write("Average Policy Syntactic Similarity(Compare with simplified input rules): " + simplifiedSynSimilarAvg +" Standard Deviation: " + simplifiedSynSimilarSd + "\n");
					outputWrite.write("Average Rule-wise Semantic Similarity(Compare with simplified input rules): " + simplifiedSemSimilarAvg +" Standard Deviation: " + simplifiedSemSimilarSd + "\n");
					outputWrite.write("Average Fraction of Simplified Rules in Both Input and Mined Policy: " + simplifiedSameFracAvg +" Standard Deviation: " + simplifiedSameFracSd + "\n");
					outputWrite.write("\n");
					outputWrite.write("Average Original Input Policy WSC: " + originalInputWSCAvg + " Standard Deviation: "+ originalInputWSCSd + "\n");
					outputWrite.write("Average Policy Syntactic Similarity(Compare with original input rules): " + originalSynSimilarAvg +" Standard Deviation: " + originalSynSimilarSd + "\n");
					outputWrite.write("Average Rule-wise Semantic Similarity(Compare with original input rules): " + originalSemSimilarAvg +" Standard Deviation: " + originalSemSimilarSd + "\n");
					outputWrite.write("Average Fraction of Original Rules in Both Input and Mined Policy: " + originalSameFracAvg +" Standard Deviation: " + originalSameFracSd + "\n");
					outputWrite.write("\n");
					outputWrite.write("Average Tuple Size: " + tupleSizeAvg + " Standard Deviation: "+ tupleSizeSd + "\n");
					outputWrite.write("Average Object Size: " + objectSizeAvg +" Standard Deviation: " + objectSizeSd + "\n");
					outputWrite.write("Average Total Field Number: " + fieldNumAvg +" Standard Deviation: " + fieldNumSd + "\n");
					outputWrite.write("\n");
					outputWrite.write("Total CPU Time: " + time + "\n");
					outputWrite.write("Average Time: " + timeAvgStr + "\n");
					outputWrite.write("Average time in seconds: " + timeAvgSecStr + "\n");
					outputWrite.write("Standard deviation of time in seconds: " + timeSdStr + "\n");
					outputWrite.close();
					
					
					FileWriter outFileWriter = new FileWriter(config.getOutputFile(), true);
					BufferedWriter outFileWrite = new BufferedWriter(outFileWriter);
					outFileWrite.write("Phase 2: Improving Policy Step: \n===============================\n");
					outFileWrite.write("Average Mined Policy WSC: " + minedWSCAvg + " Standard Deviation: "+ minedWSCSd + "\n");
					outFileWrite.write("Average Policy Syntactic Similarity(Compare with simplified input rules): " + simplifiedSynSimilarAvg +" Standard Deviation: " + simplifiedSynSimilarSd + "\n");
					outFileWrite.write("Average Time: " + timeAvgStr + "\n");
					outFileWrite.write("Average time in seconds: " + timeAvgSecStr + "\n");
					outFileWrite.write("Standard deviation of time in seconds: " + timeSdStr + "\n");
					outFileWrite.close();
				}
				catch (IOException e){
					e.printStackTrace();
				}
			}
        }
    }
}

