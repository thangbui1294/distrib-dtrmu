__author__ = 'Maddie'

# Mining Relationship-Based Access Control Policies
# Copyright (C) 2020 Thang Bui
# Copyright (C) 2020 Scott D. Stoller
# Copyright (c) 2020 Stony Brook University
# Copyright (c) 2020 Research Foundation of SUNY
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.


import numpy as np
import read_input_data
import os
import itertools as it


formatted_rules = []
filepath = ""
summed_coverage = 0
times = []


def clear_rules():
    formatted_rules.clear()
    global summed_coverage
    summed_coverage = 0


def sort_features(features, feature_names, labels):
    feature_weights = []
    for i in range(len(feature_names)):
        feature_size = read_input_data.count_wsc(feature_names[i])
        count = 0
        # count_pos = 0
        # for j in range(len(features)):
        #     if features[j][i] == 1:
        #         count += 1
        #         if labels[j] == 1:
        #             count_pos += 1
        # count = count_pos/(count + 0.0)
        feature_weights.append([feature_size, count])
    feature_weights = np.array(feature_weights, dtype='f')
    # return the indices of sorting primarily by ascending size and secondarily
    # on decreasing ration of: feature activated and action allowed / total feature activated
    sorted_feature_weights = np.lexsort(([-1, 1]*feature_weights[:, [1, 0]]).T)
    feature_names = list(np.array(feature_names)[sorted_feature_weights])
    # for i in range(len(feature_weights)):
    #   print(feature_weights[sorted_feature_weights][i], feature_names[i])
    for i in range(len(features)):
        features[i] = list(np.array(features[i])[sorted_feature_weights])
    return feature_names, features


def sort_feature_names(feature_names):
    feature_weights = []
    for i in range(len(feature_names)):
        feature_size = read_input_data.count_wsc(feature_names[i])
        feature_weights.append([feature_size, 0])
    feature_weights = np.array(feature_weights, dtype='f')
    # return the indices of sorting primarily by ascending size and secondarily
    # on decreasing ration of: feature activated and action allowed / total feature activated
    sorted_feature_weights = np.lexsort(([-1, 1]*feature_weights[:, [1, 0]]).T)
    feature_names = list(np.array(feature_names)[sorted_feature_weights])
    # for i in range(len(feature_weights)):
    #   print(feature_weights[sorted_feature_weights][i], feature_names[i])
    return feature_names


def extract_rules(tree_text):
    lines = tree_text.split('\n')
    line_array = []
    rule_base = []
    for i in range(len(lines)):
        line_array.append(lines[i].count('|'))
        if "class: 1" in lines[i]:
            rule_base.append(i)
    # finding all of the rules for a given tree
    all_correct_rules = []
    for i in range(len(rule_base)):
        rules = []
        current_line = rule_base[i]
        found_all = False
        while(found_all == False):
            found_next = False
            modifier = 1
            while(found_next == False):
                if (line_array[current_line-modifier] == line_array[current_line] - 1):
                    rules.append(lines[current_line-modifier])
                    if (line_array[current_line-modifier] == 1):
                        found_all = True
                    current_line = current_line-modifier
                    found_next = True
                else:
                    modifier += 1
        correct_rules = []
        for i in range(len(rules)):
            if "= 0" in rules[i]:
                correct_rules.append(rules[i][rules[i].index("---") + 4:len(rules[i]) - 4] + "(!=)")
            elif "= 2" in rules[i]:
                correct_rules.append(rules[i][rules[i].index("---") + 4:len(rules[i]) - 4] + "(UK)")
            else:
                correct_rules.append(rules[i][rules[i].index("---") + 4:len(rules[i]) - 4])
        all_correct_rules.append(correct_rules)
    # print(all_correct_rules)
    return all_correct_rules


