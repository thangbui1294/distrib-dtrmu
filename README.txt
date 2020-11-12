	  User Manual for Running DTRM and DTRMU Algorithms for Mining ReBAC Policies
                     Thang Bui and Scott D. Stoller
						Stony Brook University
					 
This material is based on work supported in part by NSF grants CCF-1954837, CNS-1421893, and CCF-1414078, and ONR grant N00014-20-1-2751.
			               

This file is top-level README of our system. In this file, we describe the structure of the system.

There are 3 main folders:

1. unknown-data: this folder contains the implementation of the unknown data generator and the policies with unknown attribute values that we used for the experiments with DTRMU algorithm. The detail descriptions is available in the README.txt file in the folder. 

2. algorithms: this folder contains the implementations of  DTRM and DTRMU algorithms and supplemental programs to generate training data. The detail descriptions of the folder is available in ./algorithms/README.txt.


This software was developed on Windows 10.  The example commands described in the source code's READMEs have been tested in Windows PowerShell and Windows Command Prompt (a.k.a. command.com). The code should work fine on linux, but some example commands might require minor changes, e.g., because some example commands end with a backslash, which bash interprets specially at the end of a line.