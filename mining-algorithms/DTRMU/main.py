__author__ = 'Thang'

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

import read_input_data
import utilities
from time import process_time
from learning import C45
import copy
import numpy as np
import os
import sys

if len(sys.argv) < 2:
    print('Invalid arguments! Missing ' + str(2 - len(sys.argv)) + ' argument(s)')
    exit(0)

# read parameter values for config file
config_f = open(sys.argv[1])
for line in config_f:
    if 'policy_name' in line:
        policy_name = line.strip().split('=')[1]
    elif 'num_policies' in line:
        num_policy = int(line.strip().split('=')[1])
    elif 'data_path' in line:
        data_path = line.strip().split('=')[1]
    elif 'mining_with_negation' in line:
        val = line.strip().split('=')[1].lower()
        if val == 'false':
            mining_with_negation = False
        else:
            mining_with_negation = True
    elif 'max_count_iter' in line:
        max_count_iter = int((line.strip().split('=')[1]))
    elif 'log_path' in line:
        log_path = (line.strip().split('=')[1])
    elif 'output_path' in line:
        output_path = (line.strip().split('=')[1])
    elif 'output_file' in line:
        output_file = (line.strip().split('=')[1])
config_f.close()

print("NEW POLICY: " + policy_name)
utilities.reset_times()
total_building_tree_time = []
total_eliminating_unknown_time = []
total_eliminating_neg_time = []
for i in range(0, num_policy):
    time_start = process_time()
    building_tree_time = 0
    eliminating_unknown_time = 0
    eliminating_neg_time = 0
    utilities.clear_rules()
    # read input data
    policy = policy_name + '_' + str(i)
    print(policy)
    # read data file from raw csv files
    input_file_path = data_path + policy_name + '/' + policy + '/' + policy
    acl_objects = read_input_data.read_acl_object_list_files(input_file_path, True)
    acl_by_ops = acl_objects[1]
    utilities.set_policy(policy_name, policy, log_path)
    for op in acl_by_ops:
        for sub_res_pair in acl_by_ops[op]:
            sub_res = sub_res_pair.split('-')
            sub_type = sub_res[0].strip()
            res_type = sub_res[1].strip()

            # get all training data for specific action and subject-resource types pair
            [feature_names, features, labels, sub_res_list, optional_conds] = read_input_data.get_sub_res_training_data(acl_objects, input_file_path, sub_type, res_type, op, sub_res_pair, True, False, False)

            read_input_data.remove_equivalent_features(feature_names, features)
            const_dict = utilities.compute_possible_path_constant(feature_names)

            # check if labels ever change (ex. if conditiions/ constraints are needed for the permission
            first = labels[0]
            constant = True
            for i in labels:
                if i != first:
                    constant = False
                    break
            print("\n-------------------")
            print(sub_type + " - " + res_type + " - " + op + "\n")
            if constant:
                if first == 1:
                    utilities.format_rules([[]], sub_type, res_type, op)
            else:
                # Start training here after getting list of features's names in features_name, feature vectors
                # in features and labels in labels

                # sorting features
                feature_names, features = utilities.sort_features(features, feature_names, labels)

                # Iteratively build the decision tree until all of the authorizations are covered.
                current_rule_set = []
                current_feature_names = copy.deepcopy(feature_names)
                replacable_feautre_names = copy.deepcopy(feature_names)
                if mining_with_negation:
                    for f in feature_names:
                        replacable_feautre_names.append(f + '(!=)')
                    replacable_feautre_names = utilities.sort_feature_names(replacable_feautre_names)
                current_features = copy.deepcopy(features)
                current_labels = copy.deepcopy(labels)
                all_covered = False
                count_iter = 0
                remove_all_unknown_features = True
                while not all_covered:
                    count_iter += 1
                    non_UK_iter_rule_set = []
                    UK_iter_rule_set = []
                    # build the deciion tree
                    print('START BULDING TREE')
                    st = process_time()
                    classifier = C45(current_feature_names, current_features, current_labels)
                    classifier.generate_tree()
                    et = process_time()
                    building_tree_time += et - st
                    tree_text = classifier.get_tree_text()

                    print('DONE BULDING TREE')
                    # print out the tree in text form
                    print(tree_text)
                    utilities.write_logs(tree_text, sub_type, res_type, op)

                    # translate + format the tree
                    all_rules = utilities.extract_rules(tree_text)

                    print('Rules extracted from tree:')
                    for rule in all_rules:
                        print(rule)
                    print('----------------------------')
                    # handle Unknown feature condition/constraint
                    # include negative features for replacement if the policy language supports negations.
                    print('START OF ELIMINATING UNKNOWN FEATURES')
                    st = process_time()
                    all_rules = utilities.remove_features_from_rules(all_rules, replacable_feautre_names, feature_names, features, labels, '(UK)')
                    et = process_time()
                    eliminating_unknown_time += et - st
                    remain_UK_f_idx = []
                    is_remove_rule = False
                    for rule in all_rules:
                        non_UK_f = True
                        for f in rule:
                            if '(UK)' in f:
                                non_UK_f = False
                                is_remove_rule = True
                                remain_UK_f_idx.append(current_feature_names.index(f[:-4]))
                        if non_UK_f:
                            current_rule_set.append(rule)
                            non_UK_iter_rule_set.append(rule)
                        else:
                            UK_iter_rule_set.append(rule)
                    if not is_remove_rule:
                        break
                    elif count_iter == max_count_iter:
                        for rule in UK_iter_rule_set:
                            current_rule_set.append(rule)
                        remove_all_unknown_features = False
                        break
                    else:
                        # the current rule set does not cover all authorizations when at least 1 rule is removed
                        # because it has an Unknown feature, this rule cannot be redundant because the Unknown
                        # feature would be removed in this case.
                        # update features, feature_names and labels for next round
                        current_policy_coverage = np.array([], dtype=np.int32)
                        for r in non_UK_iter_rule_set:
                            current_policy_coverage = np.append(current_policy_coverage, utilities.get_covered_instances_idx(r, current_feature_names, current_features))
                        current_policy_coverage_set = set(current_policy_coverage.flat)
                        for index in sorted(current_policy_coverage_set, reverse=True):
                            del current_features[index]
                            del current_labels[index]
                        for index in sorted(remain_UK_f_idx, reverse=True):
                            del current_feature_names[index]
                        for ins in current_features:
                            for index in sorted(remain_UK_f_idx, reverse=True):
                                del ins[index]

                        if len(current_features) == 0:
                            # stop the iterations and try other steps of eliminating unknown features
                            for rule in UK_iter_rule_set:
                                current_rule_set.append(rule)
                            st = process_time()
                            current_rule_set = utilities.replace_conds_with_id_conds(current_rule_set, feature_names, features, sub_res_list)
                            current_rule_set = utilities.replace_with_id_and_with_multiple_postive_conditions(current_rule_set, feature_names, features, labels, 2, 1.0, '(UK)')
                            et = process_time()
                            eliminating_unknown_time += et-st
                            break
                        print('NEXT ITERATION\n-------------------------------')
                        print('Iteration counter: ' + str(count_iter) + '\n-------------------------------')


                print('DONE ELIMINATING UNKNOWN FEATURES')



                if not mining_with_negation:
                    st = process_time()
                    current_rule_set = utilities.remove_features_from_rules(current_rule_set, feature_names, feature_names, features, labels, '(!=)')
                    current_rule_set = utilities.remove_negative_conds_same_path(current_rule_set, const_dict, optional_conds)
                    current_rule_set = utilities.replace_conds_with_id_conds(current_rule_set, feature_names, features, sub_res_list)
                    if not remove_all_unknown_features:
                        current_rule_set = utilities.replace_with_id_and_with_multiple_postive_conditions(current_rule_set, feature_names, features, labels, 2, 1.0, '(UK)', sub_res_list)
                    current_rule_set = utilities.replace_with_id_and_with_multiple_postive_conditions(current_rule_set, feature_names, features, labels, 2, 1.0, '(!=)', sub_res_list)
                    current_rule_set = utilities.remove_negative_boolean_features(current_rule_set)
                    et = process_time()
                    eliminating_neg_time += et - st
                    utilities.format_rules(current_rule_set, sub_type, res_type, op)
                else:
                    current_rule_set = utilities.remove_negative_boolean_features(current_rule_set)
                    utilities.format_rules(current_rule_set, sub_type, res_type, op)
                for rule in current_rule_set:
                    for f in rule:
                        if '(UK)' in f:
                            print('UNKNOWN FEATURE IS NOT REMOVED IN RULE: ' + str(rule))
                            exit(0)

    time_end = process_time()
    # print + export complete rule set
    print("\n-------------------\nComplete " + policy + " Rule Set:")
    utilities.print_complete_rules()
    utilities.write_file(policy, output_path)
    print('Running time of the policy: ' + str(time_end-time_start))
    print('Building tree running time of the policy: ' + str(building_tree_time))
    print('Eliminating Unknown running time of the policy: ' + str(eliminating_unknown_time))
    print('Eliminating Negative running time of the policy: ' + str(eliminating_neg_time))
    utilities.save_time(time_end-time_start)
    total_building_tree_time.append(building_tree_time)
    total_eliminating_neg_time.append(eliminating_neg_time)
    total_eliminating_unknown_time.append(eliminating_unknown_time)