# desired format: rule(Physician; ; MedicalRecord; ; consultations.patient.id contains consultation.patient.id; {view})
def format_rules(rules, subject, resource, action):
    # print(rules)
    for rule in rules:
        constraints = []
        sc = []
        rc = []
        # separate conditions and constraints
        for f in rule:
            if "sub<" in f:
                if ('!=' in f) or ('UK' in f):
                    formatted_f = f[4:-5] + f[len(f) - 4:]
                else:
                    formatted_f = f[4:len(f)-1]
                sc.append(formatted_f)
            elif "res<" in f:
                if ('!=' in f) or ('UK' in f):
                    formatted_f = f[4:-5] + f[len(f) - 4:]
                else:
                    formatted_f = f[4:len(f)-1]
                rc.append(formatted_f)
            else:
                if ('!=' in f) or ('UK' in f):
                    formatted_f = f[1:-5] + f[len(f) - 4:]
                else:
                    formatted_f = f[1:len(f)-1]
                constraints.append(formatted_f)
        # build the strings for conditions + constraints
        subject_conditions = ""
        for i in range(len(sc)):
            subject_conditions += sc[i]
            if (i < len(sc)-1):
                subject_conditions += " and "
        resource_conditions = ""
        for i in range(len(rc)):
            resource_conditions += rc[i]
            if (i < len(rc) - 1):
                resource_conditions += " and "
        full_constraints = ""
        for i in range(len(constraints)):
            full_constraints += constraints[i]
            if (i < len(constraints)-1):
                full_constraints += " and "
        # build the rule
        rule = "rule(" + subject + "; " + subject_conditions + "; " + resource + "; " + resource_conditions + "; " + full_constraints + "; {" + action + "})"
        formatted_rules.append(rule)
        print(rule)
        write_rule([rule])


def print_complete_rules():
    for rule in formatted_rules:
        print(rule)
    write_rule(formatted_rules)


def remove_negative_boolean_features(rules):
    for r in rules:
        for i, f in enumerate(r):
            if '(!=)' in f and 'true' in f:
                pos_f = f[0:-4]
                r[i] =  pos_f.replace('true', 'false')
            elif '(!=)' in f and 'false' in f:
                pos_f = f[0:-4]
                r[i] = pos_f.replace('false', 'true')
    return rules


def remove_negative_conds_same_path(rules, const_dict, optional_conds):
    for r in rules:
        neg_conditions = filter_features(r, '(!=)')
        if len(neg_conditions) == 0:
            continue
        # if there are still negative conditions, try to replace negative conditions that has same path with appropriate postive condition
        for i in range(len(neg_conditions)):
            neg_f = neg_conditions[i]
            feature = neg_f[0:-4]
            if neg_f in r and (('sub<' in neg_f or 'res<' in neg_f) and 'contains' not in neg_f and feature not in optional_conds):
                # get the path of the negative condition
                cond = neg_f[4:-5]
                cond_parts = cond.split(' in ')
                path = cond_parts[0].strip()
                const_str = cond_parts[1].strip()
                const = const_str[1:-1]
                if 'true' not in const and 'false' not in const:
                    removed_conds = [neg_f]
                    neg_const_vals = [const]
                    # get the set of remain neg conditions that share the same path
                    for j in range(i+1, len(neg_conditions)):
                        test_f = neg_conditions[j]
                        if ('sub<' in test_f or 'res<' in test_f) and 'contains' not in test_f:
                            cond1 = test_f[4:-5]
                            cond_parts1 = cond1.split(' in ')
                            path1 = cond_parts1[0].strip()
                            if path1 == path:
                                neg_const_vals.append(cond_parts1[1].strip()[1:-1])
                                removed_conds.append(test_f)
                    # replace the found negative conditions with corresponding positive one
                    for r_c in removed_conds:
                        r.remove(r_c)
                    pos_const_vals = []
                    for const in const_dict[path]:
                        if const not in neg_const_vals:
                            pos_const_vals.append(const)
                    new_pos_cond = neg_f[0:4] + path + ' in {'
                    for val in pos_const_vals:
                        new_pos_cond += val + ','
                    new_pos_cond = new_pos_cond[0:-1] + '}>'
                    r.append(new_pos_cond)
                    print('New condition is added: ' + new_pos_cond)
    return rules


