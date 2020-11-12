#!/bin/bash
sudo yum update -y
sudo yum install python37 -y
sudo yum install java-1.8.0-openjdk -y
sudo yum install java-1.8.0-openjdk-devel -y
export PYTHONPATH="/home/ec2-user/distrib-dtrmu/distalgo-master":${PYTHONPATH} 
mkdir ~/.aws
cd /home/ec2-user/distrib-dtrmu
mkdir inst-bash
python3 -m da -n Master main.da 1 cw
