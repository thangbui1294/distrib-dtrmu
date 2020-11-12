# This is an modified version of C4.5 algorithm's implementation from https://github.com/barisesmer/C4.5
import math
import copy


class C45:
    """Creates a decision tree with C4.5 algorithm"""
    def __init__(self, feature_names, features, label):
        self.data = []
        self.classes = []
        self.numAttributes = -1
        self.attrValues = {}
        self.attributes = []
        self.tree = Node(True, "Empty", None)
        self.preprocess_data(feature_names, features, label)

    def preprocess_data(self, feature_names, features, label):
        self.attributes = feature_names
        self.numAttributes = len(self.attributes)
        possible_values = [0,1,2]
        for attr in self.attributes:
            self.attrValues[attr] = possible_values

        self.classes.append(0)
        self.classes.append(1)

        for index, row in enumerate(features):
            rowWithLabel = copy.deepcopy(row)
            rowWithLabel.append(label[index])
            self.data.append(rowWithLabel)

    def get_tree_text(self):
        return self.print_node(self.tree)

    def print_node(self, node, indent="|--- "):
        s = ""
        if node.isLeaf:
            s += indent + 'class: ' + str(node.label) + "\n"
        else:
            if node.threshold is None:
                # discrete
                for index, child in enumerate(node.children):
                    s += indent + node.label + " = " + str(self.attrValues[node.label][index]) + "\n"
                    s += self.print_node(child, "|   " + indent)
            else:
                # numerical
                leftChild = node.children[0]
                rightChild = node.children[1]
                if leftChild.isLeaf:
                    s += indent + node.label + " <= " + str(node.threshold) + " : " + leftChild.label + "\n"
                else:
                    s += indent + node.label + " <= " + str(node.threshold) + " : " + "\n"
                    s += self.print_node(leftChild, indent + "	")

                if rightChild.isLeaf:
                    s += indent + node.label + " > " + str(node.threshold) + " : " + rightChild.label
                else:
                    s += indent + node.label + " > " + str(node.threshold) + " : " + "\n"
                    s += self.print_node(rightChild, indent + "	")
        return s

    def generate_tree(self):
        self.tree = self.recursive_generate_tree(self.data, self.attributes)

    def recursive_generate_tree(self, curData, curAttributes):
        if len(curData) == 0:
            # Fail
            return Node(True, "Empty", None)
        allSame = self.is_all_same_class(curData)
        if allSame is not False:
            # return a node with that class
            return Node(True, allSame, None)
        if len(curAttributes) == 0:
            # return a node with the majority class
            majClass = self.get_maj_class(curData)
            return Node(True, majClass, None)

        (best, best_threshold, splitted) = self.split_attr(curData, curAttributes)
        remainingAttributes = curAttributes[:]
        remainingAttributes.remove(best)
        node = Node(False, best, best_threshold)
        node.children = [self.recursive_generate_tree(subset, remainingAttributes) for subset in splitted]
        return node

    def get_maj_class(self, curData):
        freq = [0] * len(self.classes)
        for row in curData:
            index = self.classes.index(row[-1])
            freq[index] += 1
        maxInd = freq.index(max(freq))
        return self.classes[maxInd]

    def is_all_same_class(self, data):
        for row in data:
            if row[-1] != data[0][-1]:
                return False
        return data[0][-1]

    def is_discrete_attr(self, attribute):
        if attribute not in self.attributes:
            raise ValueError("Attribute not listed")
        elif len(self.attrValues[attribute]) == 1 and self.attrValues[attribute][0] == "continuous":
            return False
        else:
            return True

    def split_attr(self, curData, curAttributes):
        splitted = []
        maxEnt = -1 * float("inf")
        best_attribute = -1
        #tie_best_attributes = []
        # None for discrete attributes, threshold value for continuous attributes
        best_threshold = None
        for attribute in curAttributes:
            indexOfAttribute = self.attributes.index(attribute)
            if self.is_discrete_attr(attribute):
                # split curData into n-subsets, where n is the number of
                # different values of attribute i. Choose the attribute with
                # the max gain
                valuesForAttribute = self.attrValues[attribute]
                subsets = [[] for a in valuesForAttribute]
                for row in curData:
                    for index in range(len(valuesForAttribute)):
                        if row[indexOfAttribute] == valuesForAttribute[index]:
                            subsets[index].append(row)
                            break
                e = self.gain(curData, subsets)
                if e > maxEnt:
                    maxEnt = e
                    splitted = subsets
                    best_attribute = attribute
                    best_threshold = None
                # elif e == maxEnt:
                #     tie_best_attributes.append((attribute, e))
            else:
                # sort the data according to the column.Then try all
                # possible adjacent pairs. Choose the one that
                # yields maximum gain
                curData.sort(key=lambda x: x[indexOfAttribute])
                for j in range(0, len(curData) - 1):
                    if curData[j][indexOfAttribute] != curData[j + 1][indexOfAttribute]:
                        threshold = (curData[j][indexOfAttribute] + curData[j + 1][indexOfAttribute]) / 2
                        less = []
                        greater = []
                        for row in curData:
                            if (row[indexOfAttribute] > threshold):
                                greater.append(row)
                            else:
                                less.append(row)
                        e = self.gain(curData, [less, greater])
                        if e >= maxEnt:
                            splitted = [less, greater]
                            maxEnt = e
                            best_attribute = attribute
                            best_threshold = threshold

        # debug only
        # print('Attributes with same entropy(' + str(maxEnt) + ') with best attribute ' + best_attribute +   ': ')
        # for attr in tie_best_attributes:
        #     if attr[1] == maxEnt:
        #         print(attr)
        # print('-----------------------------------------')
        return (best_attribute, best_threshold, splitted)

    def gain(self, unionSet, subsets):
        # input : data and disjoint subsets of it
        # output : information gain
        S = len(unionSet)
        # calculate impurity before split
        impurityBeforeSplit = self.entropy(unionSet)
        # calculate impurity after split
        weights = [len(subset) / S for subset in subsets]
        impurityAfterSplit = 0
        for i in range(len(subsets)):
            impurityAfterSplit += weights[i] * self.entropy(subsets[i])
            # calculate total gain
        totalGain = impurityBeforeSplit - impurityAfterSplit
        return totalGain

    def entropy(self, dataSet):
        S = len(dataSet)
        if S == 0:
            return 0
        num_classes = [0 for i in self.classes]
        for row in dataSet:
            classIndex = list(self.classes).index(row[-1])
            num_classes[classIndex] += 1
        num_classes = [x / S for x in num_classes]
        ent = 0
        for num in num_classes:
            ent += num * self.log(num)
        return ent * -1

    def log(self, x):
        if x == 0:
            return 0
        else:
            return math.log(x, 2)


class Node:
    def __init__(self, isLeaf, label, threshold):
        self.label = label
        self.threshold = threshold
        self.isLeaf = isLeaf
        self.children = []