def replace_conds_with_id_conds(rules, feature_names, features, sub_res_list):
    for r in rules:
        conditions = []
        neg_conditions = filter_features(r, '(!=)')
        uk_conditions = filter_features(r, '(UK)')
        for cond in neg_conditions:
            conditions.append(cond)
        for cond in uk_conditions:
            conditions.append(cond)
        if len(conditions) == 0:
            continue
        # if there are still negative conditions, try to replace negative conditions with "id" attribute conditions
        # check if there is negative condition is an atomic constraint, then we need to replace with both subject id condition and resource id condition.
        has_sub_cond = False
        has_res_cond = False
        for cond in conditions:
            if has_sub_cond and has_res_cond:
                break
            if not has_sub_cond and 'sub<' in cond:
                has_sub_cond = True
                continue
            if not has_res_cond and 'res<' in cond:
                has_res_cond = True
                continue

        if has_sub_cond or has_res_cond:
            covered_idx = get_covered_instances_idx(r, feature_names, features)
        else:
            continue
        if has_sub_cond:
            subject_ids = set()
            for idx in covered_idx:
                subject_ids.add(sub_res_list[idx][0])
            # remove all subject conditions
            remove_idx = []
            for cond_idx in range(len(r)):
                if 'sub<' in r[cond_idx]:
                    remove_idx.append(cond_idx)
            for i in range(len(remove_idx)):
                remove_index = remove_idx[i] - i
                del(r[remove_index])
            sub_id_cond = 'sub<id in {'
            for sub_id in subject_ids:
                sub_id_cond += sub_id + ','
            sub_id_cond = sub_id_cond[0:-1] + '}>'
            r.append(sub_id_cond)

        if has_res_cond:
            resource_ids = set()
            for idx in covered_idx:
                resource_ids.add(sub_res_list[idx][1])
            # remove all resource conditions
            remove_idx = []
            for cond_idx in range(len(r)):
                if 'res<' in r[cond_idx]:
                    remove_idx.append(cond_idx)
            for i in range(len(remove_idx)):
                remove_index = remove_idx[i] - i
                del(r[remove_index])
            res_id_cond = 'res<id in {'
            for res_id in resource_ids:
                res_id_cond += res_id + ','
            res_id_cond = res_id_cond[0:-1] + '}>'
            r.append(res_id_cond)
    return rules


def remove_features_from_rules(rules, replacible_feature_names, feature_names, features, labels, s):
    # removes negative features (s = '(!=)' or unknown features conditions (s = '(UK)') from rules
    for i in range(len(rules)):
        #print('-----------------------------------\nRemoving negative conditions in rule # ' + str(i) + '/' + str(len(rules)) + ': ' + str(rules[i]))
        found_f = filter_features(rules[i], s)
        for feature in found_f:
            #print('Trying to remove feature: ' + feature)
            temp = rules[i].copy()
            temp.remove(feature)
            # if the rule is valid without the feature, remove it
            temp_covered_instances = get_covered_instances_idx(temp, feature_names, features)
            if is_valid_rule(None, temp_covered_instances, None, labels, True):
                write_removal("REMOVAL", feature, "none", temp, rules[i])
                rules[i] = temp
            else:
                #print("Trying to replace the negative feature")
                # try to replace the negative condition with postive condition(s)
                temp = replace_with_one_postive_condition(rules, rules[i], feature, replacible_feature_names, feature_names, features, labels)
                if temp != 'NotReplacable':
                    rules[i] = temp
                else:
                    print('NOT ABLE TO ELMINATE CONDITION WITH REMOVING AND REPLACING WITH 1 POSTIVE CONDITION: \n' + str(feature) + ' in rule: ' + str(rules[i]))
    return rules


