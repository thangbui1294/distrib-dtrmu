					Thang Bui and Scott D. Stoller
						Stony Brook University

This is an implementation of the data generator for learning in the first phase of DTRM and DTRMU algorithms. The inputs consist of an attribute data file of a policy and a configuration file. The outputs consist of an access control list, objects list and attribute list of each pair of objects as .csv files. 

An example configuration file is in the "config" folder. Note that the paths specified in the config file consider this folder as the working directory. 

The config file contains several fields:

subConstraintExtraDistance=<integer>
This allows generating non-shortest paths for subject path when generating candidate constraint. the value specifies the extra distance from the shortest path of each reachable type from the subject type.

resConstraintExtraDistance=<integer>
This allows generating non-shortest paths for resource path when generating candidate constraint. the value specifies the extra distance from the shortest path of each reachable type from the resource type.

totalConstraintLengthPathLimit=<integer>
The maximum possible length of a constraint allowed, the length of constraint is the sum of the length of subject's path and the length of resource's path. where the “maximum total path length for constraints” parameter is introduced, that, as implemented, maximum total path length for constraints counts the “id” field which, in the formal model described in the paper, is implicit.    Therefore, the value for this parameter that should be used in the implementation is two more than the value that is needed based on the description in the paper.  The reason for always adding exactly two is that the algorithm currently mines only non-boolean constraints, so in every constraint, the subject path and resource path both end with “id”.

subConditionPathLimit=<integer>
The maximum length of the path in a subject's atomic condition.

resConditionPathLimit=<integer>
The maximum length of the path in a resource's atomic condition.

sizes={<integer>, <integer>, ...}
the input size parameter of generated synthetic attribute data.

numPoliciesPerSize=<integer>
the number of attribute datasets for each size specified in "sizes".

policyName=<string>
name of the policy, the name should match with attribute data's policy name.

inputPath=<string>
Path to the input ReBAC file, input file path should specify a file with extension ".abac_txt" (even though it is a ReBAC policy).

outputPath=<string>
Path to the output training data files.

isOneAtomicConstraint=<boolean>
This parameter can be ignored and should be set to "false". It is used for different features of the system.

==========================================================
RUNNING THE SYSTEM

Compile and run the main function in src/LearningDataGenerator.java with the libraries in the libs folder to obtain a set of training data files for the given ReBAC policy.

Lets consider this folder is the working directory.

To compile the program, runs the following commands:
cd src 
javac -cp "../libs/commons-lang3-3.4.jar." -d ../bin ./*.java

After specifying the values for the parameters in the configuration file, run the main function in LearningDataGenerator.java. The program requires 1 parameter: the path to the configuration file. 

Note that we might need to allocate a large enough memory space for the program, since the implementation includes some cache optimizations which may require large memory space. We should allocate at least 3 GB when running with small polices, and at least 8GB when running with large policies.

Command line example:
cd bin
java -Xms3g -Xmx3g -cp ".;../libs/commons-lang3-3.4.jar." learningdatagenerator/LearningDataGenerator ..\\configs\\pm_rebac.conf



