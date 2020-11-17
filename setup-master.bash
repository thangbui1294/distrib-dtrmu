#!/bin/bash
sudo yum install python37 -y
sudo yum install java-1.8.0-openjdk -y
sudo yum install java-1.8.0-openjdk-devel -y
export PYTHONPATH="/home/ec2-user/distrib-dtrmu/distalgo-master":${PYTHONPATH} 
cd /home/ec2-user/distrib-dtrmu