utilities.write_time(policy_name, log_path, output_file)

output_time_file = os.path.join(log_path + policy_name, policy_name + '_time_breakdown.txt')
time_output_f = open(output_time_file, "a+")

total_building_tree_time = np.array(total_building_tree_time)
standard_dev = np.std(total_building_tree_time, dtype=np.float64)
time_average = np.mean(total_building_tree_time )
time_output_f.write('RUNNING TIME FOR BUILDING AND EXTRACTING TREE STEPS: \n=======================\n')
time_output_f.write("Average: " + str(time_average))
time_output_f.write("\nStandard Deviation: " + str(standard_dev) + "\n")

for time in total_building_tree_time:
    time_output_f.write(str(time) + ": ")
time_output_f.write('\n\n')

total_eliminating_unknown_time = np.array(total_eliminating_unknown_time)
standard_dev = np.std(total_eliminating_unknown_time, dtype=np.float64)
time_average = np.mean(total_eliminating_unknown_time )
time_output_f.write('RUNNING TIME FOR ELIMINATING UNKNOWN FEATURES STEP: \n=======================\n')
time_output_f.write("Average: " + str(time_average))
time_output_f.write("\nStandard Deviation: " + str(standard_dev) + "\n")

for time in total_eliminating_unknown_time:
    time_output_f.write(str(time) + ": ")
time_output_f.write('\n\n')

total_eliminating_neg_time = np.array(total_eliminating_neg_time)
standard_dev = np.std(total_eliminating_neg_time, dtype=np.float64)
time_average = np.mean(total_eliminating_neg_time )
time_output_f.write('RUNNING TIME FOR ELIMINATING NEGATIVE FEATURES STEP: \n=======================\n')
time_output_f.write("Average: " + str(time_average))
time_output_f.write("\nStandard Deviation: " + str(standard_dev) + "\n")

for time in total_eliminating_neg_time:
    time_output_f.write(str(time) + ": ")

time_output_f.close()
