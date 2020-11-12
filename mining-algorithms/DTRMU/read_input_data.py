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

import csv
import numpy as np


def read_acl_object_list_files(input_file_path, first_running):
    # input: input data path containing the object list, acl files and mined acl files (for iteration approach only)
    #        boolean first_running if running without iteration or running the first iteration.
    # this function return a list with
    # list[0]: dictionary of object lists with key: object ID, value: type of object.
    # list[1]: acess control list dictionary with 2 keys: action and subject-resource type, and value: list of subject-resource tuples
    # list[2]: similar to list[1], but contains only covered tuples from previous iterations (for iteration approach only)

    # read object list file
    objects_list = {}
    objects_list_file = input_file_path + '_objectList.csv'
    with open(objects_list_file, newline='') as csvfile:
        ol_reader = csv.reader(csvfile)
        for row in ol_reader:
            if ('Ojbect_ID' not in row[0]):
                # ignore the title row
                objects_list[row[0]] = row[1]

    # read covered ac list file
    covered_acl_rows = []
    covered_acls = {}
    if not first_running:
        covered_acl_file = input_file_path + '_minedACList.csv'
        with open(covered_acl_file, newline='') as csvfile:
            covered_acl_reader = csv.reader(csvfile)
            for row in covered_acl_reader:
                covered_acl_rows.append(row)
                if ('Subject' not in row[0]):
                    # ignore the title row
                    sub = row[0]
                    res = row[1]
                    op = row[2]
                    sub_type = objects_list[sub]
                    res_type = objects_list[res]
                    sub_res_pair = sub_type + '-' + res_type
                    if op not in covered_acls:
                        covered_acls[op] = {}
                    if sub_res_pair not in covered_acls[op]:
                        covered_acls[op][sub_res_pair] = []
                    covered_acls[op][sub_res_pair].append((sub,res))

    #read access control list file
    acls = {}
    acl_file = input_file_path + '_acList.csv'
    with open(acl_file, newline='') as csvfile:
        acl_reader = csv.reader(csvfile)
        for row in acl_reader:
            if ('Subject' not in row[0]) and (row not in covered_acl_rows):
                # ignore the title row
                sub = row[0]
                res = row[1]
                op = row[2]
                sub_type = objects_list[sub]
                res_type = objects_list[res]
                sub_res_pair = sub_type + '-' + res_type
                if op not in acls:
                    acls[op] = {}
                if sub_res_pair not in acls[op]:
                    acls[op][sub_res_pair] = []
                acls[op][sub_res_pair].append((sub,res))
    return [objects_list, acls, covered_acls]

def count_feature_vectors(input_file_path, sub_type, res_type):
    count_feature_vectors = 0
    num_features = 0
    file_num = 0
    got_num_feature = False
    try:
        while True:
            attribute_file = input_file_path + '_attributeList_' + sub_type+ '-' + res_type + '_' + str(file_num) + '.csv'
            num_lines = sum(1 for line in open(attribute_file))
            count_feature_vectors += num_lines
            if not got_num_feature:
                got_num_feature = True
                with open(attribute_file, newline='') as f:
                    reader = csv.reader(f)
                    row1 = next(reader)
                    num_features = len(row1) - 2 #the first two values are subject's Id and resource's id.
                    #print('Num features for ' + sub_type + '-' + res_type + 'is: ' + str(num_features))
            file_num += 1
    except FileNotFoundError:
        return count_feature_vectors, num_features


def read_attribute_files(input_file_path, sub_type, res_type, input_data, action, sub_res_pair):
    # return list of feature vectors from attribute list input file for specific <subject type, resource type> tuple
    # read through all attribute files of the specified subject type and resource type
    acl = input_data[1]
    covered_acl = input_data[2]
    data = []
    features_name = []
    labels = []
    sub_res_list = []
    first_row = True
    try:
        # read attribute file(s)
        attributes = []
        file_num = 0
        while True:
            attribute_file = input_file_path + '_attributeList_' + sub_type+ '-' + res_type + '_' + str(file_num) + '.csv'
            with open(attribute_file, newline='') as csvfile:
                attr_reader = csv.reader(csvfile)
                for row in attr_reader:
                    sub = row[0]
                    res = row[1]
                    sub_res_list.append((sub,res))
                    if (action in covered_acl) and (sub_res_pair in covered_acl[action]) and ((sub,res) in covered_acl[action][sub_res_pair]):
                        continue
                    # get label
                    if (sub,res) in acl[action][sub_res_pair]:
                        labels.append(1)
                    else:
                        labels.append(0)

                    # get data for each feature
                    row_data = []
                    for i in range(2, len(row)):
                        f_val = row[i]
                        attr_val = f_val.split(':')
                        if (first_row):
                            # get features name from the first row
                            features_name.append(attr_val[0])
                        #debug
                        if len(attr_val) < 2:
                            print('error')
                            print(f_val)
                            print(sub_res_pair)
                            print(sub)
                            print(res)
                            print(row)
                        attr_value = int(attr_val[1])
                        row_data.append(attr_value)
                    data.append(row_data)
                    first_row = False
                    #print('Reading attribute file _' + str(file_num))
            file_num += 1
    except FileNotFoundError:
        #print('gone thru all files')
        return data, features_name, labels, sub_res_list