def compute_possible_path_constant(feature_names):
    const_dict = {}
    for f_name in feature_names:
        if ('sub<' in f_name or 'res<' in f_name) and 'contains' not in f_name:
            cond = f_name[4:-1]
            cond_parts = cond.split(' in ')
            path = cond_parts[0].strip()
            const_str = cond_parts[1].strip()
            const = const_str[1:-1]
            if 'true' not in const and 'false' not in const:
                if path not in const_dict:
                    const_dict[path] = set()
                const_dict[path].add(const)
    return const_dict


def filter_features(rule, s):
    # returns a list with all the negative conditions in a rule if s = '(!=)'
    # returns a list with all the negative conditions in a rule if s = '(UK)'
    conditions = []
    for feature in rule:
        if s in feature:
            conditions.append(feature)
    return conditions


def replace_with_one_postive_condition(rules, rule, feature, replacible_feature_names, feature_names, features, labels):
    # replaces a negative feature with a positive feature)
    temp = rule.copy()
    temp.remove(feature)
    # compute set of covered UP
    current_policy_coverage = np.array([], dtype=np.int32)
    for r in rules:
        if r != rule:
            current_policy_coverage = np.append(current_policy_coverage, get_covered_instances_idx(r, feature_names, features))
    current_policy_coverage_set = set(current_policy_coverage.flat)
    original_covered_instances = get_covered_instances_idx(rule, feature_names, features)
    #print('Start trying to replace with 1 positive condition')
    for i in range(len(replacible_feature_names)):
        #!!if feature_names[i] not in rule:
        #print('replace with feature ' + str(i) + '/' + str(len(feature_names)))
        ft = replacible_feature_names[i]
        temp.append(ft)
        #print('Enter check valid rule')
        temp_covered_instances = get_covered_instances_idx(temp, feature_names, features)
        if is_valid_rule(current_policy_coverage_set, temp_covered_instances, original_covered_instances, labels, False):
            write_removal("REPLACEMENT", feature, ft, temp, rule)
            return temp
        temp.remove(ft)
    return 'NotReplacable'


def replace_with_id_and_with_multiple_postive_conditions(rules, feature_names, features, labels, max_num_conds, num_pos_conds_threshold, s, sub_res_list):
    for i in range(len(rules)):
        conditions = filter_features(rules[i], s)
        if len(conditions) == 0:
            continue
        print('NEED TO REMOVE NEGATIVE CONDITIONS WITH ID CONDITION OR WITH MULTIPLE POSITIVE CONDITIONS')
        print('Rule:' + str(rules[i]))
        print('Unremovable features: ' + str(conditions))
        authorizations = {}
        res_ids = set()
        for idx in get_covered_instances_idx(rules[i], feature_names, features):
            sub_id = sub_res_list[idx][0]
            res_id = sub_res_list[idx][1]
            res_ids.add(res_id)
            if sub_id in authorizations:
                authorizations[sub_id].append(res_id)
            else:
                authorizations[sub_id] = [res_id]
        same_res = True
        for sub_id in authorizations:
            for res_id in res_ids:
                if res_id not in authorizations[sub_id]:
                    same_res = False
                    break
            if not same_res:
                break
        if same_res:
            rules[i].clear()
            sub_cond = 'sub<id in {'
            for sub_id in authorizations:
                sub_cond += sub_id + ','
            sub_cond = sub_cond[0:-1] + '}>'
            rules[i].append(sub_cond)

            res_cond = 'res<id in {'
            for res_id in res_ids:
                res_cond += res_id + ','
            res_cond = res_cond[0:-1] + '}>'
            rules[i].append(res_cond)
            print('NEW RULE: ' + str(rules[i]))
            continue
        else:
            print('Covered UP: ')
            for idx in get_covered_instances_idx(rules[i], feature_names, features):
                print(sub_res_list[idx])
            #exit()
            continue
        for feature in conditions:
            # replaces a negative feature with a positive feature)
            temp = rules[i].copy()
            temp.remove(feature)
            # compute set of covered UP
            current_policy_coverage = np.array([], dtype=np.int32)
            for r in rules:
                if r != rules[i]:
                    current_policy_coverage = np.append(current_policy_coverage, get_covered_instances_idx(r, feature_names, features))
            current_policy_coverage_set = set(current_policy_coverage.flat)
            original_covered_instances = get_covered_instances_idx(rules[i], feature_names, features)
            filtered_feature_names = filter_feature_names(rules[i], feature_names, features, num_pos_conds_threshold)
            for i in range(2, max_num_conds + 1):
                #print('Start trying to replace with ' + str(i) + ' positive conditions')
                for group in it.combinations(filtered_feature_names, i):
                    for j in range(i):
                        temp.append(group[j])
                    temp_covered_instances = get_covered_instances_idx(temp, feature_names, features)
                    if is_valid_rule(current_policy_coverage_set, temp_covered_instances, original_covered_instances, labels, False):
                        feature_list = ""
                        for ft in group:
                            feature_list += ft + " "
                        write_removal("REPLACEMENT", feature, feature_list, temp, rules[i])
                        rules[i] = temp
                        break
                    for j in range(i):
                        temp.remove(group[j])
                i += 1
    return rules


