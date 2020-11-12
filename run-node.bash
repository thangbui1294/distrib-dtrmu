#!/bin/bash
export PYTHONPATH="/home/ec2-user/distrib-dtrmu/distalgo-master":${PYTHONPATH}
sudo chmod 777 /home/ec2-user/distrib-dtrmu/main.da
cd /home/ec2-user/distrib-dtrmu
python3 -m da -n $1 -D main.da