def get_sub_res_training_data(input_data, input_file_path, sub_type, res_type, action, sub_res_pair, is_remove_id_features, is_combined_2_features, remove_false_boolean_features):
    # this function return list of feature names, feature vectors and corresponding labels of the vectors for a specific <subject type, resrouce type, action> tuple.
    # input: specified action, <subject type, resource type> tuple as strings
    #        input_data: containing read acl and covered acl (for iteration approach only)
    #        attrs: attribute data (read feature vectors read from read_attribute_files function)
    #        boolean is_remove_id features: remove id features if the value is true
    #        boolean is_combined_2_features: generate additional features by combining each 2 features together if the value is true
    #        boolean remove_false_boolean_features: remove all the boolean features that has "false" value since this can be inferred from the corresponding features with "true" value.
    #           note that this flag should be on if the class model does not have any MANY multiplicity boolean feature.
    data, features_name, labels, sub_res_list = read_attribute_files(input_file_path, sub_type, res_type, input_data, action, sub_res_pair)

    print(sub_res_pair + ": " + str(len(features_name)))
    # remove features that are not helpful
    remove_features_index = []
    for i in range(len(features_name)):
        true_val = False
        false_val = False
        for row in data:
            if (true_val and false_val):
                break
            else:
                if (row[i] == 0):
                    false_val = True
                else:
                    true_val = True
        if (not true_val) or (not false_val):
            remove_features_index.append(i)
    for i in range(len(remove_features_index)):
        remove_index = remove_features_index[i] - i
        del(features_name[remove_index])
    for row in data:
        for i in range(len(remove_features_index)):
            remove_index = remove_features_index[i] - i
            del(row[remove_index])

    # remove id features if the is_remove_id_features flag is on
    if (is_remove_id_features):
        remove_features_index = []
        for i in range(len(features_name)):
            f_name = features_name[i]
            if ('sub<id in {' in f_name) or ('res<id in {' in f_name):
                remove_features_index.append(i)
        for i in range(len(remove_features_index)):
            remove_index = remove_features_index[i] - i
            del(features_name[remove_index])
        for row in data:
            for i in range(len(remove_features_index)):
                remove_index = remove_features_index[i] - i
                del(row[remove_index])

    # remove false boolean features if the remove_false_boolean_features flag is on
    if (remove_false_boolean_features):
        remove_features_index = []
        for i in range(len(features_name)):
            f_name = features_name[i]
            if ('false' in f_name):
                remove_features_index.append(i)
        for i in range(len(remove_features_index)):
            remove_index = remove_features_index[i] - i
            del(features_name[remove_index])
        for row in data:
            for i in range(len(remove_features_index)):
                remove_index = remove_features_index[i] - i
                del(row[remove_index])

    # add extra combined 2-features if specified
    if (is_combined_2_features):
        init_size = len(features_name)
        for i in range(init_size):
            feature1_name = features_name[i]
            for j in range(i + 1, init_size):
                feature2_name = features_name[j]
                new_feature_name = feature1_name + ' AND ' + feature2_name
                features_name.append(new_feature_name)
                for row in data:
                    feature1_val = row[i]
                    feature2_val = row[j]
                    new_feature_val = feature1_val * feature2_val
                    row.append(new_feature_val)

    # generate set of conditions with optional multiplicity
    optional_conds = set()
    for i in range(len(features_name)):
        f_name = features_name[i]
        if 'OPTIONAL' in f_name:
            f_name = f_name[8:]
            optional_conds.add(f_name)
            features_name[i] = f_name
    return [features_name, data, labels, sub_res_list, optional_conds]

def get_column(a,i):
    column = []
    for e in a[:,i]:
        column.append([e])
    return column

def count_wsc(feature):
    # this function return the size of a feature
    # note that this only apply in current FS, assuming we only have 1 constant in each subject and resource condition
    if '(!=)' in feature:
        return feature.count('.') + 3 # extra 1 if the feature is negative
    return feature.count('.') + 2

def remove_equivalent_features(feature_names, features):
    # this function identify set of features that has exact same values in all feature vectors of a specific <subject type, resource type, action> tuple
    groups = []
    grouped_features = []
    val_mat = np.array(features)
    for i in range(np.shape(val_mat)[1]):
        if (i not in grouped_features):
            col = get_column(val_mat, i)
            diff = abs(val_mat - col)
            sum_col = diff.sum(axis=0)
            group = np.where(sum_col == 0)
            if (group[0].size> 1):
                groups.append(group[0])
                grouped_features.extend(group[0])

    remove_indices = []
    # get the best feature (in terms of size) in each group and remove other
    for group in groups:
        features_group = []
        for index in group:
            features_group.append(feature_names[index])
        size_list = []
        for i in range(len(group)):
            size_list.append((group[i], count_wsc(features_group[i])))
        size_list.sort(key=lambda x:x[1])
        shortest_f = size_list[0][1]
        for f in size_list:
            if f[1] > shortest_f:
                remove_indices.append(f[0])
    remove_indices.sort()
    for i in range(len(remove_indices)):
        remove_index = remove_indices[i] - i
        del(feature_names[remove_index])
    for row in features:
        for i in range(len(remove_indices)):
            remove_index = remove_indices[i] - i
            del(row[remove_index])