def filter_feature_names(rule, feature_names, features, ratio):
    covered_indexes = get_covered_instances_idx(rule, feature_names, features)
    considered_features = []
    percentages = []
    for i in range(len(feature_names)):
        number = 0
        for index in covered_indexes:
            if features[index][i] == 1:
                number += 1
        percentages.append(number / len(covered_indexes))
    percentages = np.array(percentages)
    sorted_percentages = np.argsort(percentages)
    num_to_take = round(len(feature_names) * ratio)
    for i in range(num_to_take):
        considered_features.append(feature_names[sorted_percentages[i]])
    print('Number of consider features: ' + str(len(considered_features)) + '/' + str(len(feature_names)))
    return considered_features


def is_valid_rule(current_policy_coverage_set, current_rule_instances, original_rule_instances, labels, covered):
    # checks if a new rule is valid
    # to be valid, a new rule must cover the same sub-res pairs as the original, although it may cover more
    # all covered pairs must have the label '1' (permissions must be assigned correctly)
    if covered:
        if is_valid_covered_instances(current_rule_instances, labels):
            return True
        return False
    else:
        if not is_valid_covered_instances(current_rule_instances, labels):
            return False
        policy_coverage_set = current_policy_coverage_set.copy()
        policy_coverage_set.update(current_rule_instances)
        if not all(x in policy_coverage_set for x in original_rule_instances):
            return False
        return True


def is_valid_covered_instances(covered_instances, labels):
    # checks if a rule only covers sub-res pairs with the label '1'
    labels_a = np.array(labels)
    covered_instances_labels = labels_a[covered_instances]
    #if np.sum(covered_instances_labels) == 0:
    #    print()
    #print('TEST1: ' + str(np.sum(covered_instances_labels)))
    return np.sum(covered_instances_labels) == len(covered_instances)


def compute_features_coverage(feature_names, features):
    # this method compute a map that map each feature name to the corresponding set of instances' indice that satisfy
    # the feature.
    f_coverage = {}
    for i in range(len(feature_names)):
        f_coverage[feature_names[i]] = ([],[]) # the first array stores covered instances, the second store uncovered instances
        for j in range(len(features)):
            if features[j][i] == 1:
                f_coverage[feature_names[i]][0].append(j)
            else:
                f_coverage[feature_names[i]][1].append(j)
    return f_coverage


def get_covered_instances_idx(rule, feature_names, features):
    if len(rule) == 0:
        covered_instances = []
        for i in range(len(features)):
            covered_instances.append(i)
        return np.array(covered_instances)
    else:
        # compute list of indice of features in the rule and its corresponding positive/negative aspect
        f_rule_idx = []
        f_rule_val = []
        new_conds = []
        for f_name in rule:
            test_name = f_name
            if '(!=)' in f_name :
                test_name = f_name[0:-4]
            elif '(UK)' in f_name:
                test_name = f_name[0:-4]
            if test_name not in feature_names:
                new_conds.append(test_name)
            else:
                if '(!=)' in f_name :
                    f_name = f_name[0:-4]
                    f_rule_val.append(0)
                elif '(UK)' in f_name:
                    f_name = f_name[0:-4]
                    f_rule_val.append(2)
                else:
                    f_rule_val.append(1)
                f_rule_idx.append(feature_names.index(f_name))
        features_a = np.array(features)
        f_rule_idx_a = np.array(f_rule_idx)
        f_rule_val_a = np.array(f_rule_val)
        vals = features_a[:,f_rule_idx_a] == f_rule_val_a
        vals = np.sum(vals, axis = 1)
        covered = np.where(vals == len(f_rule_val_a))[0]
        for cond in new_conds:
            remove_indexes = []
            single_conds = []
            cond_str = cond.split('{')
            consts_str = cond_str[1]
            consts_str = consts_str[0:-2]
            consts = consts_str.split(',')
            for const in consts:
                single_conds.append(cond_str[0] + '{' + const + '}>')
            for i,idx in enumerate(covered):
                satisfied_new_cond = False
                for sc in single_conds:
                    if features[idx][feature_names.index(sc)] == 1:
                        satisfied_new_cond = True
                        break
                if not satisfied_new_cond:
                    remove_indexes.append(i)
            covered = np.delete(covered, remove_indexes)
        return covered


def write_file(policy, output_path):
    filepath = os.path.join(output_path, policy + ".rules")
    if not os.path.exists(output_path):
        os.makedirs(output_path)
    file = open(filepath, "w")
    for rule in formatted_rules:
        file.write(rule + "\n")
    file.close()


def write_logs(tree_text, sub_type, res_type, op):
    file = open(filepath, "a+")
    file.write("--------------------------------------------")
    file.write(sub_type + " - " + res_type + " - " + op + "\n")
    file.write(tree_text + "\n")
    file.close()


def write_removal(type, original, new, new_rule, original_rule):
    file = open(filepath, "a+")
    file.write(type + "\nOriginal Rule: ")
    for feature in original_rule:
        file.write(feature + "; ")
    file.write("\nNew Rule: ")
    for feature in new_rule:
        file.write(feature + "; ")
    if type == "REPLACEMENT":
        file.write("\n" + original + " ==> " + new)
    else:
        file.write("\nRemoved Feature: " + original)
    file.write("\n\n")
    file.close()


def write_rule(rules):
    file = open(filepath, "a+")
    for rule in rules:
        file.write(rule + "\n")
    file.close()


def set_policy(policy_name, policy, log_path):
    global filepath
    filepath = os.path.join(log_path + policy_name, policy + "_logs.txt")
    if not os.path.exists(log_path + policy_name):
        os.makedirs(log_path + policy_name)
    file = open(filepath, "w")
    file.close()


def check_policy_coverage(rules, feature_names, features, labels):
    # everything appears to be covered...
    needed = []
    for i in range(len(labels)):
        if labels[i] == 1:
            needed.append(i)
    coverage = []
    for rule in rules:
        coverage.extend(get_covered_instances_idx(rule, feature_names, features))
    coverage = list(set(coverage))
    coverage.sort()
    return needed == coverage


def save_time(time):
    times.append(time)


def write_time(policy_name, log_path, output_file):
    global times
    times = np.array(times)
    set_policy(policy_name, policy_name + "_time", log_path)
    file = open(filepath, "a+")
    standard_dev = np.std(times, dtype=np.float64)
    time_average = np.mean(times )
    file.write("Average: " + str(time_average))
    file.write("\nStandard Deviation: " + str(standard_dev) + "\n")
    for time in times:
        file.write(str(time) + ": ")
    file.close()
    
    output_file = open(output_file, 'a+')
    output_file.write('Phase 1: Learning Tree Step\n===============================\n')
    output_file.write('Average running time for phase 1: '+ str(time_average) + '\n\n')


def reset_times():
    global times
    times